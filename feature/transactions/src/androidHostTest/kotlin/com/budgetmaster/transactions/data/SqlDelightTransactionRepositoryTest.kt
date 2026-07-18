package com.budgetmaster.transactions.data

import com.budgetmaster.core.db.AppDataSeeder
import com.budgetmaster.core.db.DefaultData
import com.budgetmaster.core.session.ActiveAccountStore
import com.budgetmaster.core.session.SessionStore
import com.budgetmaster.transactions.InMemoryKeyValueStore
import com.budgetmaster.transactions.TestDatabaseHelper
import com.budgetmaster.transactions.domain.model.TransactionDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SqlDelightTransactionRepositoryTest {

    private fun repository(): SqlDelightTransactionRepository {
        val provider = TestDatabaseHelper.createProvider()
        return SqlDelightTransactionRepository(
            provider,
            AppDataSeeder(provider),
            SessionStore(),
            ActiveAccountStore(InMemoryKeyValueStore()),
        )
    }

    @Test
    fun seedsDefaultCategoriesOnFirstRead() = runTest {
        val repo = repository()
        val categories = repo.observeCategories().first()
        assertEquals(DefaultData.categories.size, categories.size)
        assertTrue(categories.any { it.name == "Food & Dining" })
    }

    @Test
    fun upsertStoresExpenseAsNegativeAndIncomeAsPositive() = runTest {
        val repo = repository()

        val expense = repo.upsertTransaction(
            TransactionDraft(
                amountAbs = 12.50,
                isExpense = true,
                description = "Coffee",
                categoryId = "cat_food",
                timestamp = 1_000L,
            )
        )
        val income = repo.upsertTransaction(
            TransactionDraft(
                amountAbs = 2000.0,
                isExpense = false,
                description = "Salary",
                categoryId = "cat_salary",
                timestamp = 2_000L,
            )
        )

        assertEquals(-12.50, expense.amount)
        assertEquals(2000.0, income.amount)
        assertNotNull(expense.category)
        assertEquals("Food & Dining", expense.category.name)

        val all = repo.observeTransactions().first()
        assertEquals(2, all.size)
        // Newest first (timestamp desc)
        assertEquals("Salary", all.first().description)
    }

    @Test
    fun editUpdatesInPlaceWithoutCreatingDuplicate() = runTest {
        val repo = repository()
        val created = repo.upsertTransaction(
            TransactionDraft(
                amountAbs = 5.0, isExpense = true, description = "Snack",
                categoryId = "cat_food", timestamp = 1_000L,
            )
        )

        repo.upsertTransaction(
            TransactionDraft(
                id = created.id, amountAbs = 7.0, isExpense = true, description = "Snack (fixed)",
                categoryId = "cat_food", timestamp = 1_000L,
            )
        )

        val all = repo.observeTransactions().first()
        assertEquals(1, all.size)
        assertEquals("Snack (fixed)", all.first().description)
        assertEquals(-7.0, all.first().amount)
    }

    @Test
    fun deleteThenRestoreRoundTrips() = runTest {
        val repo = repository()
        val item = repo.upsertTransaction(
            TransactionDraft(
                amountAbs = 9.0, isExpense = true, description = "Lunch",
                categoryId = "cat_food", timestamp = 1_000L,
            )
        )

        repo.deleteTransaction(item.id)
        assertTrue(repo.observeTransactions().first().isEmpty())

        repo.restoreTransaction(item)
        val restored = repo.observeTransactions().first()
        assertEquals(1, restored.size)
        assertEquals(item.id, restored.first().id)
    }
}
