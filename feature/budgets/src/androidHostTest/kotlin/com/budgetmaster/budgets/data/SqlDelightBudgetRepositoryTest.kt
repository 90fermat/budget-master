@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.budgets.data

import com.budgetmaster.budgets.TestDatabaseHelper
import com.budgetmaster.budgets.data.repository.SqlDelightBudgetRepository
import com.budgetmaster.budgets.domain.model.BudgetDraft
import com.budgetmaster.budgets.domain.model.BudgetStatus
import com.budgetmaster.core.db.AppDataSeeder
import com.budgetmaster.core.db.DatabaseProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlDelightBudgetRepositoryTest {

    private suspend fun setup(): Pair<SqlDelightBudgetRepository, DatabaseProvider> {
        val provider = TestDatabaseHelper.createProvider()
        val repo = SqlDelightBudgetRepository(provider, AppDataSeeder(provider))
        // Trigger seeding of the default user/account/categories.
        repo.observeCategories().first()
        return repo to provider
    }

    @Test
    fun spentIsComputedLiveFromExpenseTransactionsInPeriod() = runTest {
        val (repo, provider) = setup()
        val now = Clock.System.now().toEpochMilliseconds()
        val start = now - 1_000
        val end = now + 10_000_000

        repo.upsertBudget(BudgetDraft(categoryId = "cat_food", limit = 500.0, periodStart = start, periodEnd = end))

        val queries = provider.getDatabase().budgetMasterDatabaseQueries
        // Two food expenses in period + one income + one other-category expense.
        queries.insertTransaction("t1", "default_account", "cat_food", -120.0, "Dinner", now, null, null, 0)
        queries.insertTransaction("t2", "default_account", "cat_food", -30.0, "Snack", now, null, null, 0)
        queries.insertTransaction("t3", "default_account", "cat_food", 2000.0, "Refund", now, null, null, 0)
        queries.insertTransaction("t4", "default_account", "cat_travel", -50.0, "Taxi", now, null, null, 0)

        val budgets = repo.observeBudgets().first()
        assertEquals(1, budgets.size)
        val food = budgets.first()
        assertEquals(150.0, food.spent)
        assertEquals(500.0, food.limit)
        assertEquals(BudgetStatus.OK, food.status)
        assertEquals(350.0, food.remaining)
    }

    @Test
    fun statusIsExceededWhenSpentPassesLimit() = runTest {
        val (repo, provider) = setup()
        val now = Clock.System.now().toEpochMilliseconds()
        repo.upsertBudget(BudgetDraft(categoryId = "cat_food", limit = 100.0, periodStart = now - 1_000, periodEnd = now + 10_000_000))

        val queries = provider.getDatabase().budgetMasterDatabaseQueries
        queries.insertTransaction("t1", "default_account", "cat_food", -130.0, "Big dinner", now, null, null, 0)

        val food = repo.observeBudgets().first().first()
        assertEquals(130.0, food.spent)
        assertEquals(BudgetStatus.EXCEEDED, food.status)
    }

    @Test
    fun deleteRemovesBudget() = runTest {
        val (repo, _) = setup()
        val now = Clock.System.now().toEpochMilliseconds()
        repo.upsertBudget(BudgetDraft(id = "b1", categoryId = "cat_food", limit = 100.0, periodStart = now - 1_000, periodEnd = now + 10_000_000))
        assertEquals(1, repo.observeBudgets().first().size)

        repo.deleteBudget("b1")
        assertEquals(0, repo.observeBudgets().first().size)
    }
}
