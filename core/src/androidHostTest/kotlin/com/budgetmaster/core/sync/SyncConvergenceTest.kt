package com.budgetmaster.core.sync

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.budgetmaster.core.db.BudgetMasterDatabase
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.db.SyncTriggers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * An in-memory stand-in for the remote, holding the newest record per (table, row).
 *
 * Deliberately dumb: it stores and returns, and makes no decisions. Every ordering and conflict
 * rule lives in [SyncEngine], so these tests exercise the real algorithm rather than a fake's
 * opinion of it — which is the entire reason the remote sits behind an interface.
 */
private class FakeRemote : RemoteSyncDataSource {
    private var nextSeq = 0L
    val records = mutableMapOf<Pair<String, String>, RemoteChange<RemoteRecord>>()
    val tombstones = mutableMapOf<Pair<String, String>, RemoteChange<RemoteTombstone>>()

    override suspend fun pull(sinceSeq: Long) =
        records.values.filter { it.seq > sinceSeq }.sortedBy { it.seq }

    override suspend fun pullTombstones(sinceSeq: Long) =
        tombstones.values.filter { it.seq > sinceSeq }.sortedBy { it.seq }

    override suspend fun push(records: List<RemoteRecord>, tombstones: List<RemoteTombstone>) {
        records.forEach { record ->
            val key = record.tableName to record.rowId
            val existing = this.records[key]
            // Keep the newer of the two, as a real backend's write rules would. A write that is
            // accepted gets a fresh sequence, so it is visible to everyone who has not pulled yet
            // however old the edit itself is.
            if (existing == null || record.updatedAt >= existing.value.updatedAt) {
                this.records[key] = RemoteChange(record, ++nextSeq)
            }
        }
        tombstones.forEach {
            // Per the contract: the record goes with the removal, but only when the removal is not
            // older than it. A delete that arrives late must not evict a newer re-creation.
            val held = this.records[it.tableName to it.rowId]
            if (held != null && it.deletedAt >= held.value.updatedAt) {
                this.records.remove(it.tableName to it.rowId)
            }
            this.tombstones[it.tableName to it.rowId] = RemoteChange(it, ++nextSeq)
        }
    }
}

/**
 * Two reference points for test-authored edit times.
 *
 * The triggers stamp rows with real epoch milliseconds, so a test cannot just use small numbers:
 * an edit dated 1970 loses every comparison. [LATER] sits ahead of the wall clock for edits that
 * must outrank anything already in the database; [EARLIER] sits behind it for an edit that must
 * read as concurrent with a delete rather than as a deliberate re-creation after one.
 */
private val LATER = System.currentTimeMillis() + 3_600_000L
private val EARLIER = System.currentTimeMillis() - 3_600_000L

/** One simulated device: its own database, its own engine, sharing the one remote. */
private class Device(val id: String, remote: FakeRemote) {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
        BudgetMasterDatabase.Schema.synchronous().create(it)
        SyncTriggers.install(it)
        it.execute(null, "PRAGMA foreign_keys=ON", 0)
    }
    val db = BudgetMasterDatabase(driver)
    val engine = SyncEngine(DatabaseProvider(db), remote, id, Dispatchers.Unconfined)

    val q get() = db.budgetMasterDatabaseQueries

    suspend fun sync() = engine.sync()

    /** Sets a row's edit time directly, so a test can order events without sleeping. */
    fun stamp(table: String, id: String, updatedAt: Long) {
        driver.execute(null, "UPDATE $table SET updatedAt = $updatedAt WHERE id = '$id'", 0)
    }

    /**
     * Sets a removal's time directly, the counterpart to [stamp].
     *
     * A test that stamps edits but leaves deletes on the wall clock is not ordering its own
     * events — it is comparing two different clocks, and the outcome then depends on how fast the
     * machine happened to run.
     */
    fun stampTombstone(table: String, id: String, deletedAt: Long) {
        driver.execute(
            null,
            "UPDATE SyncTombstone SET deletedAt = $deletedAt WHERE tableName = '$table' AND rowId = '$id'",
            0,
        )
    }

    suspend fun accounts() = q.selectAllAccountsForBackup().awaitAsList()
    suspend fun transactions() = q.selectAllTransactions().awaitAsList()

    suspend fun tombstoneFor(rowId: String) =
        q.selectTombstoneDeletedAt("TransactionEntity", rowId).awaitAsList().firstOrNull()
}

