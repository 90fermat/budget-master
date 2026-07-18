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

    /**
     * The income breakdown was the missing half of Reports: the donut only ever covered
     * `amount < 0`, so "where does my money come from" had no answer anywhere in the app.
     */
    @Test
    fun incomeBreakdownRanksSourcesAndExcludesSpending() = runTest {
        val (repo, database) = setup()
        val queries = database.budgetMasterDatabaseQueries
        queries.insertTransaction("i1", accountId, "cat_salary", 1500.0, "Pay", now, null, null, 0, null)
        queries.insertTransaction("i2", accountId, null, 500.0, "Refund", now, null, null, 0, null)
        queries.insertTransaction("e1", accountId, "cat_food", -200.0, "Dinner", now, null, null, 0, null)
        // A transfer leg is not income, however positive it looks.
        queries.insertTransaction("t1", accountId, null, 900.0, "Transfer in", now, null, null, 0, "grp1")

        val report = repo.observeReport(ReportRange.MONTH).first()

        assertEquals(2, report.incomeCategories.size)
        assertEquals(1500.0, report.incomeCategories[0].amount, "largest source first")
        assertEquals("cat_salary", report.incomeCategories[0].categoryId)
        // Shares are a fraction of income, not of everything that moved.
        assertEquals(0.75f, report.incomeCategories[0].share)
        assertTrue(report.incomeCategories.none { it.categoryId == "cat_food" }, "spending is not income")
    }

    @Test
    fun theTwoBreakdownsAreIndependent() = runTest {
        val (repo, database) = setup()
        val queries = database.budgetMasterDatabaseQueries
        // A period can genuinely have spending and no income at all.
        queries.insertTransaction("e1", accountId, "cat_food", -80.0, "Lunch", now, null, null, 0, null)

        val report = repo.observeReport(ReportRange.MONTH).first()

        assertEquals(1, report.categories.size)
        assertTrue(report.incomeCategories.isEmpty(), "no income means an empty breakdown, not a crash")
    }

    @Test
    fun topPayeesAndPayersRankCounterpartiesSeparately() = runTest {
        val (repo, database) = setup()
        val queries = database.budgetMasterDatabaseQueries
        queries.insertTransaction("o1", accountId, null, -5000.0, "MIKAM", now, null, null, 0, null)
        queries.insertTransaction("o2", accountId, null, -3000.0, "MIKAM", now, null, null, 0, null)
        queries.insertTransaction("o3", accountId, null, -1000.0, "CANAL+", now, null, null, 0, null)
        queries.insertTransaction("i1", accountId, null, 20000.0, "TCHOUKEN", now, null, null, 0, null)

        val report = repo.observeReport(ReportRange.MONTH).first()

        // Repeat payments to one counterparty aggregate rather than listing twice.
        assertEquals("MIKAM", report.topPayees[0].name)
        assertEquals(8000.0, report.topPayees[0].amount)
        assertEquals(2, report.topPayees[0].transactionCount)
        assertEquals("CANAL+", report.topPayees[1].name)

        // Money in is a payer, never a payee.
        assertEquals(1, report.topPayers.size)
        assertEquals("TCHOUKEN", report.topPayers[0].name)
        assertTrue(report.topPayees.none { it.name == "TCHOUKEN" })
    }

    /** Fees are a charge, not a counterparty - listing "Frais - MIKAM" as a payee would be noise. */
    @Test
    fun feesAreTotalledAndExcludedFromCounterparties() = runTest {
        val (repo, database) = setup()
        val queries = database.budgetMasterDatabaseQueries
        queries.insertCategory("cat_fees", userId, "Fees", "F", "#94A3B8", 1)
        queries.insertTransaction("p1", accountId, null, -20244.0, "MIKAM", now, null, null, 0, null)
        queries.insertTransaction("f1", accountId, "cat_fees", -44.48, "Frais - MIKAM", now, null, null, 0, null)
        queries.insertTransaction("f2", accountId, "cat_fees", -20.0, "Frais - CANAL+", now, null, null, 0, null)

        val report = repo.observeReport(ReportRange.MONTH).first()

        // Tolerance because these are summed doubles (44.48 + 20.0 lands at 64.47999...), which
        // is how every amount in the app is stored; display rounds via MoneyFormatter.
        assertEquals(64.48, report.totalFees, absoluteTolerance = 0.001)
        assertEquals(1, report.topPayees.size, "fee rows must not appear as counterparties")
        assertEquals("MIKAM", report.topPayees[0].name)
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
