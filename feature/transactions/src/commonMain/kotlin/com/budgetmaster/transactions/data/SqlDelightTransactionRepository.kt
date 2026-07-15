@file:OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)

package com.budgetmaster.transactions.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.transactions.domain.model.TransactionCategory
import com.budgetmaster.transactions.domain.model.TransactionDraft
import com.budgetmaster.transactions.domain.model.TransactionItem
import com.budgetmaster.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * SQLDelight-backed [TransactionRepository]. Ensures the default account and
 * categories exist before the first read/write so foreign-key constraints hold.
 */
class SqlDelightTransactionRepository(
    private val databaseProvider: DatabaseProvider,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : TransactionRepository {

    private val seedMutex = Mutex()
    private var seeded = false

    override fun observeTransactions(): Flow<List<TransactionItem>> = flow {
        ensureSeeded()
        val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries

        val categoriesFlow = queries.selectCategoriesByUserId(DefaultSeed.DEFAULT_USER_ID)
            .asFlow()
            .mapToList(dispatcher)

        val transactionsFlow = queries.selectAllTransactions()
            .asFlow()
            .mapToList(dispatcher)

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
                    )
                }
            }
        )
    }

    override fun observeCategories(): Flow<List<TransactionCategory>> = flow {
        ensureSeeded()
        val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        emitAll(
            queries.selectCategoriesByUserId(DefaultSeed.DEFAULT_USER_ID)
                .asFlow()
                .mapToList(dispatcher)
                .map { list -> list.map { it.toDomain() } }
        )
    }

    override suspend fun upsertTransaction(draft: TransactionDraft): TransactionItem =
        withContext(dispatcher) {
            ensureSeeded()
            val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
            val id = draft.id ?: Uuid.random().toString()
            val amount = if (draft.isExpense) -kotlin.math.abs(draft.amountAbs) else kotlin.math.abs(draft.amountAbs)
            queries.insertTransaction(
                id = id,
                accountId = DefaultSeed.DEFAULT_ACCOUNT_ID,
                categoryId = draft.categoryId,
                amount = amount,
                description = draft.description.trim(),
                timestamp = draft.timestamp,
                notes = draft.notes?.trim()?.ifBlank { null },
                tags = null,
                isRecurring = 0,
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
            )
        }

    override suspend fun deleteTransaction(id: String): Unit = withContext(dispatcher) {
        databaseProvider.getDatabase().budgetMasterDatabaseQueries.deleteTransaction(id)
    }

    override suspend fun restoreTransaction(item: TransactionItem): Unit = withContext(dispatcher) {
        databaseProvider.getDatabase().budgetMasterDatabaseQueries.insertTransaction(
            id = item.id,
            accountId = DefaultSeed.DEFAULT_ACCOUNT_ID,
            categoryId = item.category?.id,
            amount = item.amount,
            description = item.description,
            timestamp = item.timestamp,
            notes = item.notes,
            tags = null,
            isRecurring = 0,
        )
    }

    /** Inserts default user/account/categories once, if not already present. */
    private suspend fun ensureSeeded() {
        if (seeded) return
        seedMutex.withLock {
            if (seeded) return
            val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
            val existing = queries.selectCategoriesByUserId(DefaultSeed.DEFAULT_USER_ID).awaitAsList()
            if (existing.isEmpty()) {
                val now = Clock.System.now().toEpochMilliseconds()
                queries.insertUser(
                    id = DefaultSeed.DEFAULT_USER_ID,
                    name = "You",
                    email = "local@budgetmaster.app",
                    currency = DefaultSeed.DEFAULT_CURRENCY,
                    createdAt = now,
                )
                queries.insertAccount(
                    id = DefaultSeed.DEFAULT_ACCOUNT_ID,
                    userId = DefaultSeed.DEFAULT_USER_ID,
                    name = "Main Account",
                    type = "CHECKING",
                    balance = 0.0,
                    currency = DefaultSeed.DEFAULT_CURRENCY,
                    createdAt = now,
                )
                DefaultSeed.categories.forEach { cat ->
                    queries.insertCategory(
                        id = cat.id,
                        userId = DefaultSeed.DEFAULT_USER_ID,
                        name = cat.name,
                        icon = cat.icon,
                        color = cat.colorHex,
                        isDefault = 1,
                    )
                }
            }
            seeded = true
        }
    }
}

private fun com.budgetmaster.core.db.CategoryEntity.toDomain(): TransactionCategory =
    TransactionCategory(id = id, name = name, icon = icon, colorHex = color)
