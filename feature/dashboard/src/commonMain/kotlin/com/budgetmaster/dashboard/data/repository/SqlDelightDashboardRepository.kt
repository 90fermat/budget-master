@file:OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)

package com.budgetmaster.dashboard.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.db.DefaultData
import com.budgetmaster.core.model.Transaction
import com.budgetmaster.core.session.ActiveAccountStore
import com.budgetmaster.core.session.SessionStore
import com.budgetmaster.dashboard.domain.model.DeletedTransaction
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
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
    private val sessionStore: SessionStore,
    private val activeAccountStore: ActiveAccountStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : DashboardRepository {

    private fun userId(): String = sessionStore.currentUserId.value ?: DefaultData.DEFAULT_USER_ID

    /**
     * Transactions in scope: the active wallet when one is selected, otherwise every wallet
     * the current user owns ("All accounts").
     */
    private suspend fun transactionsInScope(activeAccountId: String?) =
        databaseProvider.getDatabase().budgetMasterDatabaseQueries.let { q ->
            if (activeAccountId != null) q.selectTransactionsByAccount(activeAccountId)
            else q.selectTransactionsByUser(userId())
        }

    override fun getBalanceSummary(period: Period): Flow<BalanceSummary> =
        activeAccountStore.activeAccountId.flatMapLatest { activeAccountId -> flow {
        val db = databaseProvider.getDatabase()
        val queries = db.budgetMasterDatabaseQueries

        emitAll(
            transactionsInScope(activeAccountId)
                .asFlow()
                .mapToList(dispatcher)
                .map { transactions ->
                    val nowMs = Clock.System.now().toEpochMilliseconds()
                    val thirtyDaysAgo = nowMs - 30L * 24 * 60 * 60 * 1000L

                    // Transfers and balance adjustments move the user's own money between
                    // their wallets — they are neither income nor spending.
                    val flows = transactions.filter { it.transferGroupId == null }

                    val monthlyIncome = flows
                        .filter { it.timestamp >= thirtyDaysAgo && it.amount > 0 }
                        .sumOf { it.amount }

                    val monthlyExpenses = flows
                        .filter { it.timestamp >= thirtyDaysAgo && it.amount < 0 }
                        .sumOf { kotlin.math.abs(it.amount) }

                    // Account balances are opening balances; the live total is that plus
                    // the signed sum of the transactions in scope (single source of truth
                    // shared with the transactions/accounts features).
                    val accounts = queries.selectAccountsByUserId(userId()).awaitAsList()
                        .filter { activeAccountId == null || it.id == activeAccountId }
                    val openingBalance = accounts.sumOf { it.balance }
                    val totalBalance = openingBalance + transactions.sumOf { it.amount }

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
    } }

    override fun getChartData(period: Period): Flow<List<ChartPoint>> =
        activeAccountStore.activeAccountId.flatMapLatest { activeAccountId -> flow {
        emitAll(
            transactionsInScope(activeAccountId)
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
    } }

    override fun getBudgetProgress(): Flow<List<BudgetProgress>> = flow {
        val db = databaseProvider.getDatabase()
        val queries = db.budgetMasterDatabaseQueries
        val nowMs = Clock.System.now().toEpochMilliseconds()

        emitAll(
            queries.selectBudgetsByUserId(userId(), nowMs, nowMs)
                .asFlow()
                .mapToList(dispatcher)
                .map { budgets ->
                    val categories = queries.selectCategoriesByUserId(userId()).awaitAsList().associateBy { it.id }
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

    override fun getTopTransactions(limit: Int): Flow<List<Transaction>> =
        activeAccountStore.activeAccountId.flatMapLatest { activeAccountId -> flow {
        emitAll(
            transactionsInScope(activeAccountId)
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
    } }

    override val isAiConfigured: Boolean
        get() = geminiInsightsService.isConfigured

    override suspend fun getAiInsights(forceRefresh: Boolean, languageTag: String): Result<List<Insight>> {
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

            geminiInsightsService.getInsights(transactions, forceRefresh, languageTag)
        }
    }

    override suspend fun deleteTransaction(id: String): DeletedTransaction? =
        withContext(dispatcher) {
            val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
            // Read the whole row first: once it is gone there is nothing left to reconstruct an
            // undo from, and the display model is too lossy to serve as the record.
            val row = queries.selectTransactionById(id).awaitAsOneOrNull()
                ?: return@withContext null
            queries.deleteTransaction(id)
            DeletedTransaction(
                id = row.id,
                accountId = row.accountId,
                categoryId = row.categoryId,
                amount = row.amount,
                description = row.description,
                timestamp = row.timestamp,
                notes = row.notes,
                tags = row.tags,
                isRecurring = row.isRecurring,
                transferGroupId = row.transferGroupId,
                externalId = row.externalId,
                source = row.source,
            )
        }

    override suspend fun restoreTransaction(snapshot: DeletedTransaction) {
        withContext(dispatcher) {
            // insertImportedTransaction rather than insertTransaction: it is the only insert that
            // carries externalId and source, and dropping those on an undo would let a
            // mobile-money re-send import the same payment a second time.
            databaseProvider.getDatabase().budgetMasterDatabaseQueries.insertImportedTransaction(
                id = snapshot.id,
                accountId = snapshot.accountId,
                categoryId = snapshot.categoryId,
                amount = snapshot.amount,
                description = snapshot.description,
                timestamp = snapshot.timestamp,
                notes = snapshot.notes,
                tags = snapshot.tags,
                isRecurring = snapshot.isRecurring,
                transferGroupId = snapshot.transferGroupId,
                externalId = snapshot.externalId,
                source = snapshot.source,
            )
        }
    }

    override suspend fun dismissInsight(id: String) {
        withContext(dispatcher) {
            val db = databaseProvider.getDatabase()
            db.budgetMasterDatabaseQueries.deleteInsight(id)
        }
    }
}
