@file:OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)

package com.budgetmaster.reports.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.db.DefaultData
import com.budgetmaster.core.db.TransactionEntity
import com.budgetmaster.core.prefs.AppSettingsRepository
import com.budgetmaster.core.session.ActiveAccountStore
import com.budgetmaster.core.session.SessionStore
import com.budgetmaster.core.util.DateUtils
import com.budgetmaster.reports.domain.model.CategorySlice
import com.budgetmaster.reports.domain.model.ReportRange
import com.budgetmaster.reports.domain.model.ReportSummary
import com.budgetmaster.reports.domain.model.TrendPoint
import com.budgetmaster.reports.domain.repository.ReportsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * SQLDelight-backed [ReportsRepository].
 *
 * Reads the same rows as the rest of the app — scoped to the signed-in user and the active
 * wallet — and excludes transfers/adjustments from the analysis, matching the dashboard and
 * budget rules.
 */
class SqlDelightReportsRepository(
    private val databaseProvider: DatabaseProvider,
    private val sessionStore: SessionStore,
    private val activeAccountStore: ActiveAccountStore,
    private val settingsRepository: AppSettingsRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ReportsRepository {

    override fun observeReport(range: ReportRange): Flow<ReportSummary> =
        combine(
            sessionStore.currentUserId,
            activeAccountStore.activeAccountId,
            settingsRepository.settings.map { it.currency },
        ) { uid, accountId, currency -> Triple(uid, accountId, currency) }
            .flatMapLatest { (uid, accountId, currency) ->
                val userId = uid ?: DefaultData.DEFAULT_USER_ID
                flow {
                    val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
                    val transactionsFlow = (
                        if (accountId != null) queries.selectTransactionsByAccount(accountId)
                        else queries.selectTransactionsByUser(userId)
                        ).asFlow().mapToList(dispatcher)
                    val categoriesFlow = queries.selectCategoriesByUserId(userId)
                        .asFlow().mapToList(dispatcher)

                    emitAll(
                        combine(transactionsFlow, categoriesFlow) { transactions, categories ->
                            summarize(
                                range = range,
                                rows = transactions,
                                categoryNames = categories.associate { it.id to (it.name to it.color) },
                                currency = currency,
                            )
                        },
                    )
                }
            }

    override suspend fun exportCsv(range: ReportRange): String = withContext(dispatcher) {
        val userId = sessionStore.currentUserId.value ?: DefaultData.DEFAULT_USER_ID
        val accountId = activeAccountStore.activeAccountId.first()
        val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries

        val rows = (
            if (accountId != null) queries.selectTransactionsByAccount(accountId)
            else queries.selectTransactionsByUser(userId)
            ).awaitAsList()
        val categories = queries.selectCategoriesByUserId(userId).awaitAsList().associateBy { it.id }
        val accounts = queries.selectAccountsByUserId(userId).awaitAsList().associateBy { it.id }

        val (start, end) = range.bounds()
        buildString {
            appendLine("Date,Description,Category,Account,Amount,Transfer,Notes")
            rows.asSequence()
                .filter { it.timestamp in start..end }
                .sortedByDescending { it.timestamp }
                .forEach { row ->
                    appendLine(
                        listOf(
                            DateUtils.toLocalDate(row.timestamp).toString(),
                            row.description,
                            row.categoryId?.let { categories[it]?.name } ?: "",
                            accounts[row.accountId]?.name ?: "",
                            row.amount.toString(),
                            if (row.transferGroupId != null) "yes" else "no",
                            row.notes ?: "",
                        ).joinToString(",") { it.csvEscaped() },
                    )
                }
        }
    }

    private fun summarize(
        range: ReportRange,
        rows: List<TransactionEntity>,
        categoryNames: Map<String, Pair<String, String>>,
        currency: String,
    ): ReportSummary {
        val (start, end) = range.bounds()
        val periodLength = end - start
        // Transfers/adjustments move the user's own money; counting them would inflate both sides.
        val flows = rows.filter { it.transferGroupId == null }
        val current = flows.filter { it.timestamp in start..end }
        val previous = flows.filter { it.timestamp in (start - periodLength) until start }

        val income = current.filter { it.amount > 0 }.sumOf { it.amount }
        val expenses = current.filter { it.amount < 0 }.sumOf { abs(it.amount) }

        val spendByCategory = current.filter { it.amount < 0 }
            .groupBy { it.categoryId }
            .map { (categoryId, group) -> categoryId to group.sumOf { abs(it.amount) } }
            .sortedByDescending { it.second }

        val categories = spendByCategory.map { (categoryId, amount) ->
            val (name, color) = categoryId?.let { categoryNames[it] } ?: ("Uncategorized" to "#94A3B8")
            CategorySlice(
                categoryId = categoryId ?: "uncategorized",
                name = name,
                colorHex = color,
                amount = amount,
                share = if (expenses > 0.0) (amount / expenses).toFloat() else 0f,
            )
        }

        val trend = current
            .groupBy { DateUtils.toLocalDate(it.timestamp) }
            .map { (date, group) ->
                TrendPoint(
                    date = date,
                    income = group.filter { it.amount > 0 }.sumOf { it.amount },
                    expenses = group.filter { it.amount < 0 }.sumOf { abs(it.amount) },
                )
            }
            .sortedBy { it.date }

        return ReportSummary(
            range = range,
            totalIncome = income,
            totalExpenses = expenses,
            categories = categories,
            trend = trend,
            previousIncome = previous.filter { it.amount > 0 }.sumOf { it.amount },
            previousExpenses = previous.filter { it.amount < 0 }.sumOf { abs(it.amount) },
            currencyCode = currency,
        )
    }
}

/** Epoch-ms bounds of the range, ending now. */
private fun ReportRange.bounds(): Pair<Long, Long> {
    val now = Clock.System.now().toEpochMilliseconds()
    val day = 24L * 60 * 60 * 1000
    val start = when (this) {
        ReportRange.MONTH -> now - 30 * day
        ReportRange.QUARTER -> now - 90 * day
        ReportRange.YEAR -> now - 365 * day
        ReportRange.ALL -> 0L
    }
    return start to now
}

/** RFC 4180: quote when the value contains a comma, quote, or newline; double inner quotes. */
private fun String.csvEscaped(): String =
    if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + replace("\"", "\"\"") + "\""
    } else {
        this
    }
