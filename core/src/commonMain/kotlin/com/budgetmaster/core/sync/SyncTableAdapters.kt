package com.budgetmaster.core.sync

import com.budgetmaster.core.backup.BackupAccount
import com.budgetmaster.core.backup.BackupBudget
import com.budgetmaster.core.backup.BackupCategory
import com.budgetmaster.core.backup.BackupGoal
import com.budgetmaster.core.backup.BackupRecurring
import com.budgetmaster.core.backup.BackupTransaction
import com.budgetmaster.core.backup.BackupUser
import com.budgetmaster.core.db.BudgetMasterDatabase
import kotlinx.serialization.json.Json

/**
 * Reads dirty rows out of a table and writes remote rows back into it.
 *
 * One per synced table. The row's wire shape is the `backup` model, so a row is serialised the
 * same way whether it is going into a backup file or to a remote — one shape, one place to change.
 */
internal interface SyncTableAdapter {
    val tableName: String

    /** Rows this device owes the remote, as (rowId, updatedAt, payload). */
    suspend fun dirtyRows(db: BudgetMasterDatabase): List<Triple<String, Long, String>>

    /** The local row's state, or null when this device has no such row. */
    suspend fun localState(db: BudgetMasterDatabase, rowId: String): LocalRowState?

    /**
     * Writes a remote row in, carrying its timestamp so ordering survives.
     *
     * [exists] picks the statement, and getting it wrong is not cosmetic. Running both an insert
     * and an update means that whenever the insert is the one that lands, the update that follows
     * writes values identical to what it just wrote — which is precisely the shape the triggers
     * read as "an ordinary edit that touched neither sync column". The trigger then re-stamps the
     * freshly arrived row with *this* device's clock and marks it dirty, so every pulled row is
     * republished under a local timestamp it never had.
     */
    suspend fun applyRemote(db: BudgetMasterDatabase, payload: String, updatedAt: Long, exists: Boolean)

    /** Clears the pending-push flag once the remote has the row. */
    suspend fun markPushed(db: BudgetMasterDatabase, rowId: String)
}

/**
 * What this device currently holds for a row.
 *
 * [dirty] is what separates "I edited this and have not published it" from "this is simply a copy
 * of what someone else published". Only the former is a concurrent edit worth defending when a
 * delete arrives for the same row in the same clock tick.
 */
internal data class LocalRowState(val updatedAt: Long, val dirty: Boolean)

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/** Every synced table, in dependency order so a pull writes parents before children. */
internal fun syncAdapters(): List<SyncTableAdapter> = listOf(
    UserAdapter, AccountAdapter, CategoryAdapter,
    TransactionAdapter, BudgetAdapter, GoalAdapter, RecurringAdapter,
)

private object UserAdapter : SyncTableAdapter {
    override val tableName = "UserEntity"
    override suspend fun dirtyRows(db: BudgetMasterDatabase) =
        db.budgetMasterDatabaseQueries.selectDirtyUser().awaitList().map {
            Triple(it.id, it.updatedAt, json.encodeToString(BackupUser.serializer(), BackupUser(it.id, it.name, it.email, it.currency, it.createdAt)))
        }
    override suspend fun localState(db: BudgetMasterDatabase, rowId: String) =
        db.budgetMasterDatabaseQueries.selectUserUpdatedAt(rowId).awaitList().firstOrNull()
            ?.let { LocalRowState(it.updatedAt, it.dirty == 1L) }
    override suspend fun applyRemote(db: BudgetMasterDatabase, payload: String, updatedAt: Long, exists: Boolean) {
        val r = json.decodeFromString(BackupUser.serializer(), payload)
        if (exists) {
            db.budgetMasterDatabaseQueries.updateUserFromRemote(r.name, r.email, r.currency, r.createdAt, updatedAt, 0, r.id)
        } else {
            db.budgetMasterDatabaseQueries.insertUserFromRemote(r.id, r.name, r.email, r.currency, r.createdAt, updatedAt, 0)
        }
    }
    override suspend fun markPushed(db: BudgetMasterDatabase, rowId: String) {
        db.budgetMasterDatabaseQueries.markUserPushed(rowId)
    }
}

