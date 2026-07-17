@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.transactions.domain.usecase

import com.budgetmaster.core.util.DateUtils
import com.budgetmaster.transactions.domain.model.TransactionItem
import kotlin.math.abs
import kotlin.time.ExperimentalTime

/**
 * A charge that appears to repeat on a roughly monthly cadence — a likely subscription.
 *
 * @property typicalAmount the median magnitude across occurrences (positive).
 * @property occurrences how many distinct months it was seen in.
 * @property lastSeen epoch millis of the most recent occurrence.
 */
data class RecurringCharge(
    val description: String,
    val categoryId: String?,
    val typicalAmount: Double,
    val occurrences: Int,
    val lastSeen: Long,
)

/**
 * Finds likely subscriptions in the transaction list — entirely on device, no model involved.
 *
 * The heuristic: group expenses by a normalized description, keep groups that recur in at least
 * [minOccurrences] distinct months with amounts within [amountTolerance] of each other. That
 * "distinct months" test is what separates a subscription from three coffees in one week.
 *
 * Detection is deliberately local: which merchants a person pays every month is exactly the kind
 * of thing that shouldn't need to leave the device. A separate, optional step can ask the model
 * to *label* these once found, but the finding itself never does.
 */
class DetectRecurringChargesUseCase(
    private val minOccurrences: Int = 2,
    private val amountTolerance: Double = 0.15,
) {
    operator fun invoke(transactions: List<TransactionItem>): List<RecurringCharge> =
        transactions
            .filter { it.isExpense && it.description.isNotBlank() }
            .groupBy { normalize(it.description) }
            .mapNotNull { (_, group) -> group.toRecurringChargeOrNull() }
            .sortedByDescending { it.occurrences }

    private fun List<TransactionItem>.toRecurringChargeOrNull(): RecurringCharge? {
        // Distinct year-months: two charges in the same month are one occurrence, not two.
        val byMonth = groupBy { yearMonth(it.timestamp) }
        if (byMonth.size < minOccurrences) return null

        val amounts = map { abs(it.amount) }.sorted()
        val median = amounts[amounts.size / 2]
        if (median <= 0.0) return null

        // The amounts must actually be alike — a "subscription" whose price swings wildly is just
        // a frequent merchant, not a recurring charge.
        val consistent = amounts.all { abs(it - median) <= median * amountTolerance }
        if (!consistent) return null

        val newest = maxByOrNull { it.timestamp }!!
        return RecurringCharge(
            description = newest.description.trim(),
            categoryId = newest.category?.id,
            typicalAmount = median,
            occurrences = byMonth.size,
            lastSeen = newest.timestamp,
        )
    }

    /** Case/space-insensitive key so "Netflix", "netflix " and "NETFLIX" group together. */
    private fun normalize(description: String): String =
        description.trim().lowercase().replace(Regex("\\s+"), " ")

    private fun yearMonth(timestamp: Long): Int {
        val date = DateUtils.toLocalDate(timestamp)
        return date.year * 100 + date.monthNumber
    }
}
