@file:OptIn(ExperimentalTime::class, ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)

package com.budgetmaster.transactions.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.budgetmaster.core.db.AppDataSeeder
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.db.DefaultData
import com.budgetmaster.core.session.SessionStore
import com.budgetmaster.transactions.domain.model.Frequency
import com.budgetmaster.transactions.domain.model.RecurringDraft
import com.budgetmaster.transactions.domain.model.RecurringTransaction
import com.budgetmaster.transactions.domain.repository.RecurringRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * SQLDelight-backed [RecurringRepository], scoped to the signed-in user's wallets.
 */
class SqlDelightRecurringRepository(
    private val databaseProvider: DatabaseProvider,
    private val seeder: AppDataSeeder,
    private val sessionStore: SessionStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : RecurringRepository {

    private fun currentUserId(): String = sessionStore.currentUserId.value ?: DefaultData.DEFAULT_USER_ID

    override fun observeRecurring(): Flow<List<RecurringTransaction>> =
        sessionStore.currentUserId.flatMapLatest { uid ->
            val userId = uid ?: DefaultData.DEFAULT_USER_ID
            flow {
                seeder.seedForUser(userId)
                val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
                // Schedules hang off accounts, so the user's wallets define the scope.
                val accountsFlow = queries.selectAccountsByUserId(userId).asFlow().mapToList(dispatcher)
                val allFlow = queries.selectAllRecurringTransactions().asFlow().mapToList(dispatcher)
                emitAll(
                    combine(accountsFlow, allFlow) { accounts, rows ->
                        val owned = accounts.map { it.id }.toSet()
                        rows.filter { it.accountId in owned }.map { it.toDomain() }
                    },
                )
            }
        }

    override suspend fun upsertRecurring(draft: RecurringDraft): String = withContext(dispatcher) {
        val userId = currentUserId()
        seeder.seedForUser(userId)
        val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        val id = draft.id ?: Uuid.random().toString()
        val existing = draft.id?.let { queries.selectRecurringTransactionById(it).awaitAsOneOrNull() }
        val amount = if (draft.isExpense) -abs(draft.amountAbs) else abs(draft.amountAbs)

        queries.insertRecurringTransaction(
            id = id,
            accountId = draft.accountId ?: existing?.accountId ?: DefaultData.firstAccountId(userId),
            categoryId = draft.categoryId,
            amount = amount,
            description = draft.description.trim(),
            frequency = draft.frequency.name,
            startDate = draft.startDate,
            // Editing keeps the schedule's place in the cycle; a new one starts at startDate.
            nextRunDate = existing?.nextRunDate ?: draft.startDate,
            isActive = existing?.isActive ?: 1L,
        )
        id
    }

    override suspend fun setActive(id: String, active: Boolean): Unit = withContext(dispatcher) {
        val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        val existing = queries.selectRecurringTransactionById(id).awaitAsOneOrNull() ?: return@withContext
        queries.insertRecurringTransaction(
            id = existing.id,
            accountId = existing.accountId,
            categoryId = existing.categoryId,
            amount = existing.amount,
            description = existing.description,
            frequency = existing.frequency,
            startDate = existing.startDate,
            nextRunDate = existing.nextRunDate,
            isActive = if (active) 1L else 0L,
        )
    }

    override suspend fun deleteRecurring(id: String): Unit = withContext(dispatcher) {
        databaseProvider.getDatabase().budgetMasterDatabaseQueries.deleteRecurringTransaction(id)
    }

    override suspend fun materializeDue(now: Long): Int = withContext(dispatcher) {
        val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        val due = queries.selectActiveRecurringTransactions(now).awaitAsList()
        var created = 0

        due.forEach { schedule ->
            val frequency = runCatching { Frequency.valueOf(schedule.frequency) }
                .getOrNull() ?: return@forEach

            // Catch up occurrence by occurrence: an app unopened for months still produces
            // one entry per missed period, not a single lump.
            var runAt = schedule.nextRunDate
            var guard = 0
            while (runAt <= now && guard < MAX_CATCH_UP) {
                queries.insertTransaction(
                    // Deterministic id per occurrence, so re-running can't duplicate an entry.
                    id = "rec_${schedule.id}_$runAt",
                    accountId = schedule.accountId,
                    categoryId = schedule.categoryId,
                    amount = schedule.amount,
                    description = schedule.description,
                    timestamp = runAt,
                    notes = null,
                    tags = null,
                    isRecurring = 1,
                    transferGroupId = null,
                )
                created++
                runAt = frequency.next(runAt)
                guard++
            }
            if (runAt != schedule.nextRunDate) queries.updateRecurringNextRun(runAt, schedule.id)
        }
        created
    }

    private companion object {
        /** Ceiling on catch-up per schedule, so a far-past start date can't spin forever. */
        const val MAX_CATCH_UP = 500
    }
}

private fun com.budgetmaster.core.db.RecurringTransactionEntity.toDomain(): RecurringTransaction =
    RecurringTransaction(
        id = id,
        accountId = accountId,
        categoryId = categoryId,
        amount = amount,
        description = description,
        frequency = runCatching { Frequency.valueOf(frequency) }.getOrDefault(Frequency.MONTHLY),
        startDate = startDate,
        nextRunDate = nextRunDate,
        isActive = isActive == 1L,
    )
