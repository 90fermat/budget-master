@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.dashboard.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.budgetmaster.core.db.BudgetMasterDatabase
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.model.Transaction
import com.budgetmaster.dashboard.data.service.GeminiInsightsService
import com.budgetmaster.dashboard.domain.model.Insight
import com.budgetmaster.dashboard.domain.model.InsightType
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertContains

class GeminiInsightsServiceTest {

    private lateinit var database: BudgetMasterDatabase
    private lateinit var databaseProvider: DatabaseProvider

    @BeforeTest
    fun setUp() {
        database = TestDatabaseHelper.createInMemoryDatabase()
        databaseProvider = DatabaseProvider(database)
    }

    /**
     * With no key the service must say nothing at all.
     *
     * This test previously asserted the opposite — that three "mock" insights came back — which
     * pinned in place a dashboard that told users invented things about their money ("coffee
     * spending up 15%") in any build without a key, which is every release build.
     */
    @Test
    fun testGetInsightsReturnsNothingAndIsNotConfiguredWithoutApiKey() = runBlocking {
        val service = GeminiInsightsService(
            databaseProvider = databaseProvider,
            httpClient = failingClient(),
            apiKeyProvider = { "" }
        )

        assertFalse(service.isConfigured)
        assertTrue(service.getInsights(emptyList(), forceRefresh = false).isEmpty())
    }

    /** A placeholder key is not a key: same silence, so a stray placeholder can't enable AI. */
    @Test
    fun testPlaceholderApiKeyIsTreatedAsUnconfigured() = runBlocking {
        val service = GeminiInsightsService(
            databaseProvider = databaseProvider,
            httpClient = failingClient(),
            apiKeyProvider = { "MOCK_IOS_API_KEY" }
        )

        assertFalse(service.isConfigured)
        assertTrue(service.getInsights(emptyList(), forceRefresh = false).isEmpty())
    }

