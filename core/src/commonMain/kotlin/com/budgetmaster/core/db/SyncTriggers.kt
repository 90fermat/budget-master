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
 *   arrived with instead of being restamped as a local edit.
 * - **update** does the same, guarded on `updatedAt` being unchanged — an ordinary edit leaves it
 *   alone and so gets stamped and queued; a sync-applied write sets it and is left in peace.
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

    /** Epoch milliseconds at second resolution, which is ample for last-write-wins ordering. */
    private const val NOW = "(CAST(strftime('%s','now') AS INTEGER) * 1000)"

    /** Every statement needed, in order. */
    fun statements(): List<String> = SYNCED_TABLES.flatMap { table ->
        listOf(
            """
            CREATE TRIGGER IF NOT EXISTS ${table}_sync_insert AFTER INSERT ON $table
            WHEN NEW.updatedAt = 0
            BEGIN
                UPDATE $table SET updatedAt = $NOW, dirty = 1 WHERE id = NEW.id;
            END
            """.trimIndent(),
            """
            CREATE TRIGGER IF NOT EXISTS ${table}_sync_update AFTER UPDATE ON $table
            WHEN NEW.updatedAt = OLD.updatedAt
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

    /** Installs the triggers. Safe to call repeatedly. */
    fun install(driver: SqlDriver) {
        statements().forEach { driver.execute(null, it, 0) }
    }
}
