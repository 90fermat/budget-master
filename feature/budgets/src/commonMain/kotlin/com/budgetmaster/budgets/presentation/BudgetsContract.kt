package com.budgetmaster.budgets.presentation

import com.budgetmaster.budgets.domain.model.BudgetCategory
import com.budgetmaster.budgets.domain.model.BudgetItem

/** User actions on the Budgets screen. */
sealed interface BudgetsIntent {
    data object AddClicked : BudgetsIntent
    data class EditClicked(val item: BudgetItem) : BudgetsIntent
    data object EditorDismissed : BudgetsIntent
    data class SaveBudget(val categoryId: String, val limit: Double, val editingId: String?) : BudgetsIntent
    data class DeleteRequested(val id: String) : BudgetsIntent
}

/** One-shot side effects. */
sealed interface BudgetsEffect {
    data class ShowError(val message: String) : BudgetsEffect
}

/** Editor sheet/dialog state. */
data class BudgetsEditorState(
    val visible: Boolean = false,
    val editing: BudgetItem? = null,
)

/**
 * Immutable UI state of the Budgets screen.
 *
 * @property budgets Current-period budgets with live spent amounts.
 * @property categories Categories available for the picker.
 * @property currencyCode ISO currency for formatting.
 */
data class BudgetsState(
    val isLoading: Boolean = true,
    val budgets: List<BudgetItem> = emptyList(),
    val categories: List<BudgetCategory> = emptyList(),
    val currencyCode: String = "USD",
    val editor: BudgetsEditorState = BudgetsEditorState(),
) {
    val isEmpty: Boolean get() = !isLoading && budgets.isEmpty()
    val totalLimit: Double get() = budgets.sumOf { it.limit }
    val totalSpent: Double get() = budgets.sumOf { it.spent }
}
