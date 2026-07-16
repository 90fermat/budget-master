package com.budgetmaster.transactions.domain.model

/**
 * A transaction enriched with its resolved [category], ready for display.
 *
 * The [amount] keeps the storage sign convention: negative = expense, positive = income.
 *
 * @property id Unique identifier.
 * @property amount Signed monetary amount (negative for expenses).
 * @property description Human-readable label (e.g. "Starbucks Coffee").
 * @property timestamp Epoch-millisecond time the transaction occurred.
 * @property category Resolved category, or `null` if uncategorized.
 * @property notes Optional free-text note.
 * @property accountId The wallet this entry belongs to.
 * @property isRecurring Whether the entry is flagged as recurring.
 */
data class TransactionItem(
    val id: String,
    val amount: Double,
    val description: String,
    val timestamp: Long,
    val category: TransactionCategory?,
    val notes: String?,
    val accountId: String = "",
    val isRecurring: Boolean = false,
) {
    /** True when this transaction is an outflow. */
    val isExpense: Boolean get() = amount < 0
}
