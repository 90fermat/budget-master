package com.budgetmaster.dashboard.domain.usecase

import com.budgetmaster.dashboard.domain.model.ChartPoint
import com.budgetmaster.dashboard.domain.model.Period
import com.budgetmaster.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to retrieve and observe financial chart data points for a specified period.
 *
 * @property repository The repository that provides access to dashboard data.
 */
class GetChartDataUseCase(private val repository: DashboardRepository) {
    /**
     * Executes the use case to observe the chart data points for the given [period].
     *
     * @param period The time range for which chart data points are retrieved.
     * @return A Flow emitting a list of [ChartPoint]s.
     */
    operator fun invoke(period: Period): Flow<List<ChartPoint>> {
        return repository.getChartData(period)
    }
}
