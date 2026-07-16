@file:OptIn(ExperimentalUuidApi::class, kotlin.time.ExperimentalTime::class)

package com.budgetmaster.budgets.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.budgetmaster.budgets.domain.model.GoalDraft
import com.budgetmaster.budgets.domain.model.GoalItem
import com.budgetmaster.budgets.domain.repository.GoalRepository
import com.budgetmaster.core.db.AppDataSeeder
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.db.DefaultData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * SQLDelight-backed [GoalRepository] over `SavingsGoalEntity`.
 */
class SqlDelightGoalRepository(
    private val databaseProvider: DatabaseProvider,
    private val seeder: AppDataSeeder,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : GoalRepository {

    override fun observeGoals(): Flow<List<GoalItem>> = flow {
        seeder.seedIfNeeded()
        val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        emitAll(
            queries.selectSavingsGoalsByUserId(DefaultData.DEFAULT_USER_ID)
                .asFlow().mapToList(dispatcher)
                .map { rows ->
                    rows.map { g ->
                        GoalItem(
                            id = g.id,
                            name = g.name,
                            targetAmount = g.targetAmount,
                            currentAmount = g.currentAmount,
                            targetDate = g.targetDate,
                            createdAt = g.createdAt,
                        )
                    }
                }
        )
    }

    override suspend fun upsertGoal(draft: GoalDraft): Unit = withContext(dispatcher) {
        seeder.seedIfNeeded()
        val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        val existing = draft.id?.let { queries.selectSavingsGoalById(it).awaitAsList().firstOrNull() }
        val now = Clock.System.now().toEpochMilliseconds()
        queries.insertSavingsGoal(
            id = draft.id ?: Uuid.random().toString(),
            userId = DefaultData.DEFAULT_USER_ID,
            name = draft.name.trim(),
            targetAmount = draft.targetAmount,
            currentAmount = existing?.currentAmount ?: 0.0,
            targetDate = draft.targetDate,
            createdAt = existing?.createdAt ?: now,
        )
    }

    override suspend fun contribute(id: String, amount: Double): Unit = withContext(dispatcher) {
        val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        val existing = queries.selectSavingsGoalById(id).awaitAsList().firstOrNull() ?: return@withContext
        queries.updateSavingsGoalAmount(existing.currentAmount + amount, id)
    }

    override suspend fun deleteGoal(id: String): Unit = withContext(dispatcher) {
        databaseProvider.getDatabase().budgetMasterDatabaseQueries.deleteSavingsGoal(id)
    }
}
