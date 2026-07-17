package com.budgetmaster.transactions.domain

import com.budgetmaster.core.ai.GenAiClient
import com.budgetmaster.core.ai.GenAiException
import com.budgetmaster.core.ai.GenAiSchema
import com.budgetmaster.transactions.InMemoryKeyValueStore
import com.budgetmaster.transactions.domain.model.TransactionCategory
import com.budgetmaster.transactions.domain.usecase.SuggestCategoryUseCase
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class CountingGenAiClient(
    override val isAvailable: Boolean = true,
    private val respond: () -> String = { """{"categoryId":"cat_food"}""" },
) : GenAiClient {
    var calls = 0
    override suspend fun generateJson(prompt: String, schema: GenAiSchema): String {
        calls++
        return respond()
    }
}

class SuggestCategoryUseCaseTest {

    private val categories = listOf(
        TransactionCategory("cat_food", "Food & Dining", "🍔", "#F59E0B"),
        TransactionCategory("cat_transport", "Transport", "🚗", "#6366F1"),
    )

    @Test
    fun `suggests a category and caches it so the same merchant is asked once`() = runBlocking {
        val client = CountingGenAiClient()
        val useCase = SuggestCategoryUseCase(client, InMemoryKeyValueStore())

        assertEquals("cat_food", useCase("Starbucks", categories))
        // Case/space variations hit the same cache key — no second model call.
        assertEquals("cat_food", useCase("  starbucks ", categories))
        assertEquals(1, client.calls, "a learned merchant must not be asked again")
    }

    @Test
    fun `rejects a category id that isn't offered`() = runBlocking {
        val client = CountingGenAiClient { """{"categoryId":"cat_made_up"}""" }
        assertNull(SuggestCategoryUseCase(client, InMemoryKeyValueStore())("X", categories))
    }

    @Test
    fun `returns null when no provider, without calling out`() = runBlocking {
        val client = CountingGenAiClient(isAvailable = false) { error("must not be called") }
        assertNull(SuggestCategoryUseCase(client, InMemoryKeyValueStore())("Starbucks", categories))
        assertEquals(0, client.calls)
    }

    @Test
    fun `returns null for a blank description`() = runBlocking {
        val client = CountingGenAiClient()
        assertNull(SuggestCategoryUseCase(client, InMemoryKeyValueStore())("   ", categories))
        assertEquals(0, client.calls)
    }

    @Test
    fun `a model failure is swallowed to null - a suggestion is only a nicety`() = runBlocking {
        val client = object : GenAiClient {
            override val isAvailable = true
            override suspend fun generateJson(prompt: String, schema: GenAiSchema): String =
                throw GenAiException.RateLimited()
        }
        assertNull(SuggestCategoryUseCase(client, InMemoryKeyValueStore())("Starbucks", categories))
    }
}
