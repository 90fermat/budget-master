package com.budgetmaster.transactions.domain.usecase

import com.budgetmaster.transactions.domain.model.TransactionCategory
import com.budgetmaster.transactions.domain.model.TransactionDraft
import com.budgetmaster.transactions.domain.model.TransactionItem
import com.budgetmaster.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

/** Observes the available categories for the picker and filter chips. */
class ObserveCategoriesUseCase(private val repository: TransactionRepository) {
    operator fun invoke(): Flow<List<TransactionCategory>> = repository.observeCategories()
}

/** Validates and saves a new or edited transaction. */
class SaveTransactionUseCase(private val repository: TransactionRepository) {
    /**
     * @throws IllegalArgumentException if the amount is not positive or the description is blank.
     */
    suspend operator fun invoke(draft: TransactionDraft): TransactionItem {
        require(draft.amountAbs > 0.0) { "Amount must be greater than zero." }
        require(draft.description.isNotBlank()) { "Description must not be blank." }
        return repository.upsertTransaction(draft)
    }
}

/** Deletes a transaction by id. */
class DeleteTransactionUseCase(private val repository: TransactionRepository) {
    suspend operator fun invoke(id: String) = repository.deleteTransaction(id)
}

/** Restores a previously deleted transaction (undo). */
class RestoreTransactionUseCase(private val repository: TransactionRepository) {
    suspend operator fun invoke(item: TransactionItem) = repository.restoreTransaction(item)
}
