package com.budgetmaster.dashboard.domain.model

/**
 * Represents the usage status of a specific category budget.
 */
enum class BudgetStatus {
    /**
     * Budget is within safe limits (usually under warning threshold).
     */
    OK,

    /**
     * Budget is nearing its limit (typically exceeded the threshold like 80% or 90%).
     */
    WARNING,

    /**
     * Budget spending has exceeded the configured limit.
     */
    EXCEEDED
}

/**
 * Domain representation of a budget's spending progress.
 *
 * @property categoryId The unique identifier of the budget category.
 * @property categoryName The display name of the category (e.g., "Dining Out").
 * @property categoryEmoji An optional unicode emoji character representing the category.
 * @property spent The total amount spent in this category for the current period.
 * @property limit The spending limit set for this budget category.
 * @property percentage The percentage of the limit spent (range from 0.0 upwards).
 * @property status The safety/limit status of the budget (OK, WARNING, or EXCEEDED).
 */
data class BudgetProgress(
    val categoryId: String,
    val categoryName: String,
    val categoryEmoji: String?,
    val spent: Double,
    val limit: Double,
    val percentage: Double,
    val status: BudgetStatus
)
