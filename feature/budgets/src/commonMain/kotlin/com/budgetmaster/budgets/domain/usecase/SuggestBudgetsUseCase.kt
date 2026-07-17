package com.budgetmaster.budgets.domain.usecase

import com.budgetmaster.budgets.domain.model.BudgetCategory
import com.budgetmaster.budgets.domain.repository.BudgetRepository
import com.budgetmaster.core.ai.GenAiClient
import com.budgetmaster.core.ai.GenAiException
import com.budgetmaster.core.ai.GenAiSchema
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

/**
 * A proposed budget the user can apply with one tap.
 *
 * @property suggestedLimit a round monthly limit, set a little above the recent average.
 * @property rationale a short human reason, in the user's language.
 */
data class BudgetSuggestion(
    val categoryId: String,
    val categoryName: String,
    val suggestedLimit: Double,
    val rationale: String,
)

/**
 * Proposes per-category monthly limits from the last three months of spending.
 *
 * The **averages are computed locally** (SQL, on device); only those aggregates and the category
 * names go to the model, never a transaction. The model's job is judgment — a sensible round
 * limit and a one-line reason — not arithmetic, so [BudgetSuggestion.suggestedLimit] is clamped
 * to a sane band around the real average even if the model returns something odd.
 *
 * Categories that already have a budget are skipped: this suggests where the user has none.
 */
class SuggestBudgetsUseCase(
    private val repository: BudgetRepository,
    private val genAiClient: GenAiClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    val isAvailable: Boolean get() = genAiClient.isAvailable

    /**
     * @param categories all categories, to resolve names and icons.
     * @param existingBudgetCategoryIds categories that already have a budget, which are excluded.
     */
    suspend operator fun invoke(
        categories: List<BudgetCategory>,
        existingBudgetCategoryIds: Set<String>,
        languageTag: String,
    ): Result<List<BudgetSuggestion>> {
        if (!genAiClient.isAvailable) return Result.failure(GenAiException.Unavailable())

        val averages = repository.averageMonthlySpendingByCategory(MONTHS)
            .filterKeys { it !in existingBudgetCategoryIds }
            .filterValues { it > 0.0 }
        if (averages.isEmpty()) return Result.success(emptyList())

        val nameById = categories.associate { it.id to it.name }
        val prompt = buildPrompt(averages, nameById, languageTag)

        return try {
            val raw = genAiClient.generateJson(prompt, SCHEMA)
            val dtos = json.decodeFromString<List<SuggestionDto>>(raw)
            Result.success(
                dtos.mapNotNull { dto ->
                    val average = averages[dto.categoryId] ?: return@mapNotNull null
                    BudgetSuggestion(
                        categoryId = dto.categoryId,
                        categoryName = nameById[dto.categoryId] ?: dto.categoryId,
                        suggestedLimit = sanitizeLimit(dto.limit, average),
                        rationale = dto.rationale?.takeIf { it.isNotBlank() } ?: "",
                    )
                },
            )
        } catch (e: GenAiException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(GenAiException.Failed(e.message ?: "Budget suggestions failed", e))
        }
    }

    private fun buildPrompt(
        averages: Map<String, Double>,
        nameById: Map<String, String>,
        languageTag: String,
    ): String = buildString {
        append(
            "You are a budgeting assistant. For each category below, propose a sensible monthly " +
                "budget limit — a round number a little above the recent average, giving some " +
                "headroom without being wasteful — and a one-sentence rationale in the language " +
                "with BCP-47 tag '$languageTag'. Return a JSON array of objects with categoryId, " +
                "suggestedLimit (a number), and rationale.\n\n",
        )
        append("Average monthly spending over the last $MONTHS months:\n")
        averages.forEach { (id, avg) ->
            append("- $id (${nameById[id] ?: id}): ${(avg * 100).roundToInt() / 100.0}\n")
        }
    }

    /**
     * Keeps the model's number in a defensible band: never below the average (that budget is set
     * to fail) and never a runaway multiple of it. The model chooses within reason; this stops
     * an outlier from landing in the user's budget.
     */
    private fun sanitizeLimit(modelValue: Double?, average: Double): Double {
        val floor = average
        val ceiling = average * 2
        val proposed = modelValue?.takeIf { it.isFinite() && it > 0 } ?: (average * 1.15)
        return proposed.coerceIn(floor, ceiling)
    }

    @Serializable
    private data class SuggestionDto(
        val categoryId: String,
        // A string because models return "50", 50, or "50.00" interchangeably; parse leniently.
        val suggestedLimit: String? = null,
        val rationale: String? = null,
    ) {
        val limit: Double? get() = suggestedLimit?.replace(',', '.')?.trim()?.toDoubleOrNull()
    }

    private companion object {
        const val MONTHS = 3

        val SCHEMA = GenAiSchema.Arr(
            items = GenAiSchema.Obj(
                properties = mapOf(
                    "categoryId" to GenAiSchema.Str(),
                    "suggestedLimit" to GenAiSchema.Str(description = "A number: the monthly limit."),
                    "rationale" to GenAiSchema.Str(),
                ),
                optional = listOf("rationale"),
            ),
        )
    }
}