class SyncConvergenceTest {

    private suspend fun Device.seedBase() {
        q.insertUser("u1", "Cyrille", "c@example.com", "XAF", 0L)
        q.insertAccount("acc1", "u1", "Everyday", "CASH", 0.0, "XAF", 0L, 0, 1)
    }

    @Test
    fun `a row created on one device reaches the other`(): Unit = runBlocking {
        val remote = FakeRemote()
        val a = Device("A", remote)
        val b = Device("B", remote)

        a.seedBase()
        a.q.insertTransaction("t1", "acc1", null, -1_000.0, "Lunch", 10L, null, null, 0, null)
        a.sync()
        b.sync()

        assertEquals(1, b.transactions().size)
        assertEquals("Lunch", b.transactions().single().description)
    }

    @Test
    fun `the later edit wins, whichever device made it`(): Unit = runBlocking {
        val remote = FakeRemote()
        val a = Device("A", remote)
        val b = Device("B", remote)
        a.seedBase()
        a.sync()
        b.sync()

        // Both rename the same wallet while offline; B's edit is the later one.
        a.q.updateAccount("Renamed by A", "CASH", 0.0, "XAF", "acc1")
        a.stamp("AccountEntity", "acc1", LATER + 1_000L)
        b.q.updateAccount("Renamed by B", "CASH", 0.0, "XAF", "acc1")
        b.stamp("AccountEntity", "acc1", LATER + 2_000L)

        a.sync()
        b.sync()
        a.sync()

        assertEquals("Renamed by B", a.accounts().single { it.id == "acc1" }.name)
        assertEquals("Renamed by B", b.accounts().single { it.id == "acc1" }.name)
    }

    @Test
    fun `a delete on one device removes the row on the other`(): Unit = runBlocking {
        val remote = FakeRemote()
        val a = Device("A", remote)
        val b = Device("B", remote)
        a.seedBase()
        a.q.insertTransaction("t1", "acc1", null, -1_000.0, "Lunch", 10L, null, null, 0, null)
        a.sync()
        b.sync()
        assertEquals(1, b.transactions().size)

        a.q.deleteTransaction("t1")
        a.sync()
        b.sync()

        assertTrue(b.transactions().isEmpty(), "a deleted row must not survive on the other device")
    }

    @Test
    fun `a deleted row does not come back when the other device had edited it`(): Unit = runBlocking {
        val remote = FakeRemote()
        val a = Device("A", remote)
        val b = Device("B", remote)
        a.seedBase()
        a.q.insertTransaction("t1", "acc1", null, -1_000.0, "Lunch", 10L, null, null, 0, null)
        a.sync()
        b.sync()

        // B edits it while A deletes it — the classic resurrection case.
        b.q.insertTransaction("t1", "acc1", null, -2_000.0, "Lunch, edited", 10L, null, null, 0, null)
        b.stamp("TransactionEntity", "t1", EARLIER)
        a.q.deleteTransaction("t1")

        a.sync()
        b.sync()
        a.sync()
        b.sync()

        assertTrue(a.transactions().isEmpty(), "delete must beat the concurrent edit")
        assertTrue(b.transactions().isEmpty(), "and must not reappear on the device that edited it")
    }

