@file:OptIn(ExperimentalUuidApi::class, kotlin.time.ExperimentalTime::class)

package com.budgetmaster.budgets.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.budgetmaster.budgets.domain.model.BudgetCategory
import com.budgetmaster.budgets.domain.model.BudgetDraft
import com.budgetmaster.budgets.domain.model.BudgetItem
import com.budgetmaster.budgets.domain.repository.BudgetRepository
import com.budgetmaster.core.db.AppDataSeeder
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.db.DefaultData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * SQLDelight-backed [BudgetRepository]. `spent` is computed live per budget from the
 * expense transactions of its category within the budget period — the denormalized
 * `BudgetEntity.spent` column is ignored.
 */
class SqlDelightBudgetRepository(
    private val databaseProvider: DatabaseProvider,
    private val seeder: AppDataSeeder,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : BudgetRepository {

    override fun observeBudgets(): Flow<List<BudgetItem>> = flow {
        seeder.seedIfNeeded()
        val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()

        val budgetsFlow = queries.selectBudgetsByUserId(DefaultData.DEFAULT_USER_ID, now, now)
            .asFlow().mapToList(dispatcher)
        val categoriesFlow = queries.selectCategoriesByUserId(DefaultData.DEFAULT_USER_ID)
            .asFlow().mapToList(dispatcher)
        val transactionsFlow = queries.selectAllTransactions()
            .asFlow().mapToList(dispatcher)

        emitAll(
            combine(budgetsFlow, categoriesFlow, transactionsFlow) { budgets, categories, transactions ->
                val categoryById = categories.associateBy { it.id }
                budgets.mapNotNull { budget ->
                    val category = categoryById[budget.categoryId] ?: return@mapNotNull null
                    val spent = transactions
                        .filter {
                            it.categoryId == budget.categoryId &&
                                it.amount < 0 &&
                                it.timestamp in budget.startDate..budget.endDate
                        }
                        .sumOf { abs(it.amount) }
                    BudgetItem(
                        id = budget.id,
                        category = BudgetCategory(category.id, category.name, category.icon, category.color),
                        limit = budget.amount,
                        spent = spent,
                        periodStart = budget.startDate,
                        periodEnd = budget.endDate,
                    )
                }.sortedByDescending { it.ratio }
            }
        )
    }

    override fun observeCategories(): Flow<List<BudgetCategory>> = flow {
        seeder.seedIfNeeded()
        val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        emitAll(
            queries.selectCategoriesByUserId(DefaultData.DEFAULT_USER_ID)
                .asFlow().mapToList(dispatcher)
                .map { list -> list.map { BudgetCategory(it.id, it.name, it.icon, it.color) } }
        )
    }

    override suspend fun upsertBudget(draft: BudgetDraft): Unit = withContext(dispatcher) {
        seeder.seedIfNeeded()
        val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        queries.insertBudget(
            id = draft.id ?: Uuid.random().toString(),
            userId = DefaultData.DEFAULT_USER_ID,
            categoryId = draft.categoryId,
            amount = draft.limit,
            spent = 0.0,
            startDate = draft.periodStart,
            endDate = draft.periodEnd,
        )
    }

    override suspend fun deleteBudget(id: String): Unit = withContext(dispatcher) {
        databaseProvider.getDatabase().budgetMasterDatabaseQueries.deleteBudget(id)
    }
}
