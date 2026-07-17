package com.budgetmaster.reports.domain.usecase

import com.budgetmaster.core.ai.GenAiClient
import com.budgetmaster.core.ai.GenAiException
import com.budgetmaster.core.ai.GenAiSchema
import com.budgetmaster.reports.domain.model.ReportSummary
import kotlin.math.roundToInt

/**
 * Builds the aggregates-only context both AI report features share.
 *
 * Only per-category totals, the period income/expense sums, and the previous-period comparison
 * are described — never a single transaction, description, or date. The whole point of computing
 * a report locally is that the raw ledger can stay on the device.
 */
private fun ReportSummary.toAggregateContext(): String = buildString {
    append("Currency: $currencyCode\n")
    append("Total income: ${totalIncome.roundedCents()}\n")
    append("Total expenses: ${totalExpenses.roundedCents()}\n")
    append("Net: ${net.roundedCents()}\n")
    append("Previous period — income: ${previousIncome.roundedCents()}, expenses: ${previousExpenses.roundedCents()}\n")
    if (categories.isEmpty()) {
        append("No spending by category.\n")
    } else {
        append("Spending by category:\n")
        categories.sortedByDescending { it.amount }.forEach { c ->
            append("- ${c.name}: ${c.amount.roundedCents()} (${(c.share * 100).roundToInt()}%)\n")
        }
    }
}

private fun Double.roundedCents(): String = ((this * 100).roundToInt() / 100.0).toString()

/**
 * Writes a short human "story" of the period from the report aggregates ("Dining up 18%, mostly
 * weekends…"), in the user's language.
 *
 * Sends aggregates only; the raw ledger never leaves the device. Returns a failure when no
 * provider is configured so the UI can hide the card rather than show an empty one.
 */
class GenerateNarrativeUseCase(private val genAiClient: GenAiClient) {

    val isAvailable: Boolean get() = genAiClient.isAvailable

    suspend operator fun invoke(report: ReportSummary, languageTag: String): Result<String> {
        if (!genAiClient.isAvailable) return Result.failure(GenAiException.Unavailable())
        if (report.isEmpty) return Result.success("")

        val prompt = buildString {
            append(
                "You are a friendly personal-finance coach. Write a short 2-3 sentence summary of " +
                    "this month's spending in the language with BCP-47 tag '$languageTag'. Be " +
                    "specific with the numbers given, note the biggest categories and any change " +
                    "vs the previous period, and stay encouraging rather than judgmental. Return " +
                    "only the summary text, no preamble.\n\n",
            )
            append(report.toAggregateContext())
        }

        return try {
            Result.success(genAiClient.generateJson(prompt, NARRATIVE_SCHEMA).trim())
        } catch (e: GenAiException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(GenAiException.Failed(e.message ?: "Narrative failed", e))
        }
    }

    private companion object {
        // A single string field: the schema is what makes the model return the sentence alone
        // rather than wrapping it in prose or an object.
        val NARRATIVE_SCHEMA = GenAiSchema.Str(description = "2-3 sentence spending summary.")
    }
}

/**
 * Answers a free-text finance question ("How much did I spend on food?") from the report
 * aggregates, in the user's language.
 *
 * The model is given the same aggregated context as the narrative — never raw transactions — and
 * asked to answer from it, or to say it can't when the data doesn't cover the question. That last
 * instruction matters: an AI that invents a number for a money question is worse than one that
 * admits it doesn't know.
 */
class AnswerFinanceQuestionUseCase(private val genAiClient: GenAiClient) {

    val isAvailable: Boolean get() = genAiClient.isAvailable

    suspend operator fun invoke(
        question: String,
        report: ReportSummary,
        languageTag: String,
    ): Result<String> {
        if (!genAiClient.isAvailable) return Result.failure(GenAiException.Unavailable())
        if (question.isBlank()) return Result.success("")

        val prompt = buildString {
            append(
                "Answer the user's question using ONLY the spending summary below, in the language " +
                    "with BCP-47 tag '$languageTag'. Be concise and specific with the numbers. If " +
                    "the summary doesn't contain enough to answer, say so plainly instead of " +
                    "guessing. Return only the answer.\n\n",
            )
            append(report.toAggregateContext())
            append("\nQuestion: ")
            append(question.trim())
        }

        return try {
            Result.success(genAiClient.generateJson(prompt, ANSWER_SCHEMA).trim())
        } catch (e: GenAiException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(GenAiException.Failed(e.message ?: "Q&A failed", e))
        }
    }

    private companion object {
        val ANSWER_SCHEMA = GenAiSchema.Str(description = "A concise answer to the question.")
    }
}
