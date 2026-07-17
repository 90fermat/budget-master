package com.budgetmaster.reports.domain

import com.budgetmaster.core.ai.GenAiClient
import com.budgetmaster.core.ai.GenAiException
import com.budgetmaster.core.ai.GenAiSchema
import com.budgetmaster.reports.domain.model.CategorySlice
import com.budgetmaster.reports.domain.model.ReportRange
import com.budgetmaster.reports.domain.model.ReportSummary
import com.budgetmaster.reports.domain.usecase.AnswerFinanceQuestionUseCase
import com.budgetmaster.reports.domain.usecase.GenerateNarrativeUseCase
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Captures the prompt and returns a canned answer, or throws to simulate a provider failure. */
private class FakeGenAiClient(
    override val isAvailable: Boolean = true,
    private val respond: () -> String = { "ok" },
) : GenAiClient {
    var lastPrompt: String? = null
    override suspend fun generateJson(prompt: String, schema: GenAiSchema): String {
        lastPrompt = prompt
        return respond()
    }
}

class ReportsAiUseCasesTest {

    private val report = ReportSummary(
        range = ReportRange.MONTH,
        totalIncome = 5000.0,
        totalExpenses = 3200.0,
        categories = listOf(
            CategorySlice("cat_food", "Food & Dining", "#F59E0B", 1200.0, 0.375f),
            CategorySlice("cat_transport", "Transport", "#6366F1", 400.0, 0.125f),
        ),
        trend = emptyList(),
        previousIncome = 4800.0,
        previousExpenses = 2900.0,
        currencyCode = "USD",
    )

    private val empty = report.copy(totalIncome = 0.0, totalExpenses = 0.0, categories = emptyList())

    // ── Narrative ────────────────────────────────────────────────────────────

    @Test
    fun `narrative sends aggregates and the language, never a raw ledger`() = runBlocking {
        val client = FakeGenAiClient { "You spent most on Food." }
        val result = GenerateNarrativeUseCase(client).invoke(report, languageTag = "fr")

        assertEquals("You spent most on Food.", result.getOrThrow())
        val prompt = client.lastPrompt.orEmpty()
        assertTrue(prompt.contains("Food & Dining"), "category names are aggregates and expected")
        assertTrue(prompt.contains("1200"), "category totals expected")
        assertTrue(prompt.contains("'fr'"), "language tag expected")
        // There are no per-transaction fields in a ReportSummary, so none can leak.
        assertFalse(prompt.contains("transaction id", ignoreCase = true))
    }

    @Test
    fun `narrative is empty for an empty report and does not call the model`() = runBlocking {
        val client = FakeGenAiClient { error("must not run for an empty report") }
        val result = GenerateNarrativeUseCase(client).invoke(empty, "en")

        assertTrue(result.getOrThrow().isEmpty())
        assertEquals(null, client.lastPrompt)
    }

    @Test
    fun `narrative fails typed when no provider`() = runBlocking {
        val result = GenerateNarrativeUseCase(FakeGenAiClient(isAvailable = false)).invoke(report, "en")
        assertTrue(result.exceptionOrNull() is GenAiException.Unavailable)
    }

    @Test
    fun `narrative surfaces a rate limit as such`() = runBlocking {
        val client = object : GenAiClient {
            override val isAvailable = true
            override suspend fun generateJson(prompt: String, schema: GenAiSchema): String =
                throw GenAiException.RateLimited()
        }
        val result = GenerateNarrativeUseCase(client).invoke(report, "en")
        assertTrue(result.exceptionOrNull() is GenAiException.RateLimited)
    }

    // ── Q&A ──────────────────────────────────────────────────────────────────

    @Test
    fun `question includes the user question and the aggregates`() = runBlocking {
        val client = FakeGenAiClient { "You spent 1200 on food." }
        val result = AnswerFinanceQuestionUseCase(client)
            .invoke("How much on food?", report, languageTag = "en")

        assertEquals("You spent 1200 on food.", result.getOrThrow())
        val prompt = client.lastPrompt.orEmpty()
        assertTrue(prompt.contains("How much on food?"))
        assertTrue(prompt.contains("Food & Dining"))
    }

    @Test
    fun `blank question returns empty without calling the model`() = runBlocking {
        val client = FakeGenAiClient { error("must not run for a blank question") }
        val result = AnswerFinanceQuestionUseCase(client).invoke("   ", report, "en")

        assertTrue(result.getOrThrow().isEmpty())
        assertEquals(null, client.lastPrompt)
    }
}
