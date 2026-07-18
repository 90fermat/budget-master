@file:OptIn(ExperimentalTime::class, ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)

package com.budgetmaster.transactions.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.budgetmaster.core.db.AppDataSeeder
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.db.DefaultData
import com.budgetmaster.core.session.SessionStore
import com.budgetmaster.transactions.domain.repository.ImportStatus
import com.budgetmaster.transactions.domain.repository.ImportedEntry
import com.budgetmaster.transactions.domain.repository.MoneyImportRepository
import com.budgetmaster.transactions.domain.repository.PendingImport
import com.budgetmaster.transactions.domain.repository.PendingImportDetails
import com.budgetmaster.transactions.domain.usecase.ImportEntryFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
                pendingAccountId = null,
                pendingAmount = null,
                pendingFee = null,
                pendingDescription = null,
                pendingOccurredAt = null,
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
        pending: PendingImportDetails?,
    ): Unit = withContext(dispatcher) {
        val userId = currentUserId()
        // Only a pending review keeps its parsed fields. Any other outcome stores nothing about
        // what the message said - the fingerprint alone is enough to avoid re-reading it.
        val kept = pending.takeIf { status == ImportStatus.PENDING_REVIEW }
        databaseProvider.getDatabase().budgetMasterDatabaseQueries.insertImportedMessage(
            hash = hash,
            userId = userId,
            provider = provider,
            sender = sender,
            receivedAt = receivedAt,
            status = status.name,
            transactionId = transactionId,
            externalId = externalId,
            pendingAccountId = kept?.accountId,
            pendingAmount = kept?.amount,
            pendingFee = kept?.fee,
            pendingDescription = kept?.description,
            pendingOccurredAt = kept?.occurredAt,
        )
    }

    override fun observePendingReview(): Flow<List<PendingImport>> =
        sessionStore.currentUserId.flatMapLatest { userId ->
            val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
            queries.selectPendingReview(userId ?: DefaultData.DEFAULT_USER_ID)
                .asFlow()
                .mapToList(dispatcher)
                .map { rows -> rows.mapNotNull { it.toPendingImport() } }
        }

    /**
     * Null for a row that cannot be acted on - an older PENDING_REVIEW written before the parsed
     * fields were kept, or one already resolved. Dropping it is right: showing the user a review
     * whose "keep it" button could not work would be worse than showing nothing.
     */
    private fun com.budgetmaster.core.db.ImportedMessageEntity.toPendingImport(): PendingImport? =
        PendingImport(
            hash = hash,
            provider = provider,
            receivedAt = receivedAt,
            externalId = externalId ?: return null,
            existingTransactionId = transactionId ?: return null,
            details = PendingImportDetails(
                accountId = pendingAccountId ?: return null,
                amount = pendingAmount ?: return null,
                fee = pendingFee ?: 0.0,
                description = pendingDescription ?: return null,
                occurredAt = pendingOccurredAt ?: return null,
            ),
        )

    override suspend fun resolvePending(hash: String, keep: Boolean): List<String> =
        withContext(dispatcher) {
            val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
            val row = queries.selectImportedMessageByHash(hash).awaitAsOneOrNull()
                ?: return@withContext emptyList()
            val pending = row.toPendingImport()
            if (!keep || pending == null) {
                // Same event, or nothing left to write: record the answer and stop. The link to
                // the transaction the user already had is what makes this auditable later.
                queries.resolveImportedMessage(
                    status = ImportStatus.DUPLICATE.name,
                    transactionId = row.transactionId,
                    hash = hash,
                )
                return@withContext emptyList()
            }

            val entries = ImportEntryFactory.build(
                accountId = pending.details.accountId,
                amount = pending.details.amount,
                fee = pending.details.fee,
                description = pending.details.description,
                occurredAt = pending.details.occurredAt,
                externalId = pending.externalId,
            )
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
                queries.resolveImportedMessage(
                    status = ImportStatus.IMPORTED.name,
                    transactionId = ids.first(),
                    hash = hash,
                )
            }
            ids
        }

    private companion object {
        const val SOURCE_SMS = "SMS"
    }
}