private object AccountAdapter : SyncTableAdapter {
    override val tableName = "AccountEntity"
    override suspend fun dirtyRows(db: BudgetMasterDatabase) =
        db.budgetMasterDatabaseQueries.selectDirtyAccount().awaitList().map {
            Triple(it.id, it.updatedAt, json.encodeToString(BackupAccount.serializer(), BackupAccount(it.id, it.userId, it.name, it.type, it.balance, it.currency, it.createdAt, it.isArchived, it.includeInTotals)))
        }
    override suspend fun localState(db: BudgetMasterDatabase, rowId: String) =
        db.budgetMasterDatabaseQueries.selectAccountUpdatedAt(rowId).awaitList().firstOrNull()
            ?.let { LocalRowState(it.updatedAt, it.dirty == 1L) }
    override suspend fun applyRemote(db: BudgetMasterDatabase, payload: String, updatedAt: Long, exists: Boolean) {
        val r = json.decodeFromString(BackupAccount.serializer(), payload)
        if (exists) {
            db.budgetMasterDatabaseQueries.updateAccountFromRemote(r.userId, r.name, r.type, r.balance, r.currency, r.createdAt, r.isArchived, r.includeInTotals, updatedAt, 0, r.id)
        } else {
            db.budgetMasterDatabaseQueries.insertAccountFromRemote(r.id, r.userId, r.name, r.type, r.balance, r.currency, r.createdAt, r.isArchived, r.includeInTotals, updatedAt, 0)
        }
    }
    override suspend fun markPushed(db: BudgetMasterDatabase, rowId: String) {
        db.budgetMasterDatabaseQueries.markAccountPushed(rowId)
    }
}

private object CategoryAdapter : SyncTableAdapter {
    override val tableName = "CategoryEntity"
    override suspend fun dirtyRows(db: BudgetMasterDatabase) =
        db.budgetMasterDatabaseQueries.selectDirtyCategory().awaitList().map {
            Triple(it.id, it.updatedAt, json.encodeToString(BackupCategory.serializer(), BackupCategory(it.id, it.userId, it.name, it.icon, it.color, it.isDefault)))
        }
    override suspend fun localState(db: BudgetMasterDatabase, rowId: String) =
        db.budgetMasterDatabaseQueries.selectCategoryUpdatedAt(rowId).awaitList().firstOrNull()
            ?.let { LocalRowState(it.updatedAt, it.dirty == 1L) }
    override suspend fun applyRemote(db: BudgetMasterDatabase, payload: String, updatedAt: Long, exists: Boolean) {
        val r = json.decodeFromString(BackupCategory.serializer(), payload)
        if (exists) {
            db.budgetMasterDatabaseQueries.updateCategoryFromRemote(r.userId, r.name, r.icon, r.color, r.isDefault, updatedAt, 0, r.id)
        } else {
            db.budgetMasterDatabaseQueries.insertCategoryFromRemote(r.id, r.userId, r.name, r.icon, r.color, r.isDefault, updatedAt, 0)
        }
    }
    override suspend fun markPushed(db: BudgetMasterDatabase, rowId: String) {
        db.budgetMasterDatabaseQueries.markCategoryPushed(rowId)
    }
}

private object TransactionAdapter : SyncTableAdapter {
    override val tableName = "TransactionEntity"
    override suspend fun dirtyRows(db: BudgetMasterDatabase) =
        db.budgetMasterDatabaseQueries.selectDirtyTransaction().awaitList().map {
            Triple(it.id, it.updatedAt, json.encodeToString(BackupTransaction.serializer(), BackupTransaction(it.id, it.accountId, it.categoryId, it.amount, it.description, it.timestamp, it.notes, it.tags, it.isRecurring, it.transferGroupId, it.externalId, it.source)))
        }
    override suspend fun localState(db: BudgetMasterDatabase, rowId: String) =
        db.budgetMasterDatabaseQueries.selectTransactionUpdatedAt(rowId).awaitList().firstOrNull()
            ?.let { LocalRowState(it.updatedAt, it.dirty == 1L) }
    override suspend fun applyRemote(db: BudgetMasterDatabase, payload: String, updatedAt: Long, exists: Boolean) {
        val r = json.decodeFromString(BackupTransaction.serializer(), payload)
        if (exists) {
            db.budgetMasterDatabaseQueries.updateTransactionFromRemote(r.accountId, r.categoryId, r.amount, r.description, r.timestamp, r.notes, r.tags, r.isRecurring, r.transferGroupId, r.externalId, r.source, updatedAt, 0, r.id)
        } else {
            db.budgetMasterDatabaseQueries.insertTransactionFromRemote(r.id, r.accountId, r.categoryId, r.amount, r.description, r.timestamp, r.notes, r.tags, r.isRecurring, r.transferGroupId, r.externalId, r.source, updatedAt, 0)
        }
    }
    override suspend fun markPushed(db: BudgetMasterDatabase, rowId: String) {
        db.budgetMasterDatabaseQueries.markTransactionPushed(rowId)
    }
}

