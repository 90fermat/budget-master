package com.budgetmaster.transactions.domain.usecase

import com.budgetmaster.core.ai.GenAiClient
import com.budgetmaster.core.ai.GenAiException
import com.budgetmaster.core.ai.GenAiSchema
import com.budgetmaster.core.prefs.KeyValueStore
import com.budgetmaster.transactions.domain.model.TransactionCategory
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Suggests a category for a typed description ("Starbucks" → Food & Dining), so a manual entry
 * gets the same help quick-add gives.
 *
 * **Each merchant is asked at most once.** A learned `description → categoryId` pair is cached
 * locally in [KeyValueStore]; a cache hit returns instantly and never calls the model, which
 * keeps this cheap enough to run as the user types and means a merchant's category is stable
 * once learned. The model may only return one of the offered ids, so it can't invent a category.
 */
class SuggestCategoryUseCase(
    private val genAiClient: GenAiClient,
    private val store: KeyValueStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    val isAvailable: Boolean get() = genAiClient.isAvailable

    /**
     * @return a seeded/user category id, or null if none could be determined (which is fine — the
     *   user just picks one).
     */
    suspend operator fun invoke(
        description: String,
        categories: List<TransactionCategory>,
    ): String? {
        if (!genAiClient.isAvailable || description.isBlank() || categories.isEmpty()) return null

        val key = cacheKey(description)
        val allowedIds = categories.map { it.id }

        // Cache hit: return it, but only if that category still exists (it may have been deleted).
        store.observeString(key).first()?.let { cached ->
            if (cached in allowedIds) return cached
        }

        return try {
            val raw = genAiClient.generateJson(prompt(description, categories), schema(allowedIds))
            val suggested = json.decodeFromString<SuggestionDto>(raw).categoryId?.takeIf { it in allowedIds }
            if (suggested != null) store.putString(key, suggested)
            suggested
        } catch (e: GenAiException) {
            null // A suggestion is a nicety; never surface its failure.
        } catch (e: Exception) {
            null
        }
    }

    private fun prompt(description: String, categories: List<TransactionCategory>) = buildString {
        append(
            "Pick the single best category id for this transaction description, or omit if none " +
                "fits. Return JSON with just categoryId.\n\nCategories (id: name):\n",
        )
        categories.forEach { append("- ${it.id}: ${it.name}\n") }
        append("\nDescription: ")
        append(description.trim())
    }

    private fun schema(allowedIds: List<String>) = GenAiSchema.Obj(
        properties = mapOf("categoryId" to GenAiSchema.Enumeration(allowedIds)),
        optional = listOf("categoryId"),
    )

    /** Normalized so "Starbucks", "starbucks " and "STARBUCKS" share one learned entry. */
    private fun cacheKey(description: String): String =
        CACHE_PREFIX + description.trim().lowercase().replace(Regex("\\s+"), " ")

    @Serializable
    private data class SuggestionDto(val categoryId: String? = null)

    private companion object {
        const val CACHE_PREFIX = "category_suggestion."
    }
}
