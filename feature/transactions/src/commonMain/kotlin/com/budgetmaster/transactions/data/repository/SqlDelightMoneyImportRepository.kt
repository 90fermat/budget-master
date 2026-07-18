@file:OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)

package com.budgetmaster.transactions.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.async.coroutines.awaitAsList
import com.budgetmaster.core.db.AppDataSeeder
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.db.DefaultData
import com.budgetmaster.core.session.SessionStore
import com.budgetmaster.transactions.domain.repository.ImportStatus
import com.budgetmaster.transactions.domain.repository.ImportedEntry
import com.budgetmaster.transactions.domain.repository.MoneyImportRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * SQLDelight-backed message import.
 *
 * Writes are wrapped in one transaction so a message can never be half-imported: either the ledger
 * rows and the "handled" record both land, or neither does. A partial write here would be
 * particularly nasty — the message would look processed while its transaction was missing.
 */
class SqlDelightMoneyImportRepository(
    private val databaseProvider: DatabaseProvider,
    private val sessionStore: SessionStore,
    private val seeder: AppDataSeeder,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : MoneyImportRepository {

    private fun currentUserId(): String =
        sessionStore.currentUserId.value ?: DefaultData.DEFAULT_USER_ID

    override suspend fun hasSeenMessage(hash: String): Boolean = withContext(dispatcher) {
        databaseProvider.getDatabase().budgetMasterDatabaseQueries
            .selectImportedMessageByHash(hash)
            .awaitAsOneOrNull() != null
    }

    override suspend fun findTransactionIdByExternalId(externalId: String): String? =
        withContext(dispatcher) {
            databaseProvider.getDatabase().budgetMasterDatabaseQueries
                .selectTransactionByExternalId(externalId)
                .awaitAsOneOrNull()
                ?.id
        }

    override suspend fun findPossibleManualDuplicate(
        amount: Double,
        dayStart: Long,
        dayEnd: Long,
    ): String? = withContext(dispatcher) {
        val userId = currentUserId()
        databaseProvider.getDatabase().budgetMasterDatabaseQueries
            .selectTransactionsForDuplicateCheck(userId, dayStart, dayEnd, amount)
            .awaitAsList()
            .firstOrNull()
            ?.id
    }

    override suspend fun saveImported(
        hash: String,
        provider: String,
        sender: String,
        receivedAt: Long,
        externalId: String,
        entries: List<ImportedEntry>,
    ): List<String> = withContext(dispatcher) {
        val userId = currentUserId()
        seeder.seedForUser(userId)
        val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        val ids = entries.map { Uuid.random().toString() }

        queries.transaction {
            entries.forEachIndexed { index, entry ->
                queries.insertImportedTransaction(
                    id = ids[index],
                    accountId = entry.accountId,
                    categoryId = entry.categoryId,
                    amount = entry.amount,
                    description = entry.description,
                    timestamp = entry.timestamp,
                    notes = null,
                    tags = null,
                    isRecurring = 0,
                    transferGroupId = null,
                    externalId = entry.externalId,
                    source = SOURCE_SMS,
                )
            }
            queries.insertImportedMessage(
                hash = hash,
                userId = userId,
                provider = provider,
                sender = sender,
                receivedAt = receivedAt,
                status = ImportStatus.IMPORTED.name,
                // The principal, so the audit trail points at the entry the user recognises.
                transactionId = ids.firstOrNull(),
                externalId = externalId,
            )
        }
        ids
    }

    override suspend fun recordMessageOutcome(
        hash: String,
        provider: String,
        sender: String,
        receivedAt: Long,
        status: ImportStatus,
        externalId: String?,
        transactionId: String?,
    ): Unit = withContext(dispatcher) {
        val userId = currentUserId()
        databaseProvider.getDatabase().budgetMasterDatabaseQueries.insertImportedMessage(
            hash = hash,
            userId = userId,
            provider = provider,
            sender = sender,
            receivedAt = receivedAt,
            status = status.name,
            transactionId = transactionId,
            externalId = externalId,
        )
    }

    private companion object {
        const val SOURCE_SMS = "SMS"
    }
}
