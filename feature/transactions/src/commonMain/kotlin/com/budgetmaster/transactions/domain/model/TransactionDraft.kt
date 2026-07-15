package com.budgetmaster.transactions.domain.model

/**
 * Input for creating or editing a transaction, before persistence.
 *
 * @property id Existing id when editing; `null` when creating (repository assigns one).
 * @property amountAbs Non-negative magnitude entered by the user; the sign is derived
 *   from [isExpense] at save time.
 * @property isExpense Whether this is an outflow (true) or inflow (false).
 * @property description Label for the transaction.
 * @property categoryId Selected category id, or `null` for uncategorized.
 * @property timestamp Epoch-millisecond time the transaction occurred.
 * @property notes Optional free-text note.
 */
data class TransactionDraft(
    val id: String? = null,
    val amountAbs: Double,
    val isExpense: Boolean,
    val description: String,
    val categoryId: String?,
    val timestamp: Long,
    val notes: String? = null,
)
