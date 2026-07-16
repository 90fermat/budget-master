@file:OptIn(ExperimentalTime::class, ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)

package com.budgetmaster.accounts.data.repository

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

    override suspend fun deleteAccount(id: String): Unit = withContext(dispatcher) {
        databaseProvider.getDatabase().budgetMasterDatabaseQueries.deleteAccount(id)
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
)
