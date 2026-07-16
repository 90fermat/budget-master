package com.budgetmaster.dashboard.presentation

import com.budgetmaster.core.model.Transaction
import com.budgetmaster.dashboard.domain.model.TransactionType

/**
 * One-shot side effects emitted by [DashboardViewModel] and consumed exactly once by the UI.
 *
 * Effects are delivered via a `SharedFlow` and must never be stored in [DashboardState].
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
     * Navigate to the Analytics / Reports screen.
     */
    data object NavigateToAnalytics : DashboardEffect

    /**
     * Navigate to Settings (also where notifications currently live).
     *
     * Replaced a stringly-typed `onQuickAction("Settings")` callback.
     */
    data object NavigateToSettings : DashboardEffect

    /**
     * Navigate to the detail page for a specific budget category.
     *
     * @property budgetId The unique identifier of the budget to open.
     */
    data class NavigateToBudgetDetail(val budgetId: String) : DashboardEffect

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
