package com.budgetmaster.budgets.domain.usecase

import com.budgetmaster.budgets.domain.model.BudgetItem
import com.budgetmaster.budgets.domain.model.BudgetStatus
import com.budgetmaster.core.notifications.NotificationRepository
import org.jetbrains.compose.resources.getString
import com.budgetmaster.core.designsystem.categoryNameRes
import budgetmaster.core.generated.resources.notif_budget_warning_body
import budgetmaster.core.generated.resources.notif_budget_exceeded_body
import budgetmaster.core.generated.resources.Res

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
        // Resolved at write time in the current language: a notification is a historical record,
        // and the category is localised because default categories are stored as English literals.
        val categoryName = categoryNameRes(budget.category.id)?.let { getString(it) }
            ?: budget.category.name
        val percentText = "${(budget.ratio * 100).toInt()}%"
        val message = getString(
            if (threshold == EXCEEDED) Res.string.notif_budget_exceeded_body
            else Res.string.notif_budget_warning_body,
            categoryName,
            percentText,
        )
        notifications.notify(
            id = "budget_${budget.id}_${budget.periodStart}_$threshold",
            title = categoryName,
            message = message,
        )
    }

    private companion object {
        const val WARNING = "warning"
        const val EXCEEDED = "exceeded"
    }
}
