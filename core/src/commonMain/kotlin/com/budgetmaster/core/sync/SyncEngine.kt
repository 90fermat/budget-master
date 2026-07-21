@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.core.sync

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.Query
import com.budgetmaster.core.db.DatabaseProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Small shim so the adapters read the same whichever query they call. */
internal suspend fun <T : Any> Query<T>.awaitList(): List<T> = awaitAsList()

/**
 * Reconciles this device's database with a remote, in both directions.
 *
 * **Last write wins, per row.** Not CRDT: this is one person's data, and the only genuine
 * concurrent-edit hazard is editing the same transaction on two offline devices — too rare to
 * repay the permanent complexity tax CRDTs put on every table. Not field-level merge either, which
 * produces incoherent rows: one device's amount married to another's category.
 *
 * **Delete beats update.** A row that was deleted on one device and edited on another stays
 * deleted. Both outcomes lose information, but a resurrected transaction is more alarming to
 * someone reading their own ledger than an edit that did not stick, and it is also the one the
 * user cannot easily undo.
 *
 * **Derived values are recomputed, never trusted from the wire.** `BudgetEntity.spent` is a sum
 * over transactions, so each device computes it over whatever transactions it can see. Letting
 * last-write-wins pick between two such sums produces a number that matches neither device's
 * actual data and drifts further every sync — the classic "the totals are wrong after sync" bug.
 * It is recomputed locally after every pull instead.
 */
