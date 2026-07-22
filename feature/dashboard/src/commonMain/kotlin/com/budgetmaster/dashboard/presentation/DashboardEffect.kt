package com.budgetmaster.dashboard.presentation

import com.budgetmaster.core.model.Transaction
import com.budgetmaster.dashboard.domain.model.TransactionType

/**
 * One-shot side effects emitted by [DashboardViewModel] and consumed exactly once by the UI.
 *
 * Effects are delivered via a `SharedFlow` and must never be stored in [DashboardState].
 *
 * Every case here must be emitted by the ViewModel *and* handled by the screen. Two cases
 * (NavigateToAnalytics, NavigateToBudgetDetail) were removed because nothing emitted them - they
 * were speculative API that an `else ->` branch in the collector made invisible. The collector is
 * now exhaustive, so an unhandled effect is a compile error.
 */
sealed interface DashboardEffect {
    /**
     * Navigate to the full Transactions list screen.
     */
    data object NavigateToTransactions : DashboardEffect

    /**
     * Navigate to the Add Transaction bottom sheet or screen.
     *
     * @property type The pre-selected transaction type (INCOME / EXPENSE / TRANSFER).
     */
    data class NavigateToAddTransaction(val type: TransactionType) : DashboardEffect

    /**
     * Open the notifications inbox.
     *
     * The bell used to route here to Settings, because there was no inbox and the contentDescription
     * lied to screen readers about the destination. Now it opens the real thing.
     */
    data object NavigateToNotifications : DashboardEffect

    /**
     * Show a Snackbar offering to undo a recently deleted transaction.
     *
     * @property transaction The deleted [Transaction] that can be restored.
     */
    data class ShowUndoDelete(val transaction: Transaction) : DashboardEffect

    /**
     * Show a generic error Snackbar with a descriptive message.
     *
     * @property message The error message to display.
     */
    data class ShowError(val message: String) : DashboardEffect
}
