package com.budgetmaster.budgets.domain.model

/** A spending category available to budget against. */
data class BudgetCategory(
    val id: String,
    val name: String,
    val icon: String,
    val colorHex: String,
)

/** Health of a budget relative to its limit. */
enum class BudgetStatus { OK, WARNING, EXCEEDED }

/**
 * A category budget for the current period with its **live** spent amount
 * (computed from transactions, not the denormalized column).
 *
 * @property id Budget row id.
 * @property category The budgeted category (resolved).
 * @property limit The budget cap for the period.
 * @property spent Sum of expenses in this category during the period (>= 0).
 * @property periodStart / periodEnd Epoch-ms bounds of the budget period.
 */
data class BudgetItem(
    val id: String,
    val category: BudgetCategory,
    val limit: Double,
    val spent: Double,
    val periodStart: Long,
    val periodEnd: Long,
) {
    /** Spent fraction, clamped to [0,1] for the gauge. */
    val progress: Float get() = if (limit > 0) (spent / limit).coerceIn(0.0, 1.0).toFloat() else 0f

    /** Raw fraction (can exceed 1) for status decisions. */
    val ratio: Double get() = if (limit > 0) spent / limit else 0.0

    val remaining: Double get() = limit - spent

    val status: BudgetStatus
        get() = when {
            ratio > 1.0 -> BudgetStatus.EXCEEDED
            ratio >= 0.8 -> BudgetStatus.WARNING
            else -> BudgetStatus.OK
        }
}

/**
 * Input for creating or editing a budget.
 *
 * @property id Existing id when editing; null to create.
 * @property categoryId The category to budget.
 * @property limit The period cap (> 0).
 * @property periodStart / periodEnd Epoch-ms bounds (defaults to the current month).
 */
data class BudgetDraft(
    val id: String? = null,
    val categoryId: String,
    val limit: Double,
    val periodStart: Long,
    val periodEnd: Long,
)
