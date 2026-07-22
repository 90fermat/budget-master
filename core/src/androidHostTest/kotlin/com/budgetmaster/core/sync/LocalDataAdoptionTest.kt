package com.budgetmaster.core.sync

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.budgetmaster.core.db.BudgetMasterDatabase
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.db.DefaultData
import com.budgetmaster.core.db.SyncTriggers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val UID = "signed-in-uid"
private val ANON = DefaultData.DEFAULT_USER_ID
private val SEED_ACCOUNT = DefaultData.firstAccountId(ANON)

private class EmptyRemote : RemoteSyncDataSource {
    override suspend fun hasAnyRecords() = false
    override suspend fun pull(sinceSeq: Long): List<RemoteChange<RemoteRecord>> = emptyList()
    override suspend fun pullTombstones(sinceSeq: Long): List<RemoteChange<RemoteTombstone>> = emptyList()
    override suspend fun push(records: List<RemoteRecord>, tombstones: List<RemoteTombstone>) = Unit
}

private class PopulatedRemote : RemoteSyncDataSource {
    override suspend fun hasAnyRecords() = true
    override suspend fun pull(sinceSeq: Long) = listOf(
        RemoteChange(RemoteRecord("AccountEntity", "cloud-acc", 1L, "other-device", "{}"), 1L),
    )
    override suspend fun pullTombstones(sinceSeq: Long): List<RemoteChange<RemoteTombstone>> = emptyList()
    override suspend fun push(records: List<RemoteRecord>, tombstones: List<RemoteTombstone>) = Unit
}

class LocalDataAdoptionTest {

