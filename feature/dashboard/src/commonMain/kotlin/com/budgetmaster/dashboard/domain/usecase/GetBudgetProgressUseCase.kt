package com.budgetmaster.dashboard.domain.usecase

import com.budgetmaster.dashboard.domain.model.BudgetProgress
import com.budgetmaster.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to retrieve and observe the user's active budget limits and their spending progress.
 *
 * @property repository The repository that provides access to dashboard data.
 */
class GetBudgetProgressUseCase(private val repository: DashboardRepository) {
    /**
     * Executes the use case to observe the list of active budget goals and progress.
     *
     * @return A Flow emitting a list of [BudgetProgress] instances.
     */
    operator fun invoke(): Flow<List<BudgetProgress>> {
        return repository.getBudgetProgress()
    }
}
