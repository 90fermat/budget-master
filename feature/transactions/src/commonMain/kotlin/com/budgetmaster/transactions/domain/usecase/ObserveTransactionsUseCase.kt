package com.budgetmaster.transactions.domain.usecase

import com.budgetmaster.transactions.domain.model.TransactionFilter
import com.budgetmaster.transactions.domain.model.TransactionItem
import com.budgetmaster.transactions.domain.model.TypeFilter
import com.budgetmaster.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Observes transactions with the given [TransactionFilter] applied (search, category, type).
 *
 * Filtering is in-memory, so the database window must contain every candidate row: while
 * browsing unfiltered the list is paged ([TransactionFilter.limit]), but as soon as a filter
 * is active the query is unbounded — otherwise a match older than the window would be
 * invisible, which would look like data loss.
 */
class ObserveTransactionsUseCase(private val repository: TransactionRepository) {
    operator fun invoke(filter: TransactionFilter): Flow<List<TransactionItem>> =
        repository.observeTransactions(limit = filter.effectiveLimit)
            .map { items -> items.filter { it.matches(filter) } }

    private fun TransactionItem.matches(filter: TransactionFilter): Boolean {
        if (filter.categoryId != null && category?.id != filter.categoryId) return false
        when (filter.type) {
            TypeFilter.INCOME -> if (isExpense) return false
            TypeFilter.EXPENSE -> if (!isExpense) return false
            TypeFilter.ALL -> Unit
        }
        if (filter.query.isNotBlank()) {
            val q = filter.query.trim()
            val inDescription = description.contains(q, ignoreCase = true)
            val inNotes = notes?.contains(q, ignoreCase = true) == true
            val inCategory = category?.name?.contains(q, ignoreCase = true) == true
            if (!inDescription && !inNotes && !inCategory) return false
        }
        return true
    }
}
