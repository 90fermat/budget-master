@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.dashboard.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.budgetmaster.core.db.BudgetMasterDatabase
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.model.Transaction
import com.budgetmaster.dashboard.data.repository.SqlDelightDashboardRepository
import com.budgetmaster.dashboard.data.service.GeminiInsightsService
import com.budgetmaster.dashboard.domain.model.BalanceSummary
import com.budgetmaster.dashboard.domain.model.BalanceTrend
import com.budgetmaster.dashboard.domain.model.Period
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlDelightDashboardRepositoryTest {

    private lateinit var database: BudgetMasterDatabase
    private lateinit var databaseProvider: DatabaseProvider
    private lateinit var mockInsightsService: GeminiInsightsService
    private lateinit var repository: SqlDelightDashboardRepository

    @BeforeTest
    fun setUp() {
        database = TestDatabaseHelper.createInMemoryDatabase()
        databaseProvider = DatabaseProvider(database)
        mockInsightsService = GeminiInsightsService(databaseProvider, apiKeyProvider = { "" })
        repository = SqlDelightDashboardRepository(databaseProvider, mockInsightsService)
    }

    @Test
    fun testGetBalanceSummaryCalculatesCorrectTotals() = runTest {
        val queries = database.budgetMasterDatabaseQueries
        val now = Clock.System.now().toEpochMilliseconds()
        val userId = "default_user"
        val accountId = "account_1"
        val categoryId = "cat_1"

        // Setup foreign keys to avoid SQLite constraint violations
        queries.insertUser(userId, "Test User", "test@test.com", "USD", now)
        queries.insertAccount(accountId, userId, "Checking", "CHECKING", 1500.0, "USD", now)
        queries.insertCategory(categoryId, userId, "Food", "🍔", "#FF0000", 1)

        // Insert transactions within the last 30 days
        queries.insertTransaction("tx_1", accountId, categoryId, 500.0, "Salary", now, null, null, 0)
        queries.insertTransaction("tx_2", accountId, categoryId, -200.0, "Groceries", now, null, null, 0)

        val balanceSummary = repository.getBalanceSummary(Period.MONTH).first()
        // Opening account balance (1500) + signed transaction sum (+500 - 200) = 1800
        assertEquals(1800.0, balanceSummary.totalBalance)
        assertEquals(500.0, balanceSummary.monthlyIncome)
        assertEquals(200.0, balanceSummary.monthlyExpenses)
        assertEquals(BalanceTrend.POSITIVE, balanceSummary.balanceTrend)
        assertEquals(150.0, balanceSummary.trendPercentage)
    }

    @Test
    fun testGetBudgetProgressAggregatesCorrectly() = runTest {
        val queries = database.budgetMasterDatabaseQueries
        val now = Clock.System.now().toEpochMilliseconds()
        val userId = "default_user"
        val categoryId = "cat_1"

        queries.insertUser(userId, "Test User", "test@test.com", "USD", now)
        queries.insertCategory(categoryId, userId, "Shopping", "🛍️", "#00FF00", 1)

        queries.insertBudget(
            id = "budget_1",
            userId = userId,
            categoryId = categoryId,
            amount = 100.0,
            spent = 85.0,
            startDate = now - 100000L,
            endDate = now + 100000L
        )

        val progressList = repository.getBudgetProgress().first()
        assertEquals(1, progressList.size)
        val progress = progressList.first()
        assertEquals(categoryId, progress.categoryId)
        assertEquals("Shopping", progress.categoryName)
        assertEquals("🛍️", progress.categoryEmoji)
        assertEquals(85.0, progress.spent)
        assertEquals(100.0, progress.limit)
        assertEquals(0.85, progress.percentage)
    }

    @Test
    fun testDeleteTransactionRemovesIt() = runTest {
        val queries = database.budgetMasterDatabaseQueries
        val now = Clock.System.now().toEpochMilliseconds()
        val userId = "default_user"
        val accountId = "account_1"
        val categoryId = "cat_1"

        queries.insertUser(userId, "Test User", "test@test.com", "USD", now)
        queries.insertAccount(accountId, userId, "Checking", "CHECKING", 1500.0, "USD", now)
        queries.insertCategory(categoryId, userId, "Food", "🍔", "#FF0000", 1)

        queries.insertTransaction("tx_to_delete", accountId, categoryId, -25.0, "Burger", now, null, null, 0)
        
        // Verify insertion
        var txs = queries.selectAllTransactions().executeAsList()
        assertEquals(1, txs.size)

        repository.deleteTransaction("tx_to_delete")

        txs = queries.selectAllTransactions().executeAsList()
        assertTrue(txs.isEmpty())
    }

    @Test
    fun testDismissInsightRemovesIt() = runTest {
        val queries = database.budgetMasterDatabaseQueries
        val now = Clock.System.now().toEpochMilliseconds()

        queries.insertInsight(
            id = "insight_to_dismiss",
            type = "TREND",
            message = "Trend message",
            actionRoute = null,
            timestamp = now
        )

        var insights = queries.selectAllInsights().executeAsList()
        assertEquals(1, insights.size)

        repository.dismissInsight("insight_to_dismiss")

        insights = queries.selectAllInsights().executeAsList()
        assertTrue(insights.isEmpty())
    }
}
