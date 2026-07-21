package com.budgetmaster.core.db

import app.cash.sqldelight.db.SqlDriver

/**
 * The SQLite triggers that keep sync bookkeeping current without any write path knowing about it.
 *
 * Raw DDL rather than `.sq` statements because SQLDelight's analyzer cannot resolve the `NEW` and
 * `OLD` pseudo-tables inside a trigger body, so the file will not compile with them present. The
 * behaviour is the same; only the analyser is bypassed.
 *
 * Three triggers per synced table:
 *
 * - **insert** stamps `updatedAt` and marks the row dirty, but only when the insert did not supply
 *   an `updatedAt` of its own. That guard is what lets a sync-applied row keep the timestamp it
 *   arrived with instead of being restamped as a local edit. It also drops any tombstone for the
 *   id, because a row and a record of its removal must never both exist: re-creating a deleted row
 *   otherwise leaves the old tombstone behind, and the next sync pushes the row and its own
 *   obituary together — whereupon the remote applies the obituary and the row disappears again.
 * - **update** does the same, guarded on *neither* sync column having been touched. An ordinary
 *   domain edit leaves both alone and so gets stamped and queued. Two writes are exempt: applying
 *   a remote change sets `updatedAt`, and marking a row pushed sets `dirty` — without that second
 *   exemption, clearing the dirty flag would re-trigger and set it straight back, and sync would
 *   push the same rows forever.
 * - **delete** records the row's disappearance in [SyncTombstone], so another device learns the row
 *   is gone rather than helpfully restoring it.
 *
 * Deletes stay hard. Verified against SQLite rather than assumed: a delete trigger on a child table
 * fires even when the row goes through `ON DELETE CASCADE`, so cascaded deletes tombstone
 * themselves and no repository has to walk the graph by hand.
 *
 * Every statement is `IF NOT EXISTS` and installing is idempotent, so this can run on every open.
 */
object SyncTriggers {

    /** Tables whose rows are pushed to, and pulled from, a remote. */
    val SYNCED_TABLES = listOf(
        "UserEntity",
        "AccountEntity",
        "CategoryEntity",
        "TransactionEntity",
        "BudgetEntity",
        "SavingsGoalEntity",
        "RecurringTransactionEntity",
    )

    /**
     * Epoch milliseconds, at millisecond resolution.
     *
     * `strftime('%s')` would be simpler but gives whole seconds, and multiplying by 1000 does not
     * put the precision back. Everything a person does in one burst — save a transaction, correct
     * it, delete it — then carries an identical timestamp, so last-write-wins has nothing to order
     * by and falls through to its tiebreak constantly. The tiebreak is correct, but it is a
     * coin-toss standing in for an ordering that genuinely existed and was thrown away.
     */
    private const val NOW = "(CAST((julianday('now') - 2440587.5) * 86400000.0 AS INTEGER))"

    /** Every statement needed, in order. */
    fun statements(): List<String> = SYNCED_TABLES.flatMap { table ->
        listOf(
            """
            CREATE TRIGGER IF NOT EXISTS ${table}_sync_insert AFTER INSERT ON $table
            WHEN NEW.updatedAt = 0
            BEGIN
                UPDATE $table SET updatedAt = $NOW, dirty = 1 WHERE id = NEW.id;
                DELETE FROM SyncTombstone WHERE tableName = '$table' AND rowId = NEW.id;
            END
            """.trimIndent(),
            """
            CREATE TRIGGER IF NOT EXISTS ${table}_sync_update AFTER UPDATE ON $table
            WHEN NEW.updatedAt = OLD.updatedAt AND NEW.dirty = OLD.dirty
            BEGIN
                UPDATE $table SET updatedAt = $NOW, dirty = 1 WHERE id = NEW.id;
            END
            """.trimIndent(),
            """
            CREATE TRIGGER IF NOT EXISTS ${table}_sync_delete AFTER DELETE ON $table
            BEGIN
                INSERT OR REPLACE INTO SyncTombstone (tableName, rowId, deletedAt)
                VALUES ('$table', OLD.id, $NOW);
            END
            """.trimIndent(),
        )
    }

    /** The trigger names this object owns, in the order [statements] creates them. */
    private fun names(): List<String> = SYNCED_TABLES.flatMap {
        listOf("${it}_sync_insert", "${it}_sync_update", "${it}_sync_delete")
    }

    /**
     * Installs the triggers, replacing any already there. Safe to call repeatedly, and called on
     * every database open.
     *
     * Dropping first is what makes that true. `CREATE TRIGGER IF NOT EXISTS` alone would quietly
     * skip every device that had already run an older build, leaving it on the previous definition
     * for good — the trigger bodies would then be whatever shipped first rather than whatever the
     * code says, which is a difference that no test on a fresh database can detect.
     */
    fun install(driver: SqlDriver) {
        names().forEach { driver.execute(null, "DROP TRIGGER IF EXISTS $it", 0) }
        statements().forEach { driver.execute(null, it, 0) }
    }
}
