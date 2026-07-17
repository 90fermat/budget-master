package com.budgetmaster.budgets.domain

import com.budgetmaster.budgets.domain.model.BudgetCategory
import com.budgetmaster.budgets.domain.model.BudgetDraft
import com.budgetmaster.budgets.domain.model.BudgetItem
import com.budgetmaster.budgets.domain.repository.BudgetRepository
import com.budgetmaster.budgets.domain.usecase.SuggestBudgetsUseCase
import com.budgetmaster.core.ai.GenAiClient
import com.budgetmaster.core.ai.GenAiException
import com.budgetmaster.core.ai.GenAiSchema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A repository that returns canned category averages; the other methods aren't exercised here. */
private class FakeBudgetRepository(
    private val averages: Map<String, Double>,
) : BudgetRepository {
    override fun observeBudgets(): Flow<List<BudgetItem>> = flowOf(emptyList())
    override fun observeCategories(): Flow<List<BudgetCategory>> = flowOf(emptyList())
    override suspend fun upsertBudget(draft: BudgetDraft) {}
    override suspend fun deleteBudget(id: String) {}
    override suspend fun averageMonthlySpendingByCategory(months: Int): Map<String, Double> = averages
}

private class FakeGenAiClient(
    override val isAvailable: Boolean = true,
    private val respond: () -> String = { "[]" },
) : GenAiClient {
    var lastPrompt: String? = null
    override suspend fun generateJson(prompt: String, schema: GenAiSchema): String {
        lastPrompt = prompt
        return respond()
    }
}

class SuggestBudgetsUseCaseTest {

    private val categories = listOf(
        BudgetCategory("cat_food", "Food & Dining", "🍔", "#F59E0B"),
        BudgetCategory("cat_transport", "Transport", "🚗", "#6366F1"),
    )

    @Test
    fun `proposes a limit within a sane band around the average`() = runBlocking {
        val repo = FakeBudgetRepository(mapOf("cat_food" to 400.0))
        val client = FakeGenAiClient {
            """[{"categoryId":"cat_food","suggestedLimit":"450","rationale":"A bit of headroom."}]"""
        }
        val result = SuggestBudgetsUseCase(repo, client).invoke(categories, emptySet(), "en").getOrThrow()

        assertEquals(1, result.size)
        assertEquals("Food & Dining", result[0].categoryName)
        assertEquals(450.0, result[0].suggestedLimit)
    }

    @Test
    fun `clamps a runaway model number to at most twice the average`() = runBlocking {
        val repo = FakeBudgetRepository(mapOf("cat_food" to 400.0))
        val client = FakeGenAiClient {
            """[{"categoryId":"cat_food","suggestedLimit":"999999"}]"""
        }
        val result = SuggestBudgetsUseCase(repo, client).invoke(categories, emptySet(), "en").getOrThrow()

        // A budget the size of a hallucination helps no one; ceiling is 2× the real average.
        assertEquals(800.0, result[0].suggestedLimit)
    }

    @Test
    fun `never proposes below the average, so the budget isn't set up to fail`() = runBlocking {
        val repo = FakeBudgetRepository(mapOf("cat_food" to 400.0))
        val client = FakeGenAiClient { """[{"categoryId":"cat_food","suggestedLimit":"50"}]""" }
        val result = SuggestBudgetsUseCase(repo, client).invoke(categories, emptySet(), "en").getOrThrow()

        assertEquals(400.0, result[0].suggestedLimit)
    }

    @Test
    fun `excludes categories that already have a budget`() = runBlocking {
        val repo = FakeBudgetRepository(mapOf("cat_food" to 400.0, "cat_transport" to 120.0))
        val client = FakeGenAiClient {
            // The model is only asked about categories without a budget, so it won't see cat_food.
            """[{"categoryId":"cat_transport","suggestedLimit":"140"}]"""
        }
        val result = SuggestBudgetsUseCase(repo, client)
            .invoke(categories, existingBudgetCategoryIds = setOf("cat_food"), languageTag = "en")
            .getOrThrow()

        assertEquals(listOf("cat_transport"), result.map { it.categoryId })
        assertTrue(client.lastPrompt!!.contains("cat_transport"))
        assertTrue(!client.lastPrompt!!.contains("cat_food"), "a budgeted category must not be sent")
    }

    @Test
    fun `empty when there is no spending to suggest against, without calling the model`() = runBlocking {
        val client = FakeGenAiClient { error("must not run with no averages") }
        val result = SuggestBudgetsUseCase(FakeBudgetRepository(emptyMap()), client)
            .invoke(categories, emptySet(), "en").getOrThrow()

        assertTrue(result.isEmpty())
        assertEquals(null, client.lastPrompt)
    }

    @Test
    fun `unavailable provider is a typed failure`() = runBlocking {
        val repo = FakeBudgetRepository(mapOf("cat_food" to 400.0))
        val result = SuggestBudgetsUseCase(repo, FakeGenAiClient(isAvailable = false))
            .invoke(categories, emptySet(), "en")

        assertTrue(result.exceptionOrNull() is GenAiException.Unavailable)
    }
}
