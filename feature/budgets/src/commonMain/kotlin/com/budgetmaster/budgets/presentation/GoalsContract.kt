package com.budgetmaster.budgets.presentation

import com.budgetmaster.budgets.domain.model.GoalItem

/** User actions on the Goals screen. */
sealed interface GoalsIntent {
    data object AddClicked : GoalsIntent
    data class EditClicked(val item: GoalItem) : GoalsIntent
    data object EditorDismissed : GoalsIntent
    data class SaveGoal(val name: String, val targetAmount: Double, val editingId: String?) : GoalsIntent
    data class DeleteRequested(val id: String) : GoalsIntent
    data class ContributeClicked(val item: GoalItem) : GoalsIntent
    data object ContributeDismissed : GoalsIntent
    data class SubmitContribution(val id: String, val amount: Double) : GoalsIntent
}

/** One-shot side effects. */
sealed interface GoalsEffect {
    data class ShowError(val message: String) : GoalsEffect
}

data class GoalsEditorState(val visible: Boolean = false, val editing: GoalItem? = null)
data class ContributeState(val visible: Boolean = false, val goal: GoalItem? = null)

/**
 * Immutable UI state of the Goals screen.
 */
data class GoalsState(
    val isLoading: Boolean = true,
    val goals: List<GoalItem> = emptyList(),
    val currencyCode: String = "USD",
    val editor: GoalsEditorState = GoalsEditorState(),
    val contribute: ContributeState = ContributeState(),
) {
    val isEmpty: Boolean get() = !isLoading && goals.isEmpty()
}
