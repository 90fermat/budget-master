package com.budgetmaster.dashboard.domain.repository

import com.budgetmaster.dashboard.domain.model.DeletedTransaction
import com.budgetmaster.core.model.Transaction
import com.budgetmaster.dashboard.domain.model.BalanceSummary
import com.budgetmaster.dashboard.domain.model.BudgetProgress
import com.budgetmaster.dashboard.domain.model.ChartPoint
import com.budgetmaster.dashboard.domain.model.Insight
import com.budgetmaster.dashboard.domain.model.Period
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for retrieving dashboard data and insights.
 */
interface DashboardRepository {
    /**
     * Observes the balance summary (total balance, monthly income/expenses, trend) for a given [Period].
     *
     * @param period The time range to filter the balance summary.
     * @return A Flow emitting the latest [BalanceSummary].
     */
    fun getBalanceSummary(period: Period): Flow<BalanceSummary>

    /**
     * Observes the data points for generating the revenue, expenses, and balance charts over a given [Period].
     *
     * @param period The time range for which chart data points are retrieved.
     * @return A Flow emitting a list of [ChartPoint]s.
     */
    fun getChartData(period: Period): Flow<List<ChartPoint>>

    /**
     * Observes the list of active budget goals and their current spending progress.
     *
     * @return A Flow emitting a list of [BudgetProgress] instances.
     */
    fun getBudgetProgress(): Flow<List<BudgetProgress>>

    /**
     * Observes the most recent transactions up to the specified [limit].
     *
     * @param limit The maximum number of transactions to retrieve.
     * @return A Flow emitting a list of [Transaction]s.
     */
    fun getTopTransactions(limit: Int): Flow<List<Transaction>>

    /**
     * Retrieves AI-generated insights for the user's financial status.
     *
     * @param forceRefresh Whether to force a refresh from the remote server/AI service.
     * @return A [Result] containing the list of AI-generated [Insight]s on success, or an error on failure.
     */
    /**
     * Whether an AI provider is configured at all. When false the UI hides the insights surface
     * rather than showing a section that can never fill.
     */
    val isAiConfigured: Boolean

    /**
     * @param languageTag BCP-47 tag for the app's language, so insights come back in the language
     *   the user chose rather than one baked into the prompt.
     */
    suspend fun getAiInsights(forceRefresh: Boolean, languageTag: String): Result<List<Insight>>

    /**
     * Permanently deletes a transaction by its unique [id].
     *
     * @param id The identifier of the transaction to remove.
     */
    /**
     * Deletes a transaction and returns a snapshot of what was removed.
     *
     * @return the deleted row, or null if it no longer existed. Pass it to [restoreTransaction]
     *   to undo. Returning the snapshot rather than nothing is what makes an honest undo
     *   possible - the display model does not carry enough of the row to rebuild it.
     */
    suspend fun deleteTransaction(id: String): DeletedTransaction?

    /** Re-inserts a row removed by [deleteTransaction], exactly as it was. */
    suspend fun restoreTransaction(snapshot: DeletedTransaction)

    /**
     * Marks an AI insight as dismissed so it is excluded from future results.
     *
     * @param id The identifier of the insight to dismiss.
     */
    suspend fun dismissInsight(id: String)
}
