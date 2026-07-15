@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.dashboard.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.model.Transaction
import com.budgetmaster.dashboard.domain.model.BalanceSummary
import com.budgetmaster.dashboard.domain.model.BalanceTrend
import com.budgetmaster.dashboard.domain.model.BudgetProgress
import com.budgetmaster.dashboard.domain.model.BudgetStatus
import com.budgetmaster.dashboard.domain.model.ChartPoint
import com.budgetmaster.dashboard.domain.model.Insight
import com.budgetmaster.dashboard.domain.model.Period
import com.budgetmaster.dashboard.domain.repository.DashboardRepository
import com.budgetmaster.dashboard.data.service.GeminiInsightsService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Implementation of [DashboardRepository] that queries local tables using SQLDelight
 * and delegates AI-generated insights to [GeminiInsightsService].
 */
class SqlDelightDashboardRepository(
    private val databaseProvider: DatabaseProvider,
    private val geminiInsightsService: GeminiInsightsService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : DashboardRepository {

    private val defaultUserId = "default_user"

    override fun getBalanceSummary(period: Period): Flow<BalanceSummary> = flow {
        val db = databaseProvider.getDatabase()
        val queries = db.budgetMasterDatabaseQueries

        emitAll(
            queries.selectAllTransactions()
                .asFlow()
                .mapToList(dispatcher)
                .map { transactions ->
                    val nowMs = Clock.System.now().toEpochMilliseconds()
                    val thirtyDaysAgo = nowMs - 30L * 24 * 60 * 60 * 1000L

                    val monthlyIncome = transactions
                        .filter { it.timestamp >= thirtyDaysAgo && it.amount > 0 }
                        .sumOf { it.amount }

                    val monthlyExpenses = transactions
                        .filter { it.timestamp >= thirtyDaysAgo && it.amount < 0 }
                        .sumOf { kotlin.math.abs(it.amount) }

                    val accounts = queries.selectAccountsByUserId(defaultUserId).awaitAsList()
                    val totalBalance = if (accounts.isNotEmpty()) {
                        accounts.sumOf { it.balance }
                    } else {
                        transactions.sumOf { it.amount }
                    }

                    val trend = if (monthlyIncome >= monthlyExpenses) BalanceTrend.POSITIVE else BalanceTrend.NEGATIVE
                    val trendPercentage = if (monthlyExpenses > 0.0) {
                        ((monthlyIncome - monthlyExpenses) / monthlyExpenses * 100.0)
                    } else {
                        0.0
                    }

                    BalanceSummary(
                        totalBalance = totalBalance,
                        monthlyIncome = monthlyIncome,
                        monthlyExpenses = monthlyExpenses,
                        balanceTrend = trend,
                        trendPercentage = trendPercentage
                    )
                }
        )
    }

    override fun getChartData(period: Period): Flow<List<ChartPoint>> = flow {
        val db = databaseProvider.getDatabase()
        val queries = db.budgetMasterDatabaseQueries

        emitAll(
            queries.selectAllTransactions()
                .asFlow()
                .mapToList(dispatcher)
                .map { transactions ->
                    val nowMs = Clock.System.now().toEpochMilliseconds()
                    val limitMs = when (period) {
                        Period.WEEK -> nowMs - 7L * 24 * 60 * 60 * 1000L
                        Period.MONTH -> nowMs - 30L * 24 * 60 * 60 * 1000L
                        Period.YEAR -> nowMs - 365L * 24 * 60 * 60 * 1000L
                        Period.ALL -> 0L
                    }

                    val filteredTx = transactions.filter { it.timestamp >= limitMs }

                    val timeZone = TimeZone.currentSystemDefault()
                    val grouped = filteredTx.groupBy {
                        Instant.fromEpochMilliseconds(it.timestamp).toLocalDateTime(timeZone).date
                    }

                    val sortedDates = grouped.keys.sorted()
                    var runningBalance = 0.0
                    sortedDates.map { date ->
                        val dateTx = grouped[date] ?: emptyList()
                        val dateIncome = dateTx.filter { it.amount > 0 }.sumOf { it.amount }
                        val dateExpenses = dateTx.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }
                        runningBalance += dateTx.sumOf { it.amount }
                        ChartPoint(
                            date = date,
                            balance = runningBalance,
                            income = dateIncome,
                            expenses = dateExpenses
                        )
                    }
                }
        )
    }

    override fun getBudgetProgress(): Flow<List<BudgetProgress>> = flow {
        val db = databaseProvider.getDatabase()
        val queries = db.budgetMasterDatabaseQueries
        val nowMs = Clock.System.now().toEpochMilliseconds()

        emitAll(
            queries.selectBudgetsByUserId(defaultUserId, nowMs, nowMs)
                .asFlow()
                .mapToList(dispatcher)
                .map { budgets ->
                    val categories = queries.selectCategoriesByUserId(defaultUserId).awaitAsList().associateBy { it.id }
                    budgets.map { budget ->
                        val category = categories[budget.categoryId]
                        val percentage = if (budget.amount > 0.0) budget.spent / budget.amount else 0.0
                        val status = when {
                            percentage > 1.0 -> BudgetStatus.EXCEEDED
                            percentage >= 0.8 -> BudgetStatus.WARNING
                            else -> BudgetStatus.OK
                        }

                        BudgetProgress(
                            categoryId = budget.categoryId,
                            categoryName = category?.name ?: budget.categoryId,
                            categoryEmoji = category?.icon ?: "💰",
                            spent = budget.spent,
                            limit = budget.amount,
                            percentage = percentage,
                            status = status
                        )
                    }
                }
        )
    }

    override fun getTopTransactions(limit: Int): Flow<List<Transaction>> = flow {
        val db = databaseProvider.getDatabase()
        val queries = db.budgetMasterDatabaseQueries

        emitAll(
            queries.selectAllTransactions()
                .asFlow()
                .mapToList(dispatcher)
                .map { list ->
                    list.take(limit).map { entity ->
                        Transaction(
                            id = entity.id,
                            amount = entity.amount,
                            category = entity.categoryId ?: "Uncategorized",
                            description = entity.description,
                            timestamp = entity.timestamp
                        )
                    }
                }
        )
    }

    override suspend fun getAiInsights(forceRefresh: Boolean): Result<List<Insight>> {
        return runCatching {
            val db = databaseProvider.getDatabase()
            val queries = db.budgetMasterDatabaseQueries
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val thirtyDaysAgo = nowMs - 30L * 24 * 60 * 60 * 1000L

            val transactions = queries.selectTransactionsSince(thirtyDaysAgo)
                .awaitAsList()
                .map { entity ->
                    Transaction(
                        id = entity.id,
                        amount = entity.amount,
                        category = entity.categoryId ?: "Uncategorized",
                        description = entity.description,
                        timestamp = entity.timestamp
                    )
                }

            geminiInsightsService.getInsights(transactions, forceRefresh)
        }
    }

    override suspend fun deleteTransaction(id: String) {
        withContext(dispatcher) {
            val db = databaseProvider.getDatabase()
            db.budgetMasterDatabaseQueries.deleteTransaction(id)
        }
    }

    override suspend fun dismissInsight(id: String) {
        withContext(dispatcher) {
            val db = databaseProvider.getDatabase()
            db.budgetMasterDatabaseQueries.deleteInsight(id)
        }
    }
}
