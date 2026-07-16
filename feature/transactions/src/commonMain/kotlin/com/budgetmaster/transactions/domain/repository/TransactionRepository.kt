package com.budgetmaster.transactions.domain.repository

import com.budgetmaster.transactions.domain.model.TransactionAccount
import com.budgetmaster.transactions.domain.model.TransactionCategory
import com.budgetmaster.transactions.domain.model.TransactionDraft
import com.budgetmaster.transactions.domain.model.TransactionFilter
import com.budgetmaster.transactions.domain.model.TransactionItem
import kotlinx.coroutines.flow.Flow

/**
 * Data source for transactions and their categories.
 *
 * Implementations own first-launch seeding of a default account and default
 * categories so that inserts always satisfy the schema's foreign keys.
 */
interface TransactionRepository {
    /**
     * Observes transactions (newest first) with their category resolved.
     *
     * @param limit maximum rows to load; pass [TransactionFilter.NO_LIMIT] for all of them.
     */
    fun observeTransactions(limit: Long = TransactionFilter.NO_LIMIT): Flow<List<TransactionItem>>

    /** Observes the available categories (default + user-defined). */
    fun observeCategories(): Flow<List<TransactionCategory>>

    /** Observes the current user's active (non-archived) wallets, for the account picker. */
    fun observeAccounts(): Flow<List<TransactionAccount>>

    /** Inserts or updates a transaction from [draft]; returns the persisted item. */
    suspend fun upsertTransaction(draft: TransactionDraft): TransactionItem

    /** Deletes the transaction with [id]. */
    suspend fun deleteTransaction(id: String)

    /** Re-inserts a previously deleted [item] (used to undo a swipe delete). */
    suspend fun restoreTransaction(item: TransactionItem)
}
