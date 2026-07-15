package com.budgetmaster.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmaster.core.model.Transaction
import com.budgetmaster.dashboard.domain.model.BalanceSummary
import com.budgetmaster.dashboard.domain.model.Period
import com.budgetmaster.dashboard.domain.repository.DashboardRepository
import com.budgetmaster.dashboard.domain.usecase.GetAiInsightsUseCase
import com.budgetmaster.dashboard.domain.usecase.GetBalanceSummaryUseCase
import com.budgetmaster.dashboard.domain.usecase.GetBudgetProgressUseCase
import com.budgetmaster.dashboard.domain.usecase.GetChartDataUseCase
import com.budgetmaster.dashboard.domain.usecase.GetTopTransactionsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The maximum number of recent transactions displayed on the Dashboard. */
private const val TOP_TRANSACTIONS_LIMIT = 5

/**
 * ViewModel for the Dashboard screen.
 *
 * Follows strict MVI: [DashboardIntent] → mutates [DashboardState] → optionally emits [DashboardEffect].
 *
 * The four reactive data streams (balance, chart, budgets, transactions) are merged into a
 * single [StateFlow] via `combine()`. AI insights are fetched separately and cached for 24 h
 * in SQLDelight. All errors are caught and surfaced through `state.error` to keep the UI resilient.
 *
 * @param repository The dashboard data source.
 * @param getBalanceSummary Use case for observing the balance summary.
 * @param getChartData Use case for observing chart data points.
 * @param getBudgetProgress Use case for observing budget progress.
 * @param getTopTransactions Use case for observing recent transactions.
 * @param getAiInsights Use case for fetching AI-generated insights.
 */