private object BudgetAdapter : SyncTableAdapter {
    override val tableName = "BudgetEntity"
    override suspend fun dirtyRows(db: BudgetMasterDatabase) =
        db.budgetMasterDatabaseQueries.selectDirtyBudget().awaitList().map {
            // `spent` is sent as it stands but is treated as derived on arrival: see SyncEngine,
            // which recomputes it from transactions after every pull.
            Triple(it.id, it.updatedAt, json.encodeToString(BackupBudget.serializer(), BackupBudget(it.id, it.userId, it.categoryId, it.amount, it.spent, it.startDate, it.endDate)))
        }
    override suspend fun localState(db: BudgetMasterDatabase, rowId: String) =
        db.budgetMasterDatabaseQueries.selectBudgetUpdatedAt(rowId).awaitList().firstOrNull()
            ?.let { LocalRowState(it.updatedAt, it.dirty == 1L) }
    override suspend fun applyRemote(db: BudgetMasterDatabase, payload: String, updatedAt: Long, exists: Boolean) {
        val r = json.decodeFromString(BackupBudget.serializer(), payload)
        if (exists) {
            db.budgetMasterDatabaseQueries.updateBudgetFromRemote(r.userId, r.categoryId, r.amount, r.spent, r.startDate, r.endDate, updatedAt, 0, r.id)
        } else {
            db.budgetMasterDatabaseQueries.insertBudgetFromRemote(r.id, r.userId, r.categoryId, r.amount, r.spent, r.startDate, r.endDate, updatedAt, 0)
        }
    }
    override suspend fun markPushed(db: BudgetMasterDatabase, rowId: String) {
        db.budgetMasterDatabaseQueries.markBudgetPushed(rowId)
    }
}

private object GoalAdapter : SyncTableAdapter {
    override val tableName = "SavingsGoalEntity"
    override suspend fun dirtyRows(db: BudgetMasterDatabase) =
        db.budgetMasterDatabaseQueries.selectDirtySavingsGoal().awaitList().map {
            Triple(it.id, it.updatedAt, json.encodeToString(BackupGoal.serializer(), BackupGoal(it.id, it.userId, it.name, it.targetAmount, it.currentAmount, it.targetDate, it.createdAt)))
        }
    override suspend fun localState(db: BudgetMasterDatabase, rowId: String) =
        db.budgetMasterDatabaseQueries.selectSavingsGoalUpdatedAt(rowId).awaitList().firstOrNull()
            ?.let { LocalRowState(it.updatedAt, it.dirty == 1L) }
    override suspend fun applyRemote(db: BudgetMasterDatabase, payload: String, updatedAt: Long, exists: Boolean) {
        val r = json.decodeFromString(BackupGoal.serializer(), payload)
        if (exists) {
            db.budgetMasterDatabaseQueries.updateSavingsGoalFromRemote(r.userId, r.name, r.targetAmount, r.currentAmount, r.targetDate, r.createdAt, updatedAt, 0, r.id)
        } else {
            db.budgetMasterDatabaseQueries.insertSavingsGoalFromRemote(r.id, r.userId, r.name, r.targetAmount, r.currentAmount, r.targetDate, r.createdAt, updatedAt, 0)
        }
    }
    override suspend fun markPushed(db: BudgetMasterDatabase, rowId: String) {
        db.budgetMasterDatabaseQueries.markSavingsGoalPushed(rowId)
    }
}

private object RecurringAdapter : SyncTableAdapter {
    override val tableName = "RecurringTransactionEntity"
    override suspend fun dirtyRows(db: BudgetMasterDatabase) =
        db.budgetMasterDatabaseQueries.selectDirtyRecurringTransaction().awaitList().map {
            Triple(it.id, it.updatedAt, json.encodeToString(BackupRecurring.serializer(), BackupRecurring(it.id, it.accountId, it.categoryId, it.amount, it.description, it.frequency, it.startDate, it.nextRunDate, it.isActive)))
        }
    override suspend fun localState(db: BudgetMasterDatabase, rowId: String) =
        db.budgetMasterDatabaseQueries.selectRecurringTransactionUpdatedAt(rowId).awaitList().firstOrNull()
            ?.let { LocalRowState(it.updatedAt, it.dirty == 1L) }
    override suspend fun applyRemote(db: BudgetMasterDatabase, payload: String, updatedAt: Long, exists: Boolean) {
        val r = json.decodeFromString(BackupRecurring.serializer(), payload)
        if (exists) {
            db.budgetMasterDatabaseQueries.updateRecurringTransactionFromRemote(r.accountId, r.categoryId, r.amount, r.description, r.frequency, r.startDate, r.nextRunDate, r.isActive, updatedAt, 0, r.id)
        } else {
            db.budgetMasterDatabaseQueries.insertRecurringTransactionFromRemote(r.id, r.accountId, r.categoryId, r.amount, r.description, r.frequency, r.startDate, r.nextRunDate, r.isActive, updatedAt, 0)
        }
    }
    override suspend fun markPushed(db: BudgetMasterDatabase, rowId: String) {
        db.budgetMasterDatabaseQueries.markRecurringTransactionPushed(rowId)
    }
}
