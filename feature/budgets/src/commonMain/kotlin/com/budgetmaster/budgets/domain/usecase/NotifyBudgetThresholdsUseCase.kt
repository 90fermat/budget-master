package com.budgetmaster.budgets.domain.usecase

import com.budgetmaster.budgets.domain.model.BudgetItem
import com.budgetmaster.budgets.domain.model.BudgetStatus
import com.budgetmaster.core.notifications.NotificationRepository

/**
 * Raises an in-app notification when a budget crosses a threshold.
 *
 * The notification id is derived from the budget, its period, and the threshold, so a given
 * alert is written at most once no matter how often budgets re-emit — `INSERT OR REPLACE`
 * makes repeat calls idempotent rather than spamming the inbox.
 */
class NotifyBudgetThresholdsUseCase(private val notifications: NotificationRepository) {

    suspend operator fun invoke(budgets: List<BudgetItem>) {
        budgets.forEach { budget ->
            when (budget.status) {
                BudgetStatus.EXCEEDED -> notify(budget, EXCEEDED)
                BudgetStatus.WARNING -> notify(budget, WARNING)
                BudgetStatus.OK -> Unit
            }
        }
    }

    private suspend fun notify(budget: BudgetItem, threshold: String) {
        val percent = (budget.ratio * 100).toInt()
        notifications.notify(
            id = "budget_${budget.id}_${budget.periodStart}_$threshold",
            title = budget.category.name,
            message = if (threshold == EXCEEDED) {
                "You've gone over your ${budget.category.name} budget ($percent%)."
            } else {
                "You're at $percent% of your ${budget.category.name} budget."
            },
        )
    }

    private companion object {
        const val WARNING = "warning"
        const val EXCEEDED = "exceeded"
    }
}
