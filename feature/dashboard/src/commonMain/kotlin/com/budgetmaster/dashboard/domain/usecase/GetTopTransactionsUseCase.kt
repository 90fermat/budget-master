package com.budgetmaster.dashboard.domain.usecase

import com.budgetmaster.core.model.Transaction
import com.budgetmaster.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to retrieve and observe recent transactions up to a specified limit.
 *
 * @property repository The repository that provides access to dashboard data.
 */
class GetTopTransactionsUseCase(private val repository: DashboardRepository) {
    /**
     * Executes the use case to observe the top transactions.
     *
     * @param limit The maximum number of transactions to retrieve.
     * @return A Flow emitting a list of [Transaction]s.
     */
    operator fun invoke(limit: Int): Flow<List<Transaction>> {
        return repository.getTopTransactions(limit)
    }
}