class DashboardViewModel(
    private val repository: DashboardRepository,
    private val getBalanceSummary: GetBalanceSummaryUseCase,
    private val getChartData: GetChartDataUseCase,
    private val getBudgetProgress: GetBudgetProgressUseCase,
    private val getTopTransactions: GetTopTransactionsUseCase,
    private val getAiInsights: GetAiInsightsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())

    /** Observable UI state. Collected by the Dashboard Composable. */
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<DashboardEffect>()

    /** Observable stream of one-shot UI side effects. */
    val effects: SharedFlow<DashboardEffect> = _effects.asSharedFlow()

    /** Holds the active combine() Job so it can be cancelled and re-subscribed on refresh. */
    private var dataJob: Job? = null

    init {
        onIntent(DashboardIntent.LoadDashboard(Period.MONTH))
    }

    /**
     * Entry point for all UI events. Dispatches each [DashboardIntent] to its handler.
     *
     * @param intent The action dispatched from the UI.
     */
    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.LoadDashboard -> loadDashboard(intent.period)
            is DashboardIntent.RefreshRequested -> refresh()
            is DashboardIntent.PeriodChanged -> changePeriod(intent.period)
            is DashboardIntent.TransactionSwiped -> swipeTransaction(intent.id)
            is DashboardIntent.InsightsDismissed -> dismissInsight(intent.id)
            is DashboardIntent.QuickActionClicked -> emitEffect(
                DashboardEffect.NavigateToAddTransaction(intent.type)
            )
        }
    }

    // ── Private handlers ──────────────────────────────────────────────────────

    /**
     * Subscribes to the four reactive data streams combined into a single state update.
     * Any upstream error is caught per-stream and surfaced in [DashboardState.error].
     *
     * @param period The time range for balance and chart data.
     */
    private fun loadDashboard(period: Period) {
        _state.update { it.copy(isLoading = true, selectedPeriod = period, error = null) }
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            combine(
                getBalanceSummary(period).map { it as BalanceSummary? }.catch { e ->
                    _state.update { it.copy(error = e.message) }
                    // Emit null fallback so combine() doesn't stall
                    emit(null)
                },
                getChartData(period).catch { e ->
                    _state.update { it.copy(error = e.message) }
                    emit(emptyList())
                },
                getBudgetProgress().catch { e ->
                    _state.update { it.copy(error = e.message) }
                    emit(emptyList())
                },
                getTopTransactions(TOP_TRANSACTIONS_LIMIT).catch { e ->
                    _state.update { it.copy(error = e.message) }
                    emit(emptyList())
                },
            ) { balance, chart, budgets, transactions ->
                _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    balance = balance,
                    chartData = chart,
                    budgets = budgets,
                    topTransactions = transactions,
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
        loadInsights(forceRefresh = false)
    }

    /**
     * Performs a manual refresh: sets [DashboardState.isRefreshing] to `true`, re-subscribes
     * to all four data streams, and forces an AI insights reload.
     */
    private fun refresh() {
        _state.update { it.copy(isRefreshing = true, error = null) }
        val currentPeriod = _state.value.selectedPeriod
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            combine(
                getBalanceSummary(currentPeriod).map { it as BalanceSummary? }.catch { e ->
                    _state.update { it.copy(error = e.message, isRefreshing = false) }
                    emit(null)
                },
                getChartData(currentPeriod).catch { e ->
                    _state.update { it.copy(error = e.message, isRefreshing = false) }
                    emit(emptyList())
                },
                getBudgetProgress().catch { e ->
                    _state.update { it.copy(error = e.message, isRefreshing = false) }
                    emit(emptyList())
                },
                getTopTransactions(TOP_TRANSACTIONS_LIMIT).catch { e ->
                    _state.update { it.copy(error = e.message, isRefreshing = false) }
                    emit(emptyList())
                },
            ) { balance, chart, budgets, transactions ->
                _state.value.copy(
                    isRefreshing = false,
                    balance = balance,
                    chartData = chart,
                    budgets = budgets,
                    topTransactions = transactions,
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
        loadInsights(forceRefresh = true)
    }

    /**
     * Updates [DashboardState.selectedPeriod] and re-fetches balance and chart data
     * without cancelling the budget or transactions subscriptions.
     *
     * @param period The newly selected time range.
     */
    private fun changePeriod(period: Period) {
        if (_state.value.selectedPeriod == period) return
        _state.update { it.copy(selectedPeriod = period, error = null) }
        // Re-subscribe everything for the new period
        loadDashboard(period)
    }

    /**
     * Deletes the transaction identified by [id] from the repository.
     * On success, emits [DashboardEffect.ShowUndoDelete] with the deleted transaction.
     * On failure, updates [DashboardState.error] and emits [DashboardEffect.ShowError].
     *
     * @param id The unique identifier of the transaction to delete.
     */
    private fun swipeTransaction(id: String) {
        val deleted: Transaction = _state.value.topTransactions
            .firstOrNull { it.id == id }
            ?: return

        // Optimistically remove from UI immediately
        _state.update { it.copy(topTransactions = it.topTransactions.filter { t -> t.id != id }) }

        viewModelScope.launch {
            try {
                repository.deleteTransaction(id)
                emitEffect(DashboardEffect.ShowUndoDelete(deleted))
            } catch (e: Exception) {
                // Rollback optimistic update
                _state.update {
                    it.copy(
                        topTransactions = (listOf(deleted) + it.topTransactions)
                            .sortedByDescending { t -> t.timestamp },
                        error = e.message
                    )
                }
                emitEffect(DashboardEffect.ShowError(e.message ?: "Failed to delete transaction."))
            }
        }
    }

    /**
     * Marks the given insight as dismissed in the repository and removes it from the local state.
     *
     * @param id The unique identifier of the insight to dismiss.
     */
    private fun dismissInsight(id: String) {
        val current = _state.value.insights
        if (current !is InsightsState.Success) return

        // Optimistically remove from UI
        _state.update {
            it.copy(insights = InsightsState.Success(current.data.filter { insight -> insight.id != id }))
        }

        viewModelScope.launch {
            try {
                repository.dismissInsight(id)
            } catch (e: Exception) {
                // Restore the dismissed insight on failure
                _state.update { it.copy(insights = current, error = e.message) }
                emitEffect(DashboardEffect.ShowError(e.message ?: "Failed to dismiss insight."))
            }
        }
    }

    /**
     * Fetches AI-generated insights and updates [DashboardState.insights].
     *
     * Insights are cached for 24 hours in SQLDelight; [forceRefresh] bypasses the cache.
     *
     * @param forceRefresh `true` to skip the cache and call the AI service directly.
     */
    private fun loadInsights(forceRefresh: Boolean) {
        _state.update { it.copy(insights = InsightsState.Loading) }
        viewModelScope.launch {
            getAiInsights(forceRefresh)
                .onSuccess { insights ->
                    _state.update { it.copy(insights = InsightsState.Success(insights)) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(insights = InsightsState.Error(e.message ?: "Could not load insights."))
                    }
                }
        }
    }

    /**
     * Emits a [DashboardEffect] on [viewModelScope] to guarantee delivery even if the
     * calling coroutine is cancelled.
     *
     * @param effect The effect to deliver to the UI.
     */
    private fun emitEffect(effect: DashboardEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }
}
