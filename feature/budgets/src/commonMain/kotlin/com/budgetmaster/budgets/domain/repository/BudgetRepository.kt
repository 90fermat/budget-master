package com.budgetmaster.budgets.domain.repository

import com.budgetmaster.core.model.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing budget and transaction data.
 */
interface BudgetRepository {
    /**
     * Retrieves all transactions as a Flow.
     */
    fun getTransactions(): Flow<List<Transaction>>

    /**
     * Inserts or updates a transaction.
     */
    suspend fun insertTransaction(transaction: Transaction)

    /**
     * Deletes a transaction by its unique ID.
     */
    suspend fun deleteTransaction(id: String)
}