    @Test
    fun `two edits in the same instant still land on one answer`(): Unit = runBlocking {
        // Timestamps are milliseconds now, so an exact tie is rare — which is exactly why it needs
        // its own test rather than being left to the randomised run to stumble across. What is
        // asserted is not *which* version wins, but that both devices pick the same one.
        val remote = FakeRemote()
        val a = Device("A", remote)
        val b = Device("B", remote)
        a.seedBase()
        a.sync()
        b.sync()

        a.q.updateAccount("Renamed by A", "CASH", 0.0, "XAF", "acc1")
        a.stamp("AccountEntity", "acc1", LATER)
        b.q.updateAccount("Renamed by B", "CASH", 0.0, "XAF", "acc1")
        b.stamp("AccountEntity", "acc1", LATER)

        repeat(3) { a.sync(); b.sync() }

        assertEquals(
            a.accounts().single { it.id == "acc1" }.name,
            b.accounts().single { it.id == "acc1" }.name,
            "a tie must be broken the same way on both devices",
        )
    }

    @Test
    fun `a row edited before the cursor still arrives`(): Unit = runBlocking {
        // Regression. Pull cursors once advanced on the records' own edit times, which silently
        // stranded any row edited earlier than something already pulled — the ordinary case of a
        // device that was offline while the other kept working. The two devices then disagreed
        // permanently, with nothing anywhere to say so.
        val remote = FakeRemote()
        val a = Device("A", remote)
        val b = Device("B", remote)
        a.seedBase()
        a.sync()
        b.sync()

        // A edits late and syncs, pushing its cursor well forward.
        a.q.insertTransaction("late", "acc1", null, -100.0, "Late", 10L, null, null, 0, null)
        a.stamp("TransactionEntity", "late", LATER + 10_000L)
        a.sync()

        // B has been offline and its edit is older — but it reaches the remote afterwards.
        b.q.insertTransaction("early", "acc1", null, -200.0, "Early", 10L, null, null, 0, null)
        b.stamp("TransactionEntity", "early", LATER + 1_000L)
        b.sync()

        a.sync()

        assertEquals(
            listOf("early", "late"),
            a.transactions().map { it.id }.sorted(),
            "an older edit that arrives later must still be pulled",
        )
    }

    @Test
    fun `a pulled row keeps the timestamp it arrived with`(): Unit = runBlocking {
        // Regression. Applying a remote row once ran an insert *and* an update, so when the insert
        // was the one that landed the update rewrote identical values — which the triggers read as
        // an ordinary edit. Every pulled row was restamped with the receiving device's clock and
        // marked dirty, then republished under a time it never had, corrupting the ordering that
        // last-write-wins depends on.
        val remote = FakeRemote()
        val a = Device("A", remote)
        val b = Device("B", remote)
        a.seedBase()
        a.q.insertTransaction("t1", "acc1", null, -1_000.0, "Lunch", 10L, null, null, 0, null)
        a.stamp("TransactionEntity", "t1", LATER)
        a.sync()
        b.sync()

        val arrived = b.transactions().single()
        assertEquals(LATER, arrived.updatedAt, "a pulled row must keep its own edit time")
        assertEquals(0L, arrived.dirty, "and must not be queued straight back for push")
    }

    @Test
    fun `a relayed delete keeps the time it actually happened`(): Unit = runBlocking {
        // Regression. Applying a remote tombstone left the local delete trigger to stamp it with
        // this device's clock, so a removal drifted later every time it was relayed and could end
        // up outranking an edit that genuinely came after it.
        val remote = FakeRemote()
        val a = Device("A", remote)
        val b = Device("B", remote)
        a.seedBase()
        a.q.insertTransaction("t1", "acc1", null, -1_000.0, "Lunch", 10L, null, null, 0, null)
        a.sync()
        b.sync()

        a.q.deleteTransaction("t1")
        a.stampTombstone("TransactionEntity", "t1", LATER)
        a.sync()
        b.sync()

        assertEquals(
            LATER,
            b.tombstoneFor("t1"),
            "a delete must carry its own time to the devices it reaches",
        )
    }

    @Test
    fun `two devices converge under a randomised sequence of edits`(): Unit = runBlocking {
        // The property that matters: whatever order things happen in, both devices end up holding
        // the same data. Thirty seeds, because one seed explores exactly one interleaving and the
        // defects this caught were each reachable from only some of them — the last one appeared
        // only after the first four had gone green. Seeded rather than random, so a failure is
        // reproducible rather than a mystery.
        (1L..30L).forEach { seed -> converge(seed) }
    }

