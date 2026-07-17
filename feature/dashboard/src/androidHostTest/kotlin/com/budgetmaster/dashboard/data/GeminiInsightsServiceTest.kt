@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.dashboard.data

import com.budgetmaster.core.ai.GenAiClient
import com.budgetmaster.core.ai.GenAiException
import com.budgetmaster.core.ai.GenAiSchema
import com.budgetmaster.core.db.BudgetMasterDatabase
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.model.Transaction
import com.budgetmaster.dashboard.data.service.GeminiInsightsService
import com.budgetmaster.dashboard.domain.model.InsightType
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A scripted [GenAiClient].
 *
 * These tests used to mock HTTP, because the service spoke the GenerateContent REST protocol
 * itself. It now goes through `GenAiClient` (Firebase AI Logic on Android), so the seam is this
 * interface and the tests say what they mean.
 *
 * @param available Whether a provider exists on this platform.
 * @param respond Called per attempt with the 1-based attempt number; throw to simulate failure.
 */
private class FakeGenAiClient(
    override val isAvailable: Boolean = true,
    val respond: (attempt: Int) -> String = { "[]" },
) : GenAiClient {
    var calls = 0
    var lastPrompt: String? = null
    var lastSchema: GenAiSchema? = null

    override suspend fun generateJson(prompt: String, schema: GenAiSchema): String {
        calls++
        lastPrompt = prompt
        lastSchema = schema
        return respond(calls)
    }
}

class GeminiInsightsServiceTest {

    private lateinit var database: BudgetMasterDatabase
    private lateinit var databaseProvider: DatabaseProvider

    @BeforeTest
    fun setUp() {
        database = TestDatabaseHelper.createInMemoryDatabase()
        databaseProvider = DatabaseProvider(database)
    }

    private fun service(
        client: GenAiClient,
        backoff: List<Long> = listOf(0L, 0L), // no real sleeping in a test
    ) = GeminiInsightsService(
        databaseProvider = databaseProvider,
        genAiClient = client,
        retryBackoffMs = backoff,
    )

    /**
     * With no provider the service must say nothing at all.
     *
     * This test previously asserted the opposite — that three "mock" insights came back — which
     * pinned in place a dashboard that told users invented things about their money ("coffee
     * spending up 15%") whenever no key was configured.
     */
    @Test
    fun testReturnsNothingAndIsNotConfiguredWithoutAProvider() = runBlocking {
        val client = FakeGenAiClient(isAvailable = false) { error("must not generate without a provider") }
        val service = service(client)

        assertFalse(service.isConfigured)
        assertTrue(service.getInsights(emptyList(), forceRefresh = false).isEmpty())
        assertEquals(0, client.calls)
    }

    /**
     * A rate limit means "wait", not "give up": the service used to fall straight back to a stale
     * cache on the first one. It should retry and succeed.
     */
    @Test
    fun testRateLimitIsRetriedWithBackoffAndThenSucceeds() = runBlocking {
        val client = FakeGenAiClient { attempt ->
            if (attempt == 1) throw GenAiException.RateLimited()
            """[{"type":"TREND","message":"ok"}]"""
        }

        val result = service(client).getInsights(emptyList(), forceRefresh = true)

        assertEquals(2, client.calls, "expected one retry after the rate limit")
        assertEquals(1, result.size)
        assertEquals(InsightType.TREND, result[0].type)
    }

    /** Any other failure will fail the same way however long we wait, so it must not retry. */
    @Test
    fun testNonRateLimitErrorIsNotRetried() = runBlocking {
        val client = FakeGenAiClient { throw GenAiException.Failed("bad request") }

        service(client).getInsights(emptyList(), forceRefresh = true)

        assertEquals(1, client.calls, "a non-rate-limit failure must not be retried")
    }

