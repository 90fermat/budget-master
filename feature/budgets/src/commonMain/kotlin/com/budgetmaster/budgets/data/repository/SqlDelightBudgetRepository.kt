@file:OptIn(ExperimentalUuidApi::class, kotlin.time.ExperimentalTime::class, ExperimentalCoroutinesApi::class)

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
import com.budgetmaster.core.session.SessionStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * SQLDelight-backed [BudgetRepository], scoped to the signed-in user
 * ([SessionStore.currentUserId], falling back to the local default user). Budgets span all
 * of a user's accounts; `spent` is computed live per budget from the expense transactions of
 * its category within the budget period — the denormalized `BudgetEntity.spent` is ignored.
 */
class SqlDelightBudgetRepository(
    private val databaseProvider: DatabaseProvider,
    private val seeder: AppDataSeeder,
    private val sessionStore: SessionStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : BudgetRepository {

    private fun currentUserId(): String = sessionStore.currentUserId.value ?: DefaultData.DEFAULT_USER_ID

    override fun observeBudgets(): Flow<List<BudgetItem>> =
        sessionStore.currentUserId.flatMapLatest { uid ->
            val userId = uid ?: DefaultData.DEFAULT_USER_ID
            flow {
                seeder.seedForUser(userId)
                val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
                val now = kotlin.time.Clock.System.now().toEpochMilliseconds()

                val budgetsFlow = queries.selectBudgetsByUserId(userId, now, now)
                    .asFlow().mapToList(dispatcher)
                val categoriesFlow = queries.selectCategoriesByUserId(userId)
                    .asFlow().mapToList(dispatcher)
                // Budgets span all of a user's accounts; on a single-user device this is the
                // full transaction set. (Category + period already narrow the spent total.)
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
                                        // Transfers between the user's own wallets aren't spend.
                                        it.transferGroupId == null &&
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
                    },
                )
            }
        }

    override fun observeCategories(): Flow<List<BudgetCategory>> =
        sessionStore.currentUserId.flatMapLatest { uid ->
            val userId = uid ?: DefaultData.DEFAULT_USER_ID
            flow {
                seeder.seedForUser(userId)
                val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
                emitAll(
                    queries.selectCategoriesByUserId(userId)
                        .asFlow().mapToList(dispatcher)
                        .map { list -> list.map { BudgetCategory(it.id, it.name, it.icon, it.color) } },
                )
            }
        }

    override suspend fun upsertBudget(draft: BudgetDraft): Unit = withContext(dispatcher) {
        val userId = currentUserId()
        seeder.seedForUser(userId)
        val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        queries.insertBudget(
            id = draft.id ?: Uuid.random().toString(),
            userId = userId,
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
