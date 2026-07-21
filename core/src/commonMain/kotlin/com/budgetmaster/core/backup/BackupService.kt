@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.core.backup

import app.cash.sqldelight.async.coroutines.awaitAsList
import com.budgetmaster.core.db.BudgetMasterDatabase
import com.budgetmaster.core.db.DatabaseProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Reads the database into a [BackupEnvelope], and writes one back.
 *
 * Restore is **replace-all**, not merge, and runs in a single transaction. Merging two ledgers
 * without sync metadata is guesswork — there is no way to tell an edited row from a different row
 * that happens to share an id — and a wrong guess about someone's money is worse than a
 * predictable overwrite they were warned about. The transaction means a failure part-way leaves
 * the existing data untouched rather than half-replaced, which for a restore is the difference
 * between an inconvenience and a disaster.
 */
class BackupService(
    private val databaseProvider: DatabaseProvider,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    /** Snapshots everything restorable. */
    suspend fun export(): BackupEnvelope = withContext(dispatcher) {
        val q = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        BackupEnvelope(
            schemaVersion = BudgetMasterDatabase.Schema.version,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            users = q.selectAllUsersForBackup().awaitAsList().map {
                BackupUser(it.id, it.name, it.email, it.currency, it.createdAt)
            },
            accounts = q.selectAllAccountsForBackup().awaitAsList().map {
                BackupAccount(it.id, it.userId, it.name, it.type, it.balance, it.currency, it.createdAt, it.isArchived)
            },
            categories = q.selectAllCategoriesForBackup().awaitAsList().map {
                BackupCategory(it.id, it.userId, it.name, it.icon, it.color, it.isDefault)
            },
            transactions = q.selectAllTransactions().awaitAsList().map {
                BackupTransaction(
                    id = it.id,
                    accountId = it.accountId,
                    categoryId = it.categoryId,
                    amount = it.amount,
                    description = it.description,
                    timestamp = it.timestamp,
                    notes = it.notes,
                    tags = it.tags,
                    isRecurring = it.isRecurring,
                    transferGroupId = it.transferGroupId,
                    externalId = it.externalId,
                    source = it.source,
                )
            },
            budgets = q.selectAllBudgetsForBackup().awaitAsList().map {
                BackupBudget(it.id, it.userId, it.categoryId, it.amount, it.spent, it.startDate, it.endDate)
            },
            goals = q.selectAllGoalsForBackup().awaitAsList().map {
                BackupGoal(it.id, it.userId, it.name, it.targetAmount, it.currentAmount, it.targetDate, it.createdAt)
            },
            recurring = q.selectAllRecurringTransactions().awaitAsList().map {
                BackupRecurring(
                    id = it.id,
                    accountId = it.accountId,
                    categoryId = it.categoryId,
                    amount = it.amount,
                    description = it.description,
                    frequency = it.frequency,
                    startDate = it.startDate,
                    nextRunDate = it.nextRunDate,
                    isActive = it.isActive,
                )
            },
            notifications = q.selectAllNotificationsForBackup().awaitAsList().map {
                BackupNotification(it.id, it.userId, it.title, it.message, it.timestamp, it.isRead)
            },
            importedMessages = q.selectAllImportedMessagesForBackup().awaitAsList().map {
                BackupImportedMessage(
                    hash = it.hash,
                    userId = it.userId,
                    provider = it.provider,
                    sender = it.sender,
                    receivedAt = it.receivedAt,
                    status = it.status,
                    transactionId = it.transactionId,
                    externalId = it.externalId,
                    pendingAccountId = it.pendingAccountId,
                    pendingAmount = it.pendingAmount,
                    pendingFee = it.pendingFee,
                    pendingDescription = it.pendingDescription,
                    pendingOccurredAt = it.pendingOccurredAt,
                )
            },
        )
    }

    /**
     * Replaces all local data with [envelope].
     *
     * Deletes run before inserts and in dependency order, inside one transaction, so foreign keys
     * hold at every point and nothing survives from the previous contents.
     */
    suspend fun restore(envelope: BackupEnvelope): Unit = withContext(dispatcher) {
        val q = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        q.transaction {
            q.deleteAllImportedMessages()
            q.deleteAllNotifications()
            q.deleteAllRecurring()
            q.deleteAllTransactions()
            q.deleteAllBudgets()
            q.deleteAllGoals()
            q.deleteAllCategories()
            q.deleteAllAccounts()
            q.deleteAllUsers()

            // Parents before children, mirroring the delete order in reverse.
            envelope.users.forEach { q.insertUser(it.id, it.name, it.email, it.currency, it.createdAt) }
            envelope.accounts.forEach {
                q.insertAccount(it.id, it.userId, it.name, it.type, it.balance, it.currency, it.createdAt, it.isArchived)
            }
            envelope.categories.forEach {
                q.insertCategory(it.id, it.userId, it.name, it.icon, it.color, it.isDefault)
            }
            envelope.transactions.forEach {
                // The import-aware insert, so externalId and source survive a restore and a
                // re-sent provider message still deduplicates against the restored row.
                q.insertImportedTransaction(
                    id = it.id,
                    accountId = it.accountId,
                    categoryId = it.categoryId,
                    amount = it.amount,
                    description = it.description,
                    timestamp = it.timestamp,
                    notes = it.notes,
                    tags = it.tags,
                    isRecurring = it.isRecurring,
                    transferGroupId = it.transferGroupId,
                    externalId = it.externalId,
                    source = it.source,
                )
            }
            envelope.budgets.forEach {
                q.insertBudget(it.id, it.userId, it.categoryId, it.amount, it.spent, it.startDate, it.endDate)
            }
            envelope.goals.forEach {
                q.insertSavingsGoal(it.id, it.userId, it.name, it.targetAmount, it.currentAmount, it.targetDate, it.createdAt)
            }
            envelope.recurring.forEach {
                q.insertRecurringTransaction(
                    id = it.id,
                    accountId = it.accountId,
                    categoryId = it.categoryId,
                    amount = it.amount,
                    description = it.description,
                    frequency = it.frequency,
                    startDate = it.startDate,
                    nextRunDate = it.nextRunDate,
                    isActive = it.isActive,
                )
            }
            envelope.notifications.forEach {
                q.insertNotification(it.id, it.userId, it.title, it.message, it.timestamp, it.isRead)
            }
            envelope.importedMessages.forEach {
                q.insertImportedMessage(
                    hash = it.hash,
                    userId = it.userId,
                    provider = it.provider,
                    sender = it.sender,
                    receivedAt = it.receivedAt,
                    status = it.status,
                    transactionId = it.transactionId,
                    externalId = it.externalId,
                    pendingAccountId = it.pendingAccountId,
                    pendingAmount = it.pendingAmount,
                    pendingFee = it.pendingFee,
                    pendingDescription = it.pendingDescription,
                    pendingOccurredAt = it.pendingOccurredAt,
                )
            }
        }
    }
}
