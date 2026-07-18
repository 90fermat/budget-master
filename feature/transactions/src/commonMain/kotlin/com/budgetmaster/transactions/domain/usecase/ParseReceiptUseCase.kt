@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.transactions.domain.usecase

import com.budgetmaster.core.ai.GenAiClient
import com.budgetmaster.core.ai.GenAiException
import com.budgetmaster.core.ai.GenAiSchema
import com.budgetmaster.core.ocr.ReceiptImage
import com.budgetmaster.core.ocr.ReceiptTextRecognizer
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

/** Why a receipt scan produced nothing, so the UI can say something true. */
sealed class ReceiptScanError {
    /** No OCR or no AI provider on this platform/build. */
    data object Unavailable : ReceiptScanError()

    /** The photo yielded no readable text — blurry, dark, or not a receipt. */
    data object NoTextFound : ReceiptScanError()

    /** Text was read, but no total could be identified in it. */
    data object NoAmount : ReceiptScanError()

    /** Network, rate limit, or anything else transient. */
    data object Failed : ReceiptScanError()
}

/** Carries a typed [ReceiptScanError] through `Result.failure`. */
class ReceiptScanException(
    val error: ReceiptScanError,
    cause: Throwable? = null,
) : Exception(error.toString(), cause)

/**
 * Turns a photographed receipt into a draft transaction the user confirms.
 *
 * Two stages, and the split is the privacy design:
 * 1. **OCR runs on the device** ([ReceiptTextRecognizer], ML Kit). The image itself never leaves
 *    the phone — a receipt photo shows the card's last digits, the address, the full basket.
 * 2. Only the **extracted text** goes to the model, trimmed to [MAX_TEXT_CHARS], to pull out the
 *    total, merchant and date.
 *
 * Reuses [QuickEntryDraft] because the outcome is the same thing quick-add produces: fields for
 * the editor, never a saved transaction.
 */
class ParseReceiptUseCase(
    private val recognizer: ReceiptTextRecognizer,
    private val genAiClient: GenAiClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    /** True only when both stages can run; the UI hides the scan action otherwise. */
    val isAvailable: Boolean get() = recognizer.isAvailable && genAiClient.isAvailable

    suspend operator fun invoke(
        image: ReceiptImage,
        categories: List<TransactionCategory>,
        now: Long = Clock.System.now().toEpochMilliseconds(),
    ): Result<QuickEntryDraft> {
        if (!isAvailable) return Result.failure(ReceiptScanException(ReceiptScanError.Unavailable))

        val text = recognizer.recognizeText(image)
            ?: return Result.failure(ReceiptScanException(ReceiptScanError.NoTextFound))

        val allowedIds = categories.map { it.id }
        return try {
            val raw = genAiClient.generateJson(prompt(text, categories), schema(allowedIds))
            val dto = json.decodeFromString<ReceiptDto>(raw)
            val amount = dto.total
                ?: return Result.failure(ReceiptScanException(ReceiptScanError.NoAmount))

            Result.success(
                QuickEntryDraft(
                    amountAbs = kotlin.math.abs(amount),
                    // A receipt is a purchase. Refunds exist, but defaulting to "income" on a
                    // misread would silently inflate the user's balance.
                    isExpense = true,
                    description = dto.merchant?.takeIf { it.isNotBlank() } ?: "",
                    categoryId = dto.categoryId?.takeIf { it in allowedIds },
                    timestamp = resolveTimestamp(dto.daysAgo ?: 0, now),
                ),
            )
        } catch (e: GenAiException) {
            Result.failure(ReceiptScanException(ReceiptScanError.Failed, e))
        } catch (e: Exception) {
            Result.failure(ReceiptScanException(ReceiptScanError.Failed, e))
        }
    }

    private fun prompt(text: String, categories: List<TransactionCategory>) = buildString {
        append(
            "Extract the purchase from this receipt text. Return JSON with: total (the final " +
                "amount paid as a positive number — the grand total, not a line item or subtotal), " +
                "merchant (the shop name), categoryId (best fit from the list, omit if unsure), " +
                "and daysAgo (0 if the receipt is dated today or has no date, 1 for yesterday, " +
                "etc.). Do not invent a total that isn't present — omit it instead.\n\n",
        )
        append("Categories (id: name):\n")
        categories.forEach { append("- ${it.id}: ${it.name}\n") }
        append("\nReceipt text:\n")
        append(text.take(MAX_TEXT_CHARS))
    }

    private fun schema(allowedIds: List<String>) = GenAiSchema.Obj(
        properties = mapOf(
            "total" to GenAiSchema.Str(description = "The grand total as a positive number."),
            "merchant" to GenAiSchema.Str(),
            "categoryId" to GenAiSchema.Enumeration(allowedIds),
            "daysAgo" to GenAiSchema.Str(description = "Whole number of days before today."),
        ),
        optional = listOf("total", "merchant", "categoryId", "daysAgo"),
    )

    /** Midnight of the receipt's day — a purchase is recorded by day, not to the second. */
    private fun resolveTimestamp(daysAgo: Int, now: Long): Long {
        val target = DateUtils.toLocalDate(now).plus(-daysAgo.coerceAtLeast(0), DateTimeUnit.DAY)
        return target.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }

    /** Numbers arrive as strings ("12.50", "12,50"), so parse leniently rather than fail decoding. */
    @Serializable
    private data class ReceiptDto(
        @SerialName("total") private val totalRaw: String? = null,
        val merchant: String? = null,
        val categoryId: String? = null,
        @SerialName("daysAgo") private val daysAgoRaw: String? = null,
    ) {
        val total: Double? get() = totalRaw?.replace(',', '.')?.trim()?.toDoubleOrNull()
        val daysAgo: Int? get() = daysAgoRaw?.trim()?.toIntOrNull()
    }

    private companion object {
        /** Receipts can OCR into a wall of text; the total and merchant are near the top/bottom,
         *  and an unbounded prompt is a cost and latency risk on a free tier. */
        const val MAX_TEXT_CHARS = 4000
    }
}