    private fun database(): DatabaseProvider {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
            BudgetMasterDatabase.Schema.synchronous().create(it)
            SyncTriggers.install(it)
            it.execute(null, "PRAGMA foreign_keys=ON", 0)
        }
        return DatabaseProvider(BudgetMasterDatabase(driver))
    }

    /**
     * First launch while signed out: a user row and the one starter wallet, nothing typed in.
     *
     * The signed-in user's row is written too, because that is the real order of events — the
     * seeder creates it at sign-in, before adoption re-parents anything onto it.
     */
    private suspend fun DatabaseProvider.seedAnonymous() {
        val q = getDatabase().budgetMasterDatabaseQueries
        q.insertUser(ANON, "Guest", "guest@local", "XAF", 0L)
        q.insertUser(UID, "Cyrille", "cyrille@example.com", "XAF", 0L)
        q.insertAccount(SEED_ACCOUNT, ANON, "Cash", "CASH", 0.0, "XAF", 0L, 0, 1)
    }

    private suspend fun DatabaseProvider.accountsFor(userId: String) =
        getDatabase().budgetMasterDatabaseQueries.selectAccountsByUserId(userId).awaitAsList()

    private suspend fun DatabaseProvider.allTransactions() =
        getDatabase().budgetMasterDatabaseQueries.selectAllTransactions().awaitAsList()

    private suspend fun DatabaseProvider.tombstones() =
        getDatabase().budgetMasterDatabaseQueries.selectAllTombstones().awaitAsList()

    @Test
    fun `a seeded wallet alone is not treated as the user's data`() = runTest {
        val db = database().apply { seedAnonymous() }

        // If this counted as real data, every ordinary first sign-in would interrogate the user
        // about a wallet they never touched.
        assertEquals(AdoptionPlan.AdoptCloud, LocalDataAdoption(db).plan(UID, PopulatedRemote()))
    }

    @Test
    fun `a single transaction makes it the user's data`() = runTest {
        val db = database().apply {
            seedAnonymous()
            getDatabase().budgetMasterDatabaseQueries
                .insertTransaction("t1", SEED_ACCOUNT, null, -500.0, "Lunch", 10L, null, null, 0, null)
        }

        assertEquals(AdoptionPlan.AskUser, LocalDataAdoption(db).plan(UID, PopulatedRemote()))
    }

    @Test
    fun `real local data and an empty cloud is adopted without asking`() = runTest {
        val db = database().apply {
            seedAnonymous()
            getDatabase().budgetMasterDatabaseQueries
                .insertTransaction("t1", SEED_ACCOUNT, null, -500.0, "Lunch", 10L, null, null, 0, null)
        }
        val adoption = LocalDataAdoption(db)

        val plan = adoption.plan(UID, EmptyRemote())
        adoption.apply(UID, plan)

        assertEquals(AdoptionPlan.AdoptLocal, plan)
        assertEquals(1, db.accountsFor(UID).size, "the wallet must now belong to the signed-in user")
        assertTrue(db.accountsFor(ANON).isEmpty())
        assertEquals(1, db.allTransactions().size, "the ledger travels with its account")
    }

    @Test
    fun `adopting the cloud leaves no tombstones behind`() = runTest {
        // The trap. Local rows were never synced, so there is nothing to retract — but their ids
        // are shared by every device that was ever signed out. A tombstone for `default_user_cash`
        // would travel up and delete another device's wallet, during the one operation the user
        // chose specifically to keep the cloud's data intact.
        val db = database().apply { seedAnonymous() }
        val adoption = LocalDataAdoption(db)

        adoption.apply(UID, AdoptionPlan.AdoptCloud)

        assertTrue(db.accountsFor(ANON).isEmpty(), "the local rows must go")
        assertTrue(db.tombstones().isEmpty(), "but their removal must not be announced to anyone")
    }

    @Test
    fun `merging re-keys local wallets so they cannot collide`() = runTest {
        val db = database().apply {
            seedAnonymous()
            getDatabase().budgetMasterDatabaseQueries
                .insertTransaction("t1", SEED_ACCOUNT, null, -500.0, "Lunch", 10L, null, null, 0, null)
        }
        val adoption = LocalDataAdoption(db, newId = { "fresh-id" })

        adoption.apply(UID, AdoptionPlan.AskUser, AdoptionChoice.Merge)

        val accounts = db.accountsFor(UID)
        assertEquals(1, accounts.size)
        assertEquals("fresh-id", accounts.single().id, "a deterministic id would collide on the remote")
        assertEquals(
            "fresh-id",
            db.allTransactions().single().accountId,
            "the ledger must follow its wallet to the new id",
        )
        assertTrue(db.tombstones().isEmpty(), "re-keying must not read as a deletion to other devices")
    }

    @Test
    fun `choosing the cloud discards local data`() = runTest {
        val db = database().apply {
            seedAnonymous()
            getDatabase().budgetMasterDatabaseQueries
                .insertTransaction("t1", SEED_ACCOUNT, null, -500.0, "Lunch", 10L, null, null, 0, null)
        }

        LocalDataAdoption(db).apply(UID, AdoptionPlan.AskUser, AdoptionChoice.CloudOnly)

        assertTrue(db.accountsFor(ANON).isEmpty())
        assertTrue(db.accountsFor(UID).isEmpty())
        assertTrue(db.allTransactions().isEmpty(), "the accounts cascade to their transactions")
    }

    @Test
    fun `choosing this device keeps local data under the new owner`() = runTest {
        val db = database().apply {
            seedAnonymous()
            getDatabase().budgetMasterDatabaseQueries
                .insertTransaction("t1", SEED_ACCOUNT, null, -500.0, "Lunch", 10L, null, null, 0, null)
        }

        LocalDataAdoption(db).apply(UID, AdoptionPlan.AskUser, AdoptionChoice.ThisDeviceOnly)

        assertEquals(1, db.accountsFor(UID).size)
        assertEquals(1, db.allTransactions().size)
    }

    @Test
    fun `shared default categories are never re-parented`() = runTest {
        val db = database().apply {
            seedAnonymous()
            getDatabase().budgetMasterDatabaseQueries
                .insertCategory("cat_food", ANON, "Food", "F", "#fff", 1)
        }

        LocalDataAdoption(db).apply(UID, AdoptionPlan.AdoptLocal)

        // They belong to the system user and are shown to everyone. Moving them would make one
        // account the owner of every user's category list.
        val categories = db.getDatabase().budgetMasterDatabaseQueries
            .selectCategoriesByUserId(UID).awaitAsList()
        assertEquals(ANON, categories.single { it.id == "cat_food" }.userId)
    }

    @Test
    fun `a user's own categories, budgets and goals do move`() = runTest {
        val db = database().apply {
            seedAnonymous()
            val q = getDatabase().budgetMasterDatabaseQueries
            q.insertCategory("mine", ANON, "Tontine", "T", "#000", 0)
            q.insertBudget("b1", ANON, "mine", 10_000.0, 0.0, 0L, Long.MAX_VALUE)
            q.insertSavingsGoal("g1", ANON, "Roof", 500_000.0, 0.0, 0L, 0L)
        }

        LocalDataAdoption(db).apply(UID, AdoptionPlan.AdoptLocal)

        val q = db.getDatabase().budgetMasterDatabaseQueries
        assertEquals(UID, q.selectCategoriesByUserId(UID).awaitAsList().single { it.id == "mine" }.userId)
        assertEquals(1, q.selectBudgetsByUserId(UID, Long.MAX_VALUE, 0L).awaitAsList().size)
        assertEquals(1, q.selectSavingsGoalsByUserId(UID).awaitAsList().size)
    }

    @Test
    fun `nothing anywhere simply hands the starter wallet over`() = runTest {
        val db = database().apply { seedAnonymous() }
        val adoption = LocalDataAdoption(db)

        val plan = adoption.plan(UID, EmptyRemote())
        adoption.apply(UID, plan)

        assertEquals(AdoptionPlan.Nothing, plan)
        assertEquals(1, db.accountsFor(UID).size)
        assertFalse(db.accountsFor(UID).single().id == "", "the wallet is kept, not recreated")
    }
}