    /**
     * The prompt must carry aggregates only. Transaction descriptions are free text — users put
     * merchant names, people's names and notes in them — and it sent every one of them, with
     * timestamps and ids, to a third party. Only category totals should leave the device.
     */
    @Test
    fun testPromptSendsAggregatesAndNeverTheRawLedger() = runBlocking {
        val client = FakeGenAiClient { "[]" }

        service(client).getInsights(
            transactions = listOf(
                Transaction("t1", -12.5, "cat_food", "Lunch with Dr. Meredith Palmer", 1_700_000_000_000),
                Transaction("t2", -7.5, "cat_food", "Coffee", 1_700_000_100_000),
                Transaction("t3", 2000.0, "cat_salary", "ACME payroll", 1_700_000_200_000),
            ),
            forceRefresh = true,
            languageTag = "fr",
        )

        val prompt = client.lastPrompt.orEmpty()
        assertFalse(prompt.contains("Meredith"), "A transaction description reached the prompt:\n$prompt")
        assertFalse(prompt.contains("ACME"), "A transaction description reached the prompt:\n$prompt")
        assertFalse(prompt.contains("1700000000000"), "A raw timestamp reached the prompt:\n$prompt")

        // The aggregates it does need: category totals, the income/expense sums, and the language.
        assertContains(prompt, "cat_food: 20")
        assertContains(prompt, "Total income: 2000")
        assertContains(prompt, "'fr'")
    }

    /** The shape is enforced by the provider, not requested in prose and hoped for. */
    @Test
    fun testRequestsAStructuredSchema() = runBlocking {
        val client = FakeGenAiClient { "[]" }

        service(client).getInsights(emptyList(), forceRefresh = true)

        val items = (client.lastSchema as GenAiSchema.Arr).items as GenAiSchema.Obj
        assertEquals(
            listOf("SPENDING", "SAVING", "TREND"),
            (items.properties.getValue("type") as GenAiSchema.Enumeration).values,
        )
        // An enum, so the model cannot route the user to a screen that does not exist.
        assertEquals(
            listOf("transactions", "budgets", "goals"),
            (items.properties.getValue("actionRoute") as GenAiSchema.Enumeration).values,
        )
        assertEquals(listOf("actionRoute"), items.optional)
    }

    @Test
    fun testReturnsCachedIfFresh() = runBlocking {
        database.budgetMasterDatabaseQueries.insertInsight(
            id = "insight_cached",
            type = "SAVING",
            message = "Cached saving message",
            actionRoute = "budgets",
            timestamp = Clock.System.now().toEpochMilliseconds(),
        )
        val client = FakeGenAiClient { error("Should not generate while the cache is fresh") }

        val result = service(client).getInsights(emptyList(), forceRefresh = false)

        assertEquals(1, result.size)
        assertEquals("insight_cached", result[0].id)
        assertEquals(0, client.calls)
    }

    @Test
    fun testGeneratesAndCaches() = runBlocking {
        val client = FakeGenAiClient {
            """
            [
              {"type":"SPENDING","message":"High spending.","actionRoute":"transactions"},
              {"type":"SAVING","message":"Saved 50€.","actionRoute":null},
              {"type":"TREND","message":"Positive trend.","actionRoute":null}
            ]
            """.trimIndent()
        }

        val result = service(client).getInsights(emptyList(), forceRefresh = true)

        assertEquals(3, result.size)
        assertEquals(InsightType.SPENDING, result[0].type)
        assertEquals("High spending.", result[0].message)
        assertEquals("transactions", result[0].actionRoute)
        assertEquals(InsightType.SAVING, result[1].type)
        assertEquals(null, result[1].actionRoute)

        assertEquals(3, database.budgetMasterDatabaseQueries.selectAllInsights().executeAsList().size)
    }

    @Test
    fun testFallsBackToCacheWhenRateLimitedThroughout() = runBlocking {
        database.budgetMasterDatabaseQueries.insertInsight(
            id = "cached_insight_id",
            type = "TREND",
            message = "Trend cached",
            actionRoute = null,
            timestamp = Clock.System.now().toEpochMilliseconds(),
        )
        val client = FakeGenAiClient { throw GenAiException.RateLimited() }

        val result = service(client).getInsights(emptyList(), forceRefresh = true)

        assertEquals(3, client.calls, "should exhaust the retries before giving up")
        assertEquals(1, result.size)
        assertEquals("cached_insight_id", result[0].id)
    }

    /** A malformed response must never take the dashboard down with it. */
    @Test
    fun testReturnsEmptyOnParseErrorWithoutCrashing() = runBlocking {
        val client = FakeGenAiClient { "Invalid JSON or not matching structure" }

        assertTrue(service(client).getInsights(emptyList(), forceRefresh = true).isEmpty())
    }
}