    /** Fails the test rather than the request if an unconfigured service still calls out. */
    private fun failingClient() = HttpClient(MockEngine) {
        engine {
            addHandler { error("The service must not call Gemini without a configured key") }
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    /**
     * A free-tier 429 means "wait", not "give up": the service used to fall straight back to a
     * stale cache on the first one. It should retry and succeed.
     */
    @Test
    fun testRateLimitIsRetriedWithBackoffAndThenSucceeds() = runBlocking {
        var calls = 0
        val client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    calls++
                    if (calls == 1) {
                        respond("rate limited", HttpStatusCode.TooManyRequests)
                    } else {
                        respond(
                            """{"candidates":[{"content":{"parts":[{"text":"[{\"type\":\"TREND\",\"message\":\"ok\"}]"}]}}]}""",
                            HttpStatusCode.OK,
                            headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    }
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val service = GeminiInsightsService(
            databaseProvider = databaseProvider,
            httpClient = client,
            apiKeyProvider = { "test-key" },
            retryBackoffMs = listOf(0L, 0L), // no real sleeping in a test
        )

        val result = service.getInsights(emptyList(), forceRefresh = true)

        assertEquals(2, calls, "expected one retry after the 429")
        assertEquals(1, result.size)
        assertEquals(InsightType.TREND, result[0].type)
    }

    /** A non-429 failure will fail the same way however long we wait, so it must not retry. */
    @Test
    fun testNonRateLimitErrorIsNotRetried() = runBlocking {
        var calls = 0
        val client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    calls++
                    respond("bad request", HttpStatusCode.BadRequest)
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val service = GeminiInsightsService(
            databaseProvider = databaseProvider,
            httpClient = client,
            apiKeyProvider = { "test-key" },
            retryBackoffMs = listOf(0L, 0L),
        )

        service.getInsights(emptyList(), forceRefresh = true)

        assertEquals(1, calls, "a 400 must not be retried")
    }

    /**
     * The prompt must carry aggregates only. Transaction descriptions are free text — users put
     * merchant names, people's names and notes in them — and it sent every one of them, with
     * timestamps and ids, to a third party. Only category totals should leave the device.
     */
    @Test
    fun testPromptSendsAggregatesAndNeverTheRawLedger() = runBlocking {
        var body = ""
        val capturingClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    body = (request.body as io.ktor.http.content.TextContent).text
                    respond(
                        """{"candidates":[{"content":{"parts":[{"text":"[]"}]}}]}""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val service = GeminiInsightsService(
            databaseProvider = databaseProvider,
            httpClient = capturingClient,
            apiKeyProvider = { "test-key" }
        )

        service.getInsights(
            transactions = listOf(
                Transaction("t1", -12.5, "cat_food", "Lunch with Dr. Meredith Palmer", 1_700_000_000_000),
                Transaction("t2", -7.5, "cat_food", "Coffee", 1_700_000_100_000),
                Transaction("t3", 2000.0, "cat_salary", "ACME payroll", 1_700_000_200_000),
            ),
            forceRefresh = true,
            languageTag = "fr",
        )

        assertFalse(body.contains("Meredith"), "A transaction description reached the prompt:\n$body")
        assertFalse(body.contains("ACME"), "A transaction description reached the prompt:\n$body")
        assertFalse(body.contains("t1"), "A transaction id reached the prompt:\n$body")
        assertFalse(body.contains("1700000000000"), "A raw timestamp reached the prompt:\n$body")

        // The aggregates it does need: category totals, the income/expense sums, and the language.
        assertContains(body, "cat_food: 20")
        assertContains(body, "Total income: 2000")
        assertContains(body, "'fr'")
    }

    @Test
    fun testGetInsightsReturnsCachedIfFresh() = runBlocking {
        val queries = database.budgetMasterDatabaseQueries
        val now = Clock.System.now().toEpochMilliseconds()
        queries.insertInsight(
            id = "insight_cached",
            type = "SAVING",
            message = "Cached saving message",
            actionRoute = "budgets",
            timestamp = now
        )

        val mockHttpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    error("Should not hit network since cache is fresh")
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val service = GeminiInsightsService(
            databaseProvider = databaseProvider,
            httpClient = mockHttpClient,
            apiKeyProvider = { "valid_api_key" }
        )
        
        val result = service.getInsights(emptyList(), forceRefresh = false)
        assertEquals(1, result.size)
        assertEquals("insight_cached", result[0].id)
        assertEquals("Cached saving message", result[0].message)
    }

    @Test
    fun testGetInsightsCallsApiAndCaches() = runBlocking {
        val responseBody = """
            {
                "candidates": [
                    {
                        "content": {
                            "parts": [
                                {
                                    "text": "[{\"type\":\"SPENDING\",\"message\":\"High spending.\",\"actionRoute\":\"transactions\"},{\"type\":\"SAVING\",\"message\":\"Saved 50€.\",\"actionRoute\":null},{\"type\":\"TREND\",\"message\":\"Positive trend.\",\"actionRoute\":null}]"
                                }
                            ]
                        }
                    }
                ]
            }
        """.trimIndent()

        var capturedRequest: HttpRequestData? = null
        val mockHttpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    capturedRequest = request
                    respond(
                        content = responseBody,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val service = GeminiInsightsService(
            databaseProvider = databaseProvider,
            httpClient = mockHttpClient,
            apiKeyProvider = { "valid_api_key" }
        )

        val result = service.getInsights(emptyList(), forceRefresh = true)
        
        // Assert request details
        val req = capturedRequest
        assertTrue(req != null)
        assertTrue(req.url.toString().contains("v1beta/models/gemini-2.0-flash:generateContent"))
        assertEquals("valid_api_key", req.url.parameters["key"])

        assertEquals(3, result.size)
        assertEquals(InsightType.SPENDING, result[0].type)
        assertEquals("High spending.", result[0].message)
        assertEquals("transactions", result[0].actionRoute)

        assertEquals(InsightType.SAVING, result[1].type)
        assertEquals("Saved 50€.", result[1].message)
        assertEquals(null, result[1].actionRoute)

        // Verify it was written to cache
        val queries = database.budgetMasterDatabaseQueries
        val cachedEntities = queries.selectAllInsights().executeAsList()
        assertEquals(3, cachedEntities.size)
    }

    @Test
    fun testGetInsightsFallbackToCachedOn429RateLimit() = runBlocking {
        val queries = database.budgetMasterDatabaseQueries
        val now = Clock.System.now().toEpochMilliseconds()
        queries.insertInsight(
            id = "cached_insight_id",
            type = "TREND",
            message = "Trend cached",
            actionRoute = null,
            timestamp = now
        )

        val mockHttpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond(
                        content = "Too Many Requests",
                        status = HttpStatusCode.TooManyRequests,
                        headers = headersOf(HttpHeaders.ContentType, "text/plain")
                    )
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val service = GeminiInsightsService(
            databaseProvider = databaseProvider,
            httpClient = mockHttpClient,
            apiKeyProvider = { "valid_api_key" }
        )

        val result = service.getInsights(emptyList(), forceRefresh = true)
        assertEquals(1, result.size)
        assertEquals("cached_insight_id", result[0].id)
        assertEquals("Trend cached", result[0].message)
    }

    @Test
    fun testGetInsightsReturnsEmptyOnParseErrorNoCrash() = runBlocking {
        // Bad JSON returned from Gemini API
        val responseBody = """
            {
                "candidates": [
                    {
                        "content": {
                            "parts": [
                                {
                                    "text": "Invalid JSON or not matching structure"
                                }
                            ]
                        }
                    }
                ]
            }
        """.trimIndent()

        val mockHttpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond(
                        content = responseBody,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val service = GeminiInsightsService(
            databaseProvider = databaseProvider,
            httpClient = mockHttpClient,
            apiKeyProvider = { "valid_api_key" }
        )

        val result = service.getInsights(emptyList(), forceRefresh = true)
        assertTrue(result.isEmpty())
    }
}
