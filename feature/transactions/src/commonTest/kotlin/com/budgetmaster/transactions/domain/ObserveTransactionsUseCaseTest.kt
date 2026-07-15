package com.budgetmaster.transactions.domain

import com.budgetmaster.transactions.domain.model.TransactionCategory
import com.budgetmaster.transactions.domain.model.TransactionDraft
import com.budgetmaster.transactions.domain.model.TransactionFilter
import com.budgetmaster.transactions.domain.model.TransactionItem
import com.budgetmaster.transactions.domain.model.TypeFilter
import com.budgetmaster.transactions.domain.repository.TransactionRepository
import com.budgetmaster.transactions.domain.usecase.ObserveTransactionsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveTransactionsUseCaseTest {

    private val food = TransactionCategory("cat_food", "Food & Dining", "🍔", "#F59E0B")
    private val salary = TransactionCategory("cat_salary", "Salary", "💰", "#059669")

    private val items = listOf(
        TransactionItem("1", -12.5, "Starbucks Coffee", 3_000, food, null),
        TransactionItem("2", 2000.0, "June Salary", 2_000, salary, "monthly"),
        TransactionItem("3", -45.0, "Gas station", 1_000, null, null),
    )

    private val repository = object : TransactionRepository {
        override fun observeTransactions(): Flow<List<TransactionItem>> = flowOf(items)
        override fun observeCategories() = flowOf(listOf(food, salary))
        override suspend fun upsertTransaction(draft: TransactionDraft) = error("unused")
        override suspend fun deleteTransaction(id: String) = Unit
        override suspend fun restoreTransaction(item: TransactionItem) = Unit
    }

    private val useCase = ObserveTransactionsUseCase(repository)

    @Test
    fun noFilterReturnsAll() = runTest {
        assertEquals(3, useCase(TransactionFilter()).first().size)
    }

    @Test
    fun incomeFilterKeepsOnlyPositive() = runTest {
        val result = useCase(TransactionFilter(type = TypeFilter.INCOME)).first()
        assertEquals(listOf("2"), result.map { it.id })
    }

    @Test
    fun expenseFilterKeepsOnlyNegative() = runTest {
        val result = useCase(TransactionFilter(type = TypeFilter.EXPENSE)).first()
        assertEquals(listOf("1", "3"), result.map { it.id })
    }

    @Test
    fun categoryFilterMatchesCategoryId() = runTest {
        val result = useCase(TransactionFilter(categoryId = "cat_food")).first()
        assertEquals(listOf("1"), result.map { it.id })
    }

    @Test
    fun queryMatchesDescriptionNotesAndCategoryCaseInsensitively() = runTest {
        assertEquals(listOf("1"), useCase(TransactionFilter(query = "starbucks")).first().map { it.id })
        assertEquals(listOf("2"), useCase(TransactionFilter(query = "MONTHLY")).first().map { it.id })
        assertEquals(listOf("1"), useCase(TransactionFilter(query = "dining")).first().map { it.id })
    }
}
