package com.budgetmaster.transactions.presentation.recurring

import com.budgetmaster.transactions.domain.model.RecurringDraft
import com.budgetmaster.transactions.domain.model.RecurringTransaction
import com.budgetmaster.transactions.domain.model.TransactionAccount
import com.budgetmaster.transactions.domain.model.TransactionCategory

/** User actions on the Recurring screen. */
sealed interface RecurringIntent {
    data object AddClicked : RecurringIntent
    data class EditClicked(val item: RecurringTransaction) : RecurringIntent
    data object EditorDismissed : RecurringIntent
    data class Save(val draft: RecurringDraft) : RecurringIntent
    data class SetActive(val id: String, val active: Boolean) : RecurringIntent
    data class Delete(val id: String) : RecurringIntent
}

/** One-shot side effects. */
sealed interface RecurringEffect {
    data class ShowError(val message: String) : RecurringEffect
}

/** State of the editor sheet/dialog. */
data class RecurringEditorState(
    val visible: Boolean = false,
    val editing: RecurringTransaction? = null,
)

/** Immutable UI state of the Recurring screen. */
data class RecurringState(
    val isLoading: Boolean = true,
    val schedules: List<RecurringTransaction> = emptyList(),
    val categories: List<TransactionCategory> = emptyList(),
    val accounts: List<TransactionAccount> = emptyList(),
    val currencyCode: String = "USD",
    val editor: RecurringEditorState = RecurringEditorState(),
) {
    val isEmpty: Boolean get() = !isLoading && schedules.isEmpty()
}
