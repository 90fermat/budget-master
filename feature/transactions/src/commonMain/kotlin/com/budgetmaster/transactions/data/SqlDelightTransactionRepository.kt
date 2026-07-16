@file:OptIn(ExperimentalTime::class, ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)

package com.budgetmaster.transactions.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.budgetmaster.core.db.AppDataSeeder
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.db.DefaultData
import com.budgetmaster.core.session.ActiveAccountStore
import com.budgetmaster.core.session.SessionStore
import com.budgetmaster.transactions.domain.model.TransactionAccount
import com.budgetmaster.transactions.domain.model.TransactionCategory
import com.budgetmaster.transactions.domain.model.TransactionDraft
import com.budgetmaster.transactions.domain.model.TransactionItem
import com.budgetmaster.transactions.domain.repository.TransactionRepository
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
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * SQLDelight-backed [TransactionRepository], scoped to the signed-in user
 * ([SessionStore.currentUserId], falling back to the local default user) and the active
 * account ([ActiveAccountStore]): observing "All accounts" spans the user's wallets, while a
 * selected wallet filters to it. New transactions are written to the active wallet (or the
 * user's first wallet when viewing all).
 */
class SqlDelightTransactionRepository(
    private val databaseProvider: DatabaseProvider,
    private val seeder: AppDataSeeder,
    private val sessionStore: SessionStore,
    private val activeAccountStore: ActiveAccountStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : TransactionRepository {

    override fun observeTransactions(): Flow<List<TransactionItem>> =
        combine(sessionStore.currentUserId, activeAccountStore.activeAccountId) { uid, acc -> uid to acc }
            .flatMapLatest { (uid, activeAccount) ->
                val userId = uid ?: DefaultData.DEFAULT_USER_ID
                flow {
                    seeder.seedForUser(userId)
                    val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
                    val categoriesFlow = queries.selectCategoriesByUserId(userId)
                        .asFlow().mapToList(dispatcher)
                    val transactionsQuery = if (activeAccount != null) {
                        queries.selectTransactionsByAccount(activeAccount)
                    } else {
                        queries.selectTransactionsByUser(userId)
                    }
                    val transactionsFlow = transactionsQuery.asFlow().mapToList(dispatcher)
                    emitAll(
                        combine(transactionsFlow, categoriesFlow) { transactions, categories ->
                            val byId = categories.associateBy { it.id }
                            transactions.map { entity ->
                                TransactionItem(
                                    id = entity.id,
                                    amount = entity.amount,
                                    description = entity.description,
                                    timestamp = entity.timestamp,
                                    category = entity.categoryId?.let { byId[it] }?.toDomain(),
                                    notes = entity.notes,
                                    accountId = entity.accountId,
                                    isRecurring = entity.isRecurring == 1L,
                                )
                            }
                        },
                    )
                }
            }

    override fun observeCategories(): Flow<List<TransactionCategory>> =
        sessionStore.currentUserId.flatMapLatest { uid ->
            val userId = uid ?: DefaultData.DEFAULT_USER_ID
            flow {
                seeder.seedForUser(userId)
                val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
                emitAll(
                    queries.selectCategoriesByUserId(userId)
                        .asFlow()
                        .mapToList(dispatcher)
                        .map { list -> list.map { it.toDomain() } },
                )
            }
        }

    override fun observeAccounts(): Flow<List<TransactionAccount>> =
        sessionStore.currentUserId.flatMapLatest { uid ->
            val userId = uid ?: DefaultData.DEFAULT_USER_ID
            flow {
                seeder.seedForUser(userId)
                val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
                emitAll(
                    queries.selectActiveAccountsByUserId(userId)
                        .asFlow()
                        .mapToList(dispatcher)
                        .map { rows -> rows.map { TransactionAccount(it.id, it.name, it.currency) } },
                )
            }
        }

    override suspend fun upsertTransaction(draft: TransactionDraft): TransactionItem =
        withContext(dispatcher) {
            val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
            val id = draft.id ?: Uuid.random().toString()
            val amount = if (draft.isExpense) -kotlin.math.abs(draft.amountAbs) else kotlin.math.abs(draft.amountAbs)
            val accountId = draft.accountId ?: resolveAccountId()
            queries.insertTransaction(
                id = id,
                accountId = accountId,
                categoryId = draft.categoryId,
                amount = amount,
                description = draft.description.trim(),
                timestamp = draft.timestamp,
                notes = draft.notes?.trim()?.ifBlank { null },
                tags = null,
                isRecurring = if (draft.isRecurring) 1 else 0,
            )
            val category = draft.categoryId?.let { catId ->
                queries.selectCategoryById(catId).awaitAsList().firstOrNull()?.toDomain()
            }
            TransactionItem(
                id = id,
                amount = amount,
                description = draft.description.trim(),
                timestamp = draft.timestamp,
                category = category,
                notes = draft.notes?.trim()?.ifBlank { null },
                accountId = accountId,
                isRecurring = draft.isRecurring,
            )
        }

    override suspend fun deleteTransaction(id: String): Unit = withContext(dispatcher) {
        databaseProvider.getDatabase().budgetMasterDatabaseQueries.deleteTransaction(id)
    }

    override suspend fun restoreTransaction(item: TransactionItem): Unit = withContext(dispatcher) {
        databaseProvider.getDatabase().budgetMasterDatabaseQueries.insertTransaction(
            id = item.id,
            // Undo must put the entry back on its original wallet, not the active one.
            accountId = item.accountId.ifBlank { resolveAccountId() },
            categoryId = item.category?.id,
            amount = item.amount,
            description = item.description,
            timestamp = item.timestamp,
            notes = item.notes,
            tags = null,
            isRecurring = if (item.isRecurring) 1 else 0,
        )
    }

    /** The wallet a new transaction targets: the active one, else the user's first wallet. */
    private suspend fun resolveAccountId(): String {
        val userId = sessionStore.currentUserId.value ?: DefaultData.DEFAULT_USER_ID
        seeder.seedForUser(userId)
        return activeAccountStore.activeAccountId.first() ?: DefaultData.firstAccountId(userId)
    }
}

private fun com.budgetmaster.core.db.CategoryEntity.toDomain(): TransactionCategory =
    TransactionCategory(id = id, name = name, icon = icon, colorHex = color)