class SyncEngine(
    private val databaseProvider: DatabaseProvider,
    private val remote: RemoteSyncDataSource,
    private val deviceId: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val adapters = syncAdapters()

    /** One full pass: publish what is ours, take what is theirs, then rebuild derived values. */
    suspend fun sync(): Unit = withContext(dispatcher) {
        push()
        pull()
        recomputeDerived()
    }

    private suspend fun push() {
        val db = databaseProvider.getDatabase()
        val q = db.budgetMasterDatabaseQueries

        val records = adapters.flatMap { adapter ->
            adapter.dirtyRows(db).map { (rowId, updatedAt, payload) ->
                RemoteRecord(adapter.tableName, rowId, updatedAt, deviceId, payload)
            }
        }
        val tombstones = q.selectUnpushedTombstones().awaitAsList().map {
            RemoteTombstone(it.tableName, it.rowId, it.deletedAt, deviceId)
        }

        if (records.isEmpty() && tombstones.isEmpty()) return

        remote.push(records, tombstones)

        // Only after the remote has accepted them. A failed push leaves the rows dirty and the
        // tombstones unpushed, so the next pass retries rather than losing the change.
        adapters.forEach { adapter ->
            records.filter { it.tableName == adapter.tableName }
                .forEach { adapter.markPushed(db, it.rowId) }
        }
        tombstones.forEach { q.markTombstonePushed(it.tableName, it.rowId) }
    }

    private suspend fun pull() {
        val db = databaseProvider.getDatabase()
        val q = db.budgetMasterDatabaseQueries
        val byTable = adapters.associateBy { it.tableName }

        // Removals first, then records. A record carries a timestamp that can legitimately beat a
        // tombstone — that is how re-creating a row after deleting it works — whereas a tombstone
        // applied afterwards would undo a record that had already won on merit.
        val tombstoneSince = q.selectSyncCursor(TOMBSTONE_PULL_CURSOR).awaitAsList().firstOrNull() ?: 0L
        val removals = remote.pullTombstones(tombstoneSince)
        removals.map { it.value }.forEach { tombstone ->
            val adapter = byTable[tombstone.tableName] ?: return@forEach
            val local = adapter.localState(db, tombstone.rowId) ?: return@forEach
            if (beats(tombstone.deletedAt, tombstone.deviceId, local)) {
                deleteLocally(tombstone.tableName, tombstone.rowId)
                // The delete trigger has just written a tombstone stamped with *this* device's
                // clock, which throws away when the removal actually happened. Put the original
                // time back. Otherwise the delete drifts later every time it is relayed, and can
                // end up outranking an edit that genuinely came after it — the same reason an
                // applied record keeps the updatedAt it arrived with instead of being restamped.
                q.insertTombstone(tombstone.tableName, tombstone.rowId, tombstone.deletedAt)
                // The remote is where this came from, so it does not need it back.
                q.markTombstonePushed(tombstone.tableName, tombstone.rowId)
            }
        }
        removals.maxOfOrNull { it.seq }?.let { q.upsertSyncCursor(TOMBSTONE_PULL_CURSOR, it) }

        val since = q.selectSyncCursor(RECORD_CURSOR).awaitAsList().firstOrNull() ?: 0L
        val incoming = remote.pull(since)

        // Applied in adapter order, so a parent row lands before the child that references it.
        adapters.forEach { adapter ->
            incoming.map { it.value }.filter { it.tableName == adapter.tableName }.forEach { record ->
                val local = adapter.localState(db, record.rowId)
                // Absent locally can mean two very different things: never seen, or deleted here.
                // Treating both as "never seen" resurrects rows — the other device publishes the
                // version it held before it learned of the delete, and this device inserts it back.
                val blocking = if (local == null) localTombstone(adapter.tableName, record.rowId) else null
                val apply = when {
                    blocking != null -> beats(record.updatedAt, record.deviceId, blocking, defended = true)
                    local == null -> true
                    else -> beats(record.updatedAt, record.deviceId, local)
                }
                if (apply) {
                    adapter.applyRemote(db, record.payload, record.updatedAt, exists = local != null)
                    // The row is alive here again, so the tombstone is no longer true. Left behind
                    // it would be pushed again later and kill the row a second time.
                    if (blocking != null) q.deleteTombstone(adapter.tableName, record.rowId)
                }
            }
        }
        // The remote's sequence, never the records' edit times. See RemoteChange.
        incoming.maxOfOrNull { it.seq }?.let { q.upsertSyncCursor(RECORD_CURSOR, it) }
    }

    /** When this device recorded the row's removal, if it did. */
    private suspend fun localTombstone(tableName: String, rowId: String): Long? =
        databaseProvider.getDatabase().budgetMasterDatabaseQueries
            .selectTombstoneDeletedAt(tableName, rowId).awaitAsList().firstOrNull()

    /**
     * The one ordering rule, used for every incoming-versus-local decision there is: edit against
     * edit, edit against delete, delete against edit.
     *
     * Whether something from [theirDeviceId] stamped [theirs] should displace what is here, stamped
     * [ours]. Using a single function for all three cases is the point: when the record path and
     * the tombstone path had rules of their own they disagreed on ties, and the two devices settled
     * on different data with nothing anywhere to indicate it.
     *
     * The timestamps have second resolution, so exact ties are ordinary rather than exotic, and
     * [defended] decides them. It answers "is what is here an unpublished local edit?" — if it is
     * not, this device is merely holding a copy of something someone else published and has nothing
     * to defend, so the incoming change simply wins. Only when both sides really did act in the
     * same tick do the device ids arbitrate, and they do so consistently: the comparison is against
     * *this* device's id, which is sound because the only tie that can arise is mine-versus-theirs,
     * where both devices evaluate the same pair and reach opposite answers — that is, they agree.
     */
    private fun beats(theirs: Long, theirDeviceId: String, ours: Long, defended: Boolean): Boolean = when {
        theirs > ours -> true
        theirs < ours -> false
        // Our own writing, handed straight back: a device always pulls the records it has just
        // pushed, because its cursor necessarily sits behind them. Re-applying them is not merely
        // wasted work — the write would look like an edit to the triggers and re-queue the row,
        // and the device would push the same rows on every pass forever.
        theirDeviceId == deviceId -> false
        !defended -> true
        else -> theirDeviceId > deviceId
    }

    private fun beats(theirs: Long, theirDeviceId: String, local: LocalRowState): Boolean =
        beats(theirs, theirDeviceId, local.updatedAt, defended = local.dirty)

    private suspend fun deleteLocally(tableName: String, rowId: String) {
        val q = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        when (tableName) {
            "UserEntity" -> q.deleteUser(rowId)
            "AccountEntity" -> q.deleteAccount(rowId)
            "CategoryEntity" -> q.deleteCategory(rowId)
            "TransactionEntity" -> q.deleteTransaction(rowId)
            "BudgetEntity" -> q.deleteBudget(rowId)
            "SavingsGoalEntity" -> q.deleteSavingsGoal(rowId)
            "RecurringTransactionEntity" -> q.deleteRecurringTransaction(rowId)
        }
    }

    /**
     * Rebuilds values that are sums of other rows.
     *
     * Runs after every pull, because the transactions a budget sums over may only have arrived a
     * moment ago. The write goes through `updateBudgetSpent`, an ordinary edit — so the row is
     * re-stamped and queued, which is correct: this device now holds the authoritative figure for
     * everything it can see.
     */
    private suspend fun recomputeDerived() {
        val q = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        q.selectAllBudgetsForBackup().awaitAsList().forEach { budget ->
            val spent = q.selectTransactionsByCategory(budget.categoryId).awaitAsList()
                .filter { it.timestamp in budget.startDate..budget.endDate && it.amount < 0 }
                .sumOf { -it.amount }
            if (spent != budget.spent) {
                q.updateBudgetSpent(spent = spent, id = budget.id)
            }
        }
    }

    private companion object {
        // Cursors are stored per purpose in the same table, keyed by a name that is not a real
        // table so it cannot collide with a per-table cursor added later. Both hold a remote
        // sequence, not a timestamp, despite the column being named lastPulledAt.
        const val RECORD_CURSOR = "__records"
        const val TOMBSTONE_PULL_CURSOR = "__tombstones_pulled"
    }
}
