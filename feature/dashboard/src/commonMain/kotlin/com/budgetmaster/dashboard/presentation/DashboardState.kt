package com.budgetmaster.dashboard.presentation

import com.budgetmaster.core.model.Transaction
import com.budgetmaster.dashboard.domain.model.BalanceSummary
import com.budgetmaster.dashboard.domain.model.BudgetProgress
import com.budgetmaster.dashboard.domain.model.ChartPoint
import com.budgetmaster.dashboard.domain.model.Period

/**
 * Immutable UI state snapshot for the Dashboard screen.
 *
 * All fields have safe defaults so the ViewModel can emit an initial state immediately.
 *
 * @property isLoading `true` while the initial data load is in progress.
 * @property isRefreshing `true` during a pull-to-refresh or manual re-fetch operation.
 * @property balance The current balance summary, or `null` before the first successful load.
 * @property chartData The list of cash-flow data points for the active [selectedPeriod].
 * @property budgets The list of category budget progress items.
 * @property topTransactions The most recent transactions to display on the dashboard.
 * @property insights The async state of AI-generated insights (Loading / Success / Error).
 * @property selectedPeriod The currently active time range filter.
 * @property error A human-readable error message from the last failed operation, or `null`.
 */
data class DashboardState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val balance: BalanceSummary? = null,
    val chartData: List<ChartPoint> = emptyList(),
    val budgets: List<BudgetProgress> = emptyList(),
    val topTransactions: List<Transaction> = emptyList(),
    val insights: InsightsState = InsightsState.Loading,
    val selectedPeriod: Period = Period.MONTH,
    val error: String? = null,
    /** ISO currency from the user's settings, used to format every amount on the screen. */
    val currencyCode: String = "USD"
)