    private suspend fun converge(seed: Long) {
        val random = Random(seed)
        val remote = FakeRemote()
        val a = Device("A", remote)
        val b = Device("B", remote)
        a.seedBase()
        a.sync()
        b.sync()

        // Edits and deletes are both stamped from one logical clock. Stamping only the edits
        // would leave deletes on the wall clock, so every edit would outrank every delete and the
        // interleavings that actually break convergence would never be reached — and the result
        // would depend on how fast the machine ran.
        var clock = LATER
        repeat(50) { step ->
            val device = if (random.nextBoolean()) a else b
            val id = "t${random.nextInt(8)}"
            clock += 10
            when (random.nextInt(3)) {
                0, 1 -> {
                    device.q.insertTransaction(
                        id, "acc1", null, -random.nextInt(1, 500).toDouble(), "e$step", 10L, null, null, 0, null,
                    )
                    device.stamp("TransactionEntity", id, clock)
                }
                else -> {
                    // Only a delete that actually removed something produces a tombstone. Stamping
                    // unconditionally would rewrite the time of some older, unrelated removal and
                    // invent an ordering the run never had.
                    val existed = device.transactions().any { it.id == id }
                    device.q.deleteTransaction(id)
                    if (existed) device.stampTombstone("TransactionEntity", id, clock)
                }
            }
            if (random.nextInt(3) == 0) {
                a.sync()
                b.sync()
            }
        }

        // Settle: enough passes for every change to travel in both directions.
        repeat(3) { a.sync(); b.sync() }

        val fromA = a.transactions().map { it.id to it.amount }.sortedBy { it.first }
        val fromB = b.transactions().map { it.id to it.amount }.sortedBy { it.first }
        assertEquals(fromA, fromB, "both devices must end up holding the same ledger (seed $seed)")
    }

    @Test
    fun `budget spend is recomputed locally rather than taken from the wire`(): Unit = runBlocking {
        val remote = FakeRemote()
        val a = Device("A", remote)
        val b = Device("B", remote)
        a.seedBase()
        a.q.insertCategory("cat_food", "u1", "Food", "F", "#fff", 1)
        a.q.insertBudget("b1", "u1", "cat_food", 10_000.0, 0.0, 0L, Long.MAX_VALUE)
        a.q.insertTransaction("t1", "acc1", "cat_food", -2_500.0, "Lunch", 100L, null, null, 0, null)
        a.sync()
        b.sync()

        // A derived sum must reflect the transactions the device can actually see, not whatever
        // figure happened to win a last-write-wins race.
        val budget = b.q.selectAllBudgetsForBackup().awaitAsList().single()
        assertEquals(2_500.0, budget.spent)
    }

    @Test
    fun `a second sync with nothing to do changes nothing`(): Unit = runBlocking {
        val remote = FakeRemote()
        val a = Device("A", remote)
        a.seedBase()
        a.q.insertTransaction("t1", "acc1", null, -1_000.0, "Lunch", 10L, null, null, 0, null)
        a.sync()

        val afterFirst = a.transactions().single().updatedAt
        a.sync()
        a.sync()

        // If clearing `dirty` re-triggered a stamp, this would climb on every pass and the device
        // would push the same row forever.
        assertEquals(afterFirst, a.transactions().single().updatedAt, "a no-op sync must be a no-op")
        assertEquals(0L, a.transactions().single().dirty)
    }

    @Test
    fun `a row this device has never seen is inserted rather than skipped`(): Unit = runBlocking {
        val remote = FakeRemote()
        val a = Device("A", remote)
        val b = Device("B", remote)
        a.seedBase()
        a.sync()

        // B has no account row at all; the pull must create it, not treat "no local row" as a loss.
        b.sync()
        assertEquals(1, b.accounts().size)
        assertNull(b.accounts().singleOrNull { it.id != "acc1" })
    }
}
