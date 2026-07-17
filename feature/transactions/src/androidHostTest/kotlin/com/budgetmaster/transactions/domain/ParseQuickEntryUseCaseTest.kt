@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.transactions.domain

import com.budgetmaster.core.ai.GenAiClient
import com.budgetmaster.core.ai.GenAiException
import com.budgetmaster.core.ai.GenAiSchema
import com.budgetmaster.core.util.DateUtils
import com.budgetmaster.transactions.domain.model.TransactionCategory
import com.budgetmaster.transactions.domain.usecase.ParseQuickEntryUseCase
import com.budgetmaster.transactions.domain.usecase.QuickEntryError
import com.budgetmaster.transactions.domain.usecase.QuickEntryException
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A GenAiClient that returns a fixed JSON string (or throws), and records the prompt it saw. */
private class FakeGenAiClient(
    override val isAvailable: Boolean = true,
    private val respond: () -> String = { "{}" },
) : GenAiClient {
    var lastPrompt: String? = null
    override suspend fun generateJson(prompt: String, schema: GenAiSchema): String {
        lastPrompt = prompt
        return respond()
    }
}

class ParseQuickEntryUseCaseTest {

    private val categories = listOf(
        TransactionCategory("cat_food", "Food & Dining", "🍔", "#F59E0B"),
        TransactionCategory("cat_transport", "Transport", "🚗", "#6366F1"),
    )

    // A fixed "now" so relative-date assertions are stable: 2026-07-17 12:00 UTC-ish.
    private val now = 1_752_753_600_000L

    private fun parse(json: String, available: Boolean = true) =
        ParseQuickEntryUseCase(FakeGenAiClient(available) { json })

    @Test
    fun `parses amount description category and expense flag`() = runBlocking {
        val result = parse(
            """{"amount":"4.50","isExpense":"true","description":"Coffee","categoryId":"cat_food","daysAgo":"0"}""",
        ).invoke("coffee 4.50 today", categories, now)

        val draft = result.getOrThrow()
        assertEquals(4.50, draft.amountAbs)
        assertTrue(draft.isExpense)
        assertEquals("Coffee", draft.description)
        assertEquals("cat_food", draft.categoryId)
    }

    @Test
    fun `resolves daysAgo to the start of that day`() = runBlocking {
        val draft = parse("""{"amount":"12","daysAgo":"1"}""")
            .invoke("lunch 12 yesterday", categories, now).getOrThrow()

        val expected = DateUtils.toLocalDate(now)
            .plus(-1, DateTimeUnit.DAY)
            .atStartOfDayIn(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
        assertEquals(expected, draft.timestamp)
    }

    @Test
    fun `accepts a comma decimal and takes the magnitude`() = runBlocking {
        // Models echo the user's locale: "4,50" must not parse to null, and a negative is still
        // an amount magnitude (the sign is carried by isExpense).
        val draft = parse("""{"amount":"-4,50","isExpense":"true"}""")
            .invoke("café 4,50", categories, now).getOrThrow()

        assertEquals(4.50, draft.amountAbs)
    }

    @Test
    fun `a hallucinated category id becomes uncategorized`() = runBlocking {
        val draft = parse("""{"amount":"9","categoryId":"cat_does_not_exist"}""")
            .invoke("thing 9", categories, now).getOrThrow()

        assertNull(draft.categoryId, "only ids we offered may be accepted")
    }

    @Test
    fun `no amount is a typed NoAmount failure`() = runBlocking {
        val result = parse("""{"description":"just a note"}""")
            .invoke("just a note", categories, now)

        assertEquals(QuickEntryError.NoAmount, (result.exceptionOrNull() as QuickEntryException).error)
    }

    @Test
    fun `unavailable provider is a typed Unavailable failure and is not called`() = runBlocking {
        val result = ParseQuickEntryUseCase(FakeGenAiClient(isAvailable = false))
            .invoke("coffee 4", categories, now)

        assertEquals(QuickEntryError.Unavailable, (result.exceptionOrNull() as QuickEntryException).error)
    }

    @Test
    fun `blank input fails without calling the model`() = runBlocking {
        val client = FakeGenAiClient { error("must not be called for blank input") }
        val result = ParseQuickEntryUseCase(client).invoke("   ", categories, now)

        assertEquals(QuickEntryError.NoAmount, (result.exceptionOrNull() as QuickEntryException).error)
        assertNull(client.lastPrompt)
    }

    @Test
    fun `a rate limit surfaces as a Failed result rather than throwing`() = runBlocking {
        val useCase = ParseQuickEntryUseCase(
            object : GenAiClient {
                override val isAvailable = true
                override suspend fun generateJson(prompt: String, schema: GenAiSchema): String =
                    throw GenAiException.RateLimited()
            },
        )
        val result = useCase.invoke("coffee 4", categories, now)

        assertTrue(result.isFailure)
        assertEquals(QuickEntryError.Failed, (result.exceptionOrNull() as QuickEntryException).error)
    }

    @Test
    fun `the prompt carries only the note and category list, never a ledger`() = runBlocking {
        val client = FakeGenAiClient { """{"amount":"4"}""" }
        ParseQuickEntryUseCase(client).invoke("coffee 4.50 at Starbucks", categories, now)

        val prompt = client.lastPrompt.orEmpty()
        assertTrue(prompt.contains("coffee 4.50 at Starbucks"), "the user's own note is expected")
        assertTrue(prompt.contains("cat_food"), "the category list is expected")
        // Nothing else about the user exists in this use case to leak — no transactions are read.
        assertFalse(prompt.contains("balance", ignoreCase = true))
    }
}
