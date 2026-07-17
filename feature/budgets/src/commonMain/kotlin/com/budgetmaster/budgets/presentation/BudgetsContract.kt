package com.budgetmaster.budgets.presentation

import com.budgetmaster.budgets.domain.model.BudgetCategory
import com.budgetmaster.budgets.domain.model.BudgetItem
import com.budgetmaster.budgets.domain.usecase.BudgetSuggestion

/** User actions on the Budgets screen. */
sealed interface BudgetsIntent {
    data object AddClicked : BudgetsIntent
    data class EditClicked(val item: BudgetItem) : BudgetsIntent
    data object EditorDismissed : BudgetsIntent
    data class SaveBudget(val categoryId: String, val limit: Double, val editingId: String?) : BudgetsIntent
    data class DeleteRequested(val id: String) : BudgetsIntent

    /** Ask the AI to propose budgets for categories that don't have one. */
    data object SuggestBudgets : BudgetsIntent

    /** Apply one proposed budget as a real budget. */
    data class ApplySuggestion(val suggestion: BudgetSuggestion) : BudgetsIntent

    /** Dismiss the suggestions without applying. */
    data object DismissSuggestions : BudgetsIntent
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
    /** True when an AI provider exists and the user opted in; gates the suggestion UI. */
    val aiEnabled: Boolean = false,
    val isSuggesting: Boolean = false,
    /** AI-proposed budgets awaiting the user's one-tap apply; empty until requested. */
    val suggestions: List<BudgetSuggestion> = emptyList(),
) {
    val isEmpty: Boolean get() = !isLoading && budgets.isEmpty()
    val totalLimit: Double get() = budgets.sumOf { it.limit }
    val totalSpent: Double get() = budgets.sumOf { it.spent }
}
