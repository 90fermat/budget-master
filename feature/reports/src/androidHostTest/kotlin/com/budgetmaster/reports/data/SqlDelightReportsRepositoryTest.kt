@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.reports.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.budgetmaster.core.db.BudgetMasterDatabase
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.prefs.AppSettingsRepository
import com.budgetmaster.core.prefs.KeyValueStore
import com.budgetmaster.core.session.ActiveAccountStore
import com.budgetmaster.core.session.SessionStore
import com.budgetmaster.reports.domain.model.ReportRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeStore : KeyValueStore {
    private val entries = MutableStateFlow<Map<String, String>>(emptyMap())
    override fun observeString(key: String): Flow<String?> = entries.map { it[key] }
    override suspend fun putString(key: String, value: String) {
        entries.value = entries.value + (key to value)
    }
    override suspend fun remove(key: String) {
        entries.value = entries.value - key
    }
}

class SqlDelightReportsRepositoryTest {

    private val userId = "default_user"
    private val accountId = "acc_1"
    private val now = Clock.System.now().toEpochMilliseconds()

    private suspend fun setup(): Pair<SqlDelightReportsRepository, BudgetMasterDatabase> {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BudgetMasterDatabase.Schema.synchronous().create(driver)
        val database = BudgetMasterDatabase(driver)
        val provider = DatabaseProvider(database)
        val repo = SqlDelightReportsRepository(
            provider,
            SessionStore(),
            ActiveAccountStore(FakeStore()),
            AppSettingsRepository(FakeStore()),
        )

        val queries = database.budgetMasterDatabaseQueries
        queries.insertUser(userId, "You", "you@test.com", "USD", now)
        queries.insertAccount(accountId, userId, "Cash", "CASH", 0.0, "USD", now, 0)
        queries.insertCategory("cat_food", userId, "Food", "🍔", "#F59E0B", 1)
        queries.insertCategory("cat_salary", userId, "Salary", "💰", "#059669", 1)
        return repo to database
    }

    @Test
    fun totalsSplitIncomeAndExpensesAndExcludeTransfers() = runTest {
        val (repo, database) = setup()
        val queries = database.budgetMasterDatabaseQueries
        queries.insertTransaction("t1", accountId, "cat_salary", 2000.0, "Pay", now, null, null, 0, null)
        queries.insertTransaction("t2", accountId, "cat_food", -150.0, "Dinner", now, null, null, 0, null)
        // A transfer leg must not inflate either side of the report.
        queries.insertTransaction("t3", accountId, null, -500.0, "Transfer", now, null, null, 0, "grp1")

        val report = repo.observeReport(ReportRange.MONTH).first()
        assertEquals(2000.0, report.totalIncome)
        assertEquals(150.0, report.totalExpenses)
        assertEquals(1850.0, report.net)
    }

    @Test
    fun categoryBreakdownRanksSpendingAndComputesShares() = runTest {
        val (repo, database) = setup()
        val queries = database.budgetMasterDatabaseQueries
        queries.insertTransaction("t1", accountId, "cat_food", -75.0, "Lunch", now, null, null, 0, null)
        queries.insertTransaction("t2", accountId, "cat_food", -25.0, "Snack", now, null, null, 0, null)
        queries.insertTransaction("t3", accountId, null, -100.0, "Misc", now, null, null, 0, null)
        // Income must not appear as a spending slice.
        queries.insertTransaction("t4", accountId, "cat_salary", 900.0, "Pay", now, null, null, 0, null)

        val report = repo.observeReport(ReportRange.MONTH).first()
        assertEquals(2, report.categories.size)
        assertEquals(100.0, report.categories[0].amount)
        assertEquals(0.5f, report.categories[0].share)
        assertTrue(report.categories.none { it.categoryId == "cat_salary" })
    }

    @Test
    fun previousPeriodDrivesTheComparison() = runTest {
        val (repo, database) = setup()
        val queries = database.budgetMasterDatabaseQueries
        val day = 24L * 60 * 60 * 1000
        queries.insertTransaction("cur", accountId, "cat_food", -150.0, "Now", now - day, null, null, 0, null)
        // 45 days back: inside the preceding 30-day window, outside the current one.
        queries.insertTransaction("prev", accountId, "cat_food", -100.0, "Before", now - 45 * day, null, null, 0, null)

        val report = repo.observeReport(ReportRange.MONTH).first()
        assertEquals(150.0, report.totalExpenses)
        assertEquals(100.0, report.previousExpenses)
        assertEquals(0.5f, report.expenseChange)
    }

    @Test
    fun csvQuotesSeparatorsAndIncludesTransfersFlagged() = runTest {
        val (repo, database) = setup()
        val queries = database.budgetMasterDatabaseQueries
        // A description containing a comma and a quote must survive a round trip.
        queries.insertTransaction(
            "t1", accountId, "cat_food", -12.5, """Cafe "Bo", downtown""", now, null, null, 0, null,
        )
        queries.insertTransaction("t2", accountId, null, -500.0, "Transfer", now, null, null, 0, "grp1")

        val csv = repo.exportCsv(ReportRange.MONTH)
        assertTrue(csv.startsWith("Date,Description,Category,Account,Amount,Transfer,Notes"))
        assertTrue(csv.contains(""""Cafe ""Bo"", downtown""""))
        // The export is a record of the money, so transfers are present but marked.
        assertTrue(csv.contains(",yes,"))
        assertTrue(csv.contains(",no,"))
    }
}
