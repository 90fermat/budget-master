@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.transactions.domain.usecase

import com.budgetmaster.core.ai.GenAiClient
import com.budgetmaster.core.ai.GenAiException
import com.budgetmaster.core.ai.GenAiSchema
import com.budgetmaster.core.util.DateUtils
import com.budgetmaster.transactions.domain.model.TransactionCategory
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The fields a natural-language entry resolves to, ready to prefill the editor.
 *
 * Nothing here is committed: the user still sees and confirms the editor. AI drafts the entry;
 * it never silently records money.
 *
 * @property categoryId a seeded category id the model chose, or null if it wasn't sure.
 * @property timestamp resolved from a relative phrase ("yesterday") to an epoch millisecond.
 */
data class QuickEntryDraft(
    val amountAbs: Double,
    val isExpense: Boolean,
    val description: String,
    val categoryId: String?,
    val timestamp: Long,
)

/** What went wrong parsing, so the UI can say something true rather than a generic failure. */
sealed class QuickEntryError {
    /** No AI provider on this platform/build — the caller should hide the quick-add field. */
    data object Unavailable : QuickEntryError()

    /** The model couldn't find an amount in the text; the entry is unusable without one. */
    data object NoAmount : QuickEntryError()

    /** Network, rate limit, or anything else transient. */
    data object Failed : QuickEntryError()
}

/**
 * Turns "coffee 4.50 yesterday" into a draft transaction the user can confirm.
 *
 * Local-first: the whole point is speed, so the text is short and no ledger data is involved —
 * only what the user just typed is sent. The date is resolved **on device** from the relative
 * days the model returns, never by asking it for a wall-clock timestamp it would guess wrong.
 *
 * @property allowedCategoryIds the seeded ids the model may pick from, so it can't invent one.
 */
class ParseQuickEntryUseCase(
    private val genAiClient: GenAiClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    val isAvailable: Boolean get() = genAiClient.isAvailable

    suspend operator fun invoke(
        text: String,
        categories: List<TransactionCategory>,
        now: Long = Clock.System.now().toEpochMilliseconds(),
    ): Result<QuickEntryDraft> {
        if (!genAiClient.isAvailable) return Result.failure(QuickEntryException(QuickEntryError.Unavailable))
        if (text.isBlank()) return Result.failure(QuickEntryException(QuickEntryError.NoAmount))

        val allowedIds = categories.map { it.id }
        val prompt = buildPrompt(text, categories)

        return try {
            val raw = genAiClient.generateJson(prompt, schema(allowedIds))
            val dto = json.decodeFromString<QuickEntryDto>(raw)
            val amount = dto.amount
                ?: return Result.failure(QuickEntryException(QuickEntryError.NoAmount))

            Result.success(
                QuickEntryDraft(
                    amountAbs = kotlin.math.abs(amount),
                    isExpense = dto.isExpense ?: true,
                    description = dto.description?.takeIf { it.isNotBlank() } ?: text.trim(),
                    // Only accept an id we offered; a hallucinated one becomes "uncategorized".
                    categoryId = dto.categoryId?.takeIf { it in allowedIds },
                    timestamp = resolveTimestamp(dto.daysAgo ?: 0, now),
                ),
            )
        } catch (e: GenAiException.RateLimited) {
            Result.failure(QuickEntryException(QuickEntryError.Failed, e))
        } catch (e: GenAiException) {
            Result.failure(QuickEntryException(QuickEntryError.Failed, e))
        } catch (e: Exception) {
            // A malformed response is a parse failure, not a crash.
            Result.failure(QuickEntryException(QuickEntryError.Failed, e))
        }
    }

    private fun buildPrompt(text: String, categories: List<TransactionCategory>): String = buildString {
        append(
            "Extract a single personal-finance transaction from the user's note. Return JSON with: " +
                "amount (a positive number, the magnitude), isExpense (true for money spent, false " +
                "for money received), description (a short human label), categoryId (the best fit " +
                "from the list, or omit if unsure), daysAgo (0 for today, 1 for yesterday, etc.; 0 " +
                "if no date is mentioned). Do not guess an amount that isn't there — omit it.",
        )
        append("\n\nCategories (id: name):\n")
        categories.forEach { append("- ${it.id}: ${it.name}\n") }
        append("\nUser note: ")
        append(text.trim())
    }

    private fun schema(allowedIds: List<String>) = GenAiSchema.Obj(
        properties = mapOf(
            "amount" to GenAiSchema.Str(description = "Positive number; omit if none is present."),
            "isExpense" to GenAiSchema.Str(description = "true or false."),
            "description" to GenAiSchema.Str(),
            "categoryId" to GenAiSchema.Enumeration(allowedIds),
            "daysAgo" to GenAiSchema.Str(description = "Whole number of days before today."),
        ),
        optional = listOf("amount", "isExpense", "description", "categoryId", "daysAgo"),
    )

    /** Midnight of the target day in the device zone — a quick entry means the day, not the second. */
    private fun resolveTimestamp(daysAgo: Int, now: Long): Long {
        val today = DateUtils.toLocalDate(now)
        val target = today.plus(-daysAgo.coerceAtLeast(0), DateTimeUnit.DAY)
        return target.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }

    /**
     * The DTO is intentionally all-nullable strings for the numbers: models return "4.50", 4.5,
     * or "4,50" interchangeably, so parsing leniently here beats a decode that throws on the shape.
     */
    @Serializable
    private data class QuickEntryDto(
        @SerialName("amount") private val amountRaw: String? = null,
        @SerialName("isExpense") private val isExpenseRaw: String? = null,
        val description: String? = null,
        val categoryId: String? = null,
        @SerialName("daysAgo") private val daysAgoRaw: String? = null,
    ) {
        val amount: Double? get() = amountRaw?.replace(',', '.')?.trim()?.toDoubleOrNull()
        val isExpense: Boolean? get() = isExpenseRaw?.trim()?.lowercase()?.toBooleanStrictOrNull()
        val daysAgo: Int? get() = daysAgoRaw?.trim()?.toIntOrNull()
    }
}

/** Carries a typed [QuickEntryError] through `Result.failure`. */
class QuickEntryException(
    val error: QuickEntryError,
    cause: Throwable? = null,
) : Exception(error.toString(), cause)
