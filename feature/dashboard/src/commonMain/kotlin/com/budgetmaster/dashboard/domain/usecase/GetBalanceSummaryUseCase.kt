package com.budgetmaster.dashboard.domain.usecase

import com.budgetmaster.dashboard.domain.model.BalanceSummary
import com.budgetmaster.dashboard.domain.model.Period
import com.budgetmaster.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to retrieve and observe the balance summary (total balance, monthly income, monthly expenses, trend) for a specified period.
 *
 * @property repository The repository that provides access to dashboard data.
 */
class GetBalanceSummaryUseCase(private val repository: DashboardRepository) {
    /**
     * Executes the use case to observe the balance summary for the given [period].
     *
     * @param period The time range to filter the balance summary.
     * @return A Flow emitting the latest [BalanceSummary].
     */
    operator fun invoke(period: Period): Flow<BalanceSummary> {
        return repository.getBalanceSummary(period)
    }
}
