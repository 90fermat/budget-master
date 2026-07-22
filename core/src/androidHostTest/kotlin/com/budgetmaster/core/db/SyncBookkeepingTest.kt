package com.budgetmaster.core.db

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The sync bookkeeping is maintained entirely by triggers, so these tests are the only thing
 * standing between the design and silent data loss on first sync.
 *
 * Run against a real SQLite driver, because triggers are a database behaviour — a fake would
 * prove nothing about whether they fire, least of all through a cascade.
 */
class SyncBookkeepingTest {

    private fun database(): Pair<BudgetMasterDatabase, JdbcSqliteDriver> {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BudgetMasterDatabase.Schema.synchronous().create(driver)
        SyncTriggers.install(driver)
        // Cascades are off by default on this driver; the app's Android driver enables them, and
        // the cascade-tombstone behaviour below is only meaningful with them on.
        driver.execute(null, "PRAGMA foreign_keys=ON", 0)
        return BudgetMasterDatabase(driver) to driver
    }

    private suspend fun seedUserAndAccount(db: BudgetMasterDatabase) {
        val q = db.budgetMasterDatabaseQueries
        q.insertUser("u1", "Cyrille", "c@example.com", "XAF", 0L)
        q.insertAccount("acc1", "u1", "Everyday", "CASH", 0.0, "XAF", 0L, 0, 1)
    }

    private fun BudgetMasterDatabase.accountRow(id: String) =
        budgetMasterDatabaseQueries.selectAccountById(id)

    @Test
    fun `an inserted row is stamped and queued for push`(): Unit = runBlocking {
        val (db, _) = database()
        seedUserAndAccount(db)

        val account = db.accountRow("acc1").awaitAsList().single()
        assertTrue(account.updatedAt > 0, "insert must stamp updatedAt, got ${account.updatedAt}")
        assertEquals(1L, account.dirty, "a locally created row is pending push")
    }

    @Test
    fun `an updated row is restamped and re-queued`(): Unit = runBlocking {
        val (db, driver) = database()
        seedUserAndAccount(db)

        // Pretend it was already pushed, and that time has passed.
        driver.execute(null, "UPDATE AccountEntity SET dirty = 0, updatedAt = 1000 WHERE id = 'acc1'", 0)
        assertEquals(0L, db.accountRow("acc1").awaitAsList().single().dirty)

        db.budgetMasterDatabaseQueries.setAccountArchived(isArchived = 1, id = "acc1")

        val account = db.accountRow("acc1").awaitAsList().single()
        assertEquals(1L, account.dirty, "an ordinary edit must queue the row again")
        assertTrue(account.updatedAt > 1000, "and move updatedAt forward")
    }

    @Test
    fun `a write that sets updatedAt itself is left alone`(): Unit = runBlocking {
        val (db, driver) = database()
        seedUserAndAccount(db)

        // This is what applying a remote change looks like: the row arrives with its own
        // timestamp and is already in sync, so the trigger must not restamp it as a local edit.
        driver.execute(
            null,
            "UPDATE AccountEntity SET name = 'Renamed', updatedAt = 5000, dirty = 0 WHERE id = 'acc1'",
            0,
        )

        val account = db.accountRow("acc1").awaitAsList().single()
        assertEquals(5000L, account.updatedAt, "the remote timestamp must survive")
        assertEquals(0L, account.dirty, "a synced row must not be queued straight back for push")
    }

    @Test
    fun `marking a row pushed actually clears the flag`(): Unit = runBlocking {
        val (db, driver) = database()
        seedUserAndAccount(db)
        assertEquals(1L, db.accountRow("acc1").awaitAsList().single().dirty)

        // Clearing dirty leaves updatedAt untouched. Without the guard covering `dirty` too, the
        // update trigger would fire and set it straight back to 1 - and sync would push the same
        // rows on every pass, forever.
        driver.execute(null, "UPDATE AccountEntity SET dirty = 0 WHERE id = 'acc1'", 0)

        assertEquals(0L, db.accountRow("acc1").awaitAsList().single().dirty, "the flag must stay clear")
    }

    @Test
    fun `deleting a row leaves a tombstone`(): Unit = runBlocking {
        val (db, _) = database()
        seedUserAndAccount(db)

        db.budgetMasterDatabaseQueries.deleteAccount("acc1")

        val tombstones = db.budgetMasterDatabaseQueries.selectUnpushedTombstones().awaitAsList()
        assertTrue(
            tombstones.any { it.tableName == "AccountEntity" && it.rowId == "acc1" },
            "without this, another device would helpfully restore the deleted row",
        )
    }

    @Test
    fun `a cascaded delete tombstones the children too`(): Unit = runBlocking {
        val (db, _) = database()
        seedUserAndAccount(db)
        val q = db.budgetMasterDatabaseQueries
        q.insertTransaction("t1", "acc1", null, -10.0, "Lunch", 1L, null, null, 0, null)

        // Deleting the account cascades to its transactions. The whole design rests on the child
        // trigger firing for rows removed by the cascade rather than by an explicit DELETE.
        q.deleteAccount("acc1")

        val tombstones = q.selectUnpushedTombstones().awaitAsList()
        assertTrue(
            tombstones.any { it.tableName == "TransactionEntity" && it.rowId == "t1" },
            "a cascaded transaction must tombstone itself, or it resurrects on the next pull",
        )
    }

    @Test
    fun `the sync cursor round-trips`(): Unit = runBlocking {
        val (db, _) = database()
        val q = db.budgetMasterDatabaseQueries

        assertTrue(q.selectSyncCursor("TransactionEntity").awaitAsList().isEmpty())
        q.upsertSyncCursor("TransactionEntity", 1_234L)
        assertEquals(1_234L, q.selectSyncCursor("TransactionEntity").awaitAsList().single())

        // Upsert, not insert: a second sync must move the cursor rather than fail on the key.
        q.upsertSyncCursor("TransactionEntity", 5_678L)
        assertEquals(5_678L, q.selectSyncCursor("TransactionEntity").awaitAsList().single())
    }

    @Test
    fun `purging drops only tombstones older than the cutoff`(): Unit = runBlocking {
        val (db, _) = database()
        val q = db.budgetMasterDatabaseQueries
        q.insertTombstone("TransactionEntity", "old", 100L)
        q.insertTombstone("TransactionEntity", "recent", 9_000L)

        q.purgeTombstonesBefore(1_000L)

        val left = q.selectUnpushedTombstones().awaitAsList()
        assertEquals(1, left.size)
        assertEquals("recent", left.single().rowId)
    }
}
