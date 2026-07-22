@file:OptIn(ExperimentalTime::class, ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)

package com.budgetmaster.accounts.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.budgetmaster.accounts.domain.model.Account
import com.budgetmaster.accounts.domain.model.AccountDraft
import com.budgetmaster.accounts.domain.model.AccountType
import com.budgetmaster.accounts.domain.repository.AccountRepository
import com.budgetmaster.core.db.AccountEntity
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
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * SQLDelight-backed [AccountRepository]. Scopes every read/write to
 * [SessionStore.currentUserId] (falling back to the local [DefaultData.DEFAULT_USER_ID] when
 * nobody is signed in) and computes each account's current balance live from its transactions.
 */
class SqlDelightAccountRepository(
    private val databaseProvider: DatabaseProvider,
    private val sessionStore: SessionStore,
    private val seeder: AppDataSeeder,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : AccountRepository {

    override fun observeAccounts(): Flow<List<Account>> =
        sessionStore.currentUserId.flatMapLatest { uid ->
            val userId = uid ?: DefaultData.DEFAULT_USER_ID
            flow {
                seeder.seedForUser(userId)
                val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
                val accountsFlow = queries.selectAccountsByUserId(userId).asFlow().mapToList(dispatcher)
                val transactionsFlow = queries.selectTransactionsByUser(userId).asFlow().mapToList(dispatcher)
                emitAll(
                    combine(accountsFlow, transactionsFlow) { accounts, transactions ->
                        val spentByAccount = transactions.groupBy { it.accountId }
                            .mapValues { (_, rows) -> rows.sumOf { it.amount } }
                        accounts.map { it.toDomain(movement = spentByAccount[it.id] ?: 0.0) }
                    },
                )
            }
        }

    override suspend fun upsertAccount(draft: AccountDraft): String = withContext(dispatcher) {
        val userId = sessionStore.currentUserId.value ?: DefaultData.DEFAULT_USER_ID
        seeder.seedForUser(userId)
        val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        val id = draft.id
        if (id == null) {
            val newId = Uuid.random().toString()
            queries.insertAccount(
                id = newId,
                userId = userId,
                name = draft.name.trim(),
                type = draft.type.name,
                balance = draft.openingBalance,
                currency = draft.currency,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                isArchived = 0,
                // New wallets count toward totals; opting one out is a deliberate act afterwards.
                includeInTotals = 1,
            )
            newId
        } else {
            queries.updateAccount(
                name = draft.name.trim(),
                type = draft.type.name,
                balance = draft.openingBalance,
                currency = draft.currency,
                id = id,
            )
            id
        }
    }

    override suspend fun setArchived(id: String, archived: Boolean): Unit = withContext(dispatcher) {
        databaseProvider.getDatabase().budgetMasterDatabaseQueries
            .setAccountArchived(isArchived = if (archived) 1 else 0, id = id)
    }

    override suspend fun setIncludedInTotals(id: String, included: Boolean): Unit = withContext(dispatcher) {
        databaseProvider.getDatabase().budgetMasterDatabaseQueries
            .setAccountIncludedInTotals(includeInTotals = if (included) 1 else 0, id = id)
    }

    override suspend fun transfer(
        fromAccountId: String,
        toAccountId: String,
        amount: Double,
        timestamp: Long,
        note: String?,
    ): Unit = withContext(dispatcher) {
        require(fromAccountId != toAccountId) { "Cannot transfer to the same account." }
        require(amount > 0.0) { "Transfer amount must be greater than zero." }

        val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        val groupId = Uuid.random().toString()
        val label = note?.trim()?.ifBlank { null } ?: TRANSFER_LABEL

        // Both legs share transferGroupId so they can be recognised (and removed) as one move.
        queries.transaction {
            queries.insertTransaction(
                id = Uuid.random().toString(),
                accountId = fromAccountId,
                categoryId = null,
                amount = -amount,
                description = label,
                timestamp = timestamp,
                notes = null,
                tags = null,
                isRecurring = 0,
                transferGroupId = groupId,
            )
            queries.insertTransaction(
                id = Uuid.random().toString(),
                accountId = toAccountId,
                categoryId = null,
                amount = amount,
                description = label,
                timestamp = timestamp,
                notes = null,
                tags = null,
                isRecurring = 0,
                transferGroupId = groupId,
            )
        }
    }

    override suspend fun reconcile(
        accountId: String,
        actualBalance: Double,
        timestamp: Long,
    ): Unit = withContext(dispatcher) {
        val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        val account = queries.selectAccountById(accountId).awaitAsOneOrNull() ?: return@withContext
        val movement = queries.selectTransactionsByAccount(accountId).awaitAsList().sumOf { it.amount }
        val currentBalance = account.balance + movement
        val delta = actualBalance - currentBalance
        if (delta == 0.0) return@withContext

        // Post the difference as a normal entry so the balance math stays derived, never patched.
        queries.insertTransaction(
            id = Uuid.random().toString(),
            accountId = accountId,
            categoryId = null,
            amount = delta,
            description = RECONCILE_LABEL,
            timestamp = timestamp,
            notes = null,
            tags = null,
            isRecurring = 0,
            // Excluded from income/expense: an adjustment is neither earning nor spending.
            transferGroupId = "reconcile_${Uuid.random()}",
        )
    }

    override suspend fun deleteAccount(id: String): Unit = withContext(dispatcher) {
        databaseProvider.getDatabase().budgetMasterDatabaseQueries.deleteAccount(id)
    }

    private companion object {
        const val TRANSFER_LABEL = "Transfer"
        const val RECONCILE_LABEL = "Balance adjustment"
    }
}

private fun AccountEntity.toDomain(movement: Double): Account = Account(
    id = id,
    name = name,
    type = runCatching { AccountType.valueOf(type) }.getOrDefault(AccountType.CASH),
    openingBalance = balance,
    currentBalance = balance + movement,
    currency = currency,
    isArchived = isArchived == 1L,
    includeInTotals = includeInTotals == 1L,
)
