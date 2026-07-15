package com.budgetmaster.dashboard.presentation

import com.budgetmaster.dashboard.domain.model.Period
import com.budgetmaster.dashboard.domain.model.TransactionType

/**
 * All user actions and UI events that can be dispatched to the [DashboardViewModel].
 */
sealed interface DashboardIntent {
    /**
     * Triggers the initial data load for the Dashboard for a given [period].
     *
     * @property period The time range to load data for.
     */
    data class LoadDashboard(val period: Period) : DashboardIntent

    /**
     * Requests a full data refresh (pull-to-refresh or manual reload).
     * Sets [DashboardState.isRefreshing] to `true` while running.
     */
    data object RefreshRequested : DashboardIntent

    /**
     * Changes the active period filter, then re-fetches chart and balance data.
     *
     * @property period The newly selected time range.
     */
    data class PeriodChanged(val period: Period) : DashboardIntent

    /**
     * Indicates that the user swiped a transaction row to delete it.
     * Triggers deletion and emits a [DashboardEffect.ShowUndoDelete] effect.
     *
     * @property id The unique identifier of the swiped transaction.
     */
    data class TransactionSwiped(val id: String) : DashboardIntent

    /**
     * Dismisses an AI-generated insight from the dashboard feed.
     *
     * @property id The unique identifier of the insight to dismiss.
     */
    data class InsightsDismissed(val id: String) : DashboardIntent

    /**
     * Triggered when the user taps a quick-action button.
     * Emits a [DashboardEffect.NavigateToAddTransaction] effect.
     *
     * @property type The type of transaction to create.
     */
    data class QuickActionClicked(val type: TransactionType) : DashboardIntent
}
