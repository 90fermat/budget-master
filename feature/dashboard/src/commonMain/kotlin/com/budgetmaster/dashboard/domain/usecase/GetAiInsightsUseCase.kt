package com.budgetmaster.dashboard.domain.usecase

import com.budgetmaster.dashboard.domain.model.Insight
import com.budgetmaster.dashboard.domain.repository.DashboardRepository

/**
 * Use case to retrieve AI-generated financial insights for the dashboard.
 *
 * @property repository The repository that provides access to dashboard data.
 */
class GetAiInsightsUseCase(private val repository: DashboardRepository) {
    /**
     * Executes the use case to fetch AI-generated insights.
     *
     * @param forceRefresh Whether to force a refresh from the remote server or AI service.
     * @return A [Result] containing a list of AI [Insight]s on success, or an error on failure.
     */
    suspend operator fun invoke(forceRefresh: Boolean): Result<List<Insight>> {
        return repository.getAiInsights(forceRefresh)
    }
}
