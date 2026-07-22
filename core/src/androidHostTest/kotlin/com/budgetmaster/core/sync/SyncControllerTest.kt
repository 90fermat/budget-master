package com.budgetmaster.core.sync

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.budgetmaster.core.db.BudgetMasterDatabase
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.db.SyncTriggers
import com.budgetmaster.core.prefs.KeyValueStore
import com.budgetmaster.core.session.SessionStore
import com.budgetmaster.core.session.SessionUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class StubStore : KeyValueStore {
    private val values = MutableStateFlow(emptyMap<String, String>())
    override fun observeString(key: String): Flow<String?> = values.map { it[key] }
    override suspend fun putString(key: String, value: String) {
        values.value = values.value + (key to value)
    }
    override suspend fun remove(key: String) {
        values.value = values.value - key
    }
}

private class RecordingRemote(private val failWith: Exception? = null) : RemoteSyncDataSource {
    var pushes = 0
    override suspend fun pull(sinceSeq: Long): List<RemoteChange<RemoteRecord>> {
        failWith?.let { throw it }
        return emptyList()
    }
    override suspend fun pullTombstones(sinceSeq: Long): List<RemoteChange<RemoteTombstone>> = emptyList()
    override suspend fun hasAnyRecords() = false
    override suspend fun push(records: List<RemoteRecord>, tombstones: List<RemoteTombstone>) {
        failWith?.let { throw it }
        pushes++
    }
}

class SyncControllerTest {

    private fun database(): DatabaseProvider {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
            BudgetMasterDatabase.Schema.synchronous().create(it)
            SyncTriggers.install(it)
        }
        return DatabaseProvider(BudgetMasterDatabase(driver))
    }

    private fun controller(
        session: SessionStore,
        remote: (String) -> RemoteSyncDataSource?,
    ) = SyncController(
        databaseProvider = database(),
        sessionStore = session,
        deviceIdProvider = DeviceIdProvider(StubStore()),
        scope = TestScope(UnconfinedTestDispatcher()),
        remoteFactory = remote,
        dispatcher = UnconfinedTestDispatcher(),
        now = { 1_000L },
    )

    private fun signedIn() = SessionStore().apply {
        setCurrentUser(SessionUser("uid-1", "Cyrille", "c@example.com"))
    }

    @Test
    fun `reports signed out rather than pretending to sync`() = runTest {
        val controller = controller(SessionStore()) { error("must not be asked for a remote") }

        assertFalse(controller.sync())
        assertEquals(SyncStatus.SignedOut, controller.status.value)
    }

    @Test
    fun `reports unsupported where there is no remote`() = runTest {
        // The web build. Saying "unsupported" is the honest answer; reporting success would be a
        // lie the user could act on, and reporting failure would invite a pointless retry.
        val controller = controller(signedIn()) { null }

        assertFalse(controller.sync())
        assertEquals(SyncStatus.Unsupported, controller.status.value)
    }

    @Test
    fun `records when the last successful pass happened`() = runTest {
        val database = database()
        val remote = RecordingRemote()
        val controller = SyncController(
            databaseProvider = database,
            sessionStore = signedIn(),
            deviceIdProvider = DeviceIdProvider(StubStore()),
            scope = TestScope(UnconfinedTestDispatcher()),
            remoteFactory = { remote },
            dispatcher = UnconfinedTestDispatcher(),
            now = { 1_000L },
        )
        // Something to send, or the engine short-circuits and the pass proves nothing.
        database.getDatabase().budgetMasterDatabaseQueries
            .insertUser("uid-1", "Cyrille", "c@example.com", "XAF", 0L)

        assertTrue(controller.sync())
        assertEquals(SyncStatus.Synced(1_000L), controller.status.value)
        assertTrue(remote.pushes > 0)
    }

    @Test
    fun `a failure is reported, not thrown`() = runTest {
        // A phone without signal is the normal case, not an incident. The local database is
        // complete on its own, so nothing the user is doing should stop because a pass did not land.
        val controller = controller(signedIn()) { RecordingRemote(IllegalStateException("no network")) }

        assertFalse(controller.sync())
        val status = controller.status.value
        assertIs<SyncStatus.Failed>(status)
        assertEquals("no network", status.reason)
    }

    @Test
    fun `a remote that never answers ends as a failure, not a permanent spinner`() = runTest {
        // The real failure mode. Firestore resolves a write only when the server acknowledges it,
        // so a rejected or undeliverable request leaves the call suspended rather than throwing.
        // Without a ceiling the pass never ends, the lock is never released, and every later
        // attempt — including the user pressing "Sync now" — queues behind it on a spinner that
        // can never resolve. That is what a device reported.
        val silent = object : RemoteSyncDataSource {
            override suspend fun pull(sinceSeq: Long): List<RemoteChange<RemoteRecord>> {
                awaitCancellation()
            }
            override suspend fun pullTombstones(sinceSeq: Long): List<RemoteChange<RemoteTombstone>> = emptyList()
            override suspend fun hasAnyRecords() = false
            override suspend fun push(records: List<RemoteRecord>, tombstones: List<RemoteTombstone>) {
                awaitCancellation()
            }
        }
        val database = database()
        val controller = SyncController(
            databaseProvider = database,
            sessionStore = signedIn(),
            deviceIdProvider = DeviceIdProvider(StubStore()),
            scope = TestScope(UnconfinedTestDispatcher()),
            remoteFactory = { silent },
            dispatcher = UnconfinedTestDispatcher(),
            now = { 1_000L },
        )
        database.getDatabase().budgetMasterDatabaseQueries
            .insertUser("uid-1", "Cyrille", "c@example.com", "XAF", 0L)

        assertFalse(controller.sync())

        assertIs<SyncStatus.Failed>(controller.status.value)
        // And the lock is free, so the next attempt is not stuck behind the abandoned one.
        assertFalse(controller.sync())
    }

    @Test
    fun `a failed pass leaves the work queued for the next one`() = runTest {
        val database = database()
        val session = signedIn()
        val failing = RecordingRemote(IllegalStateException("offline"))
        val flaky = object : (String) -> RemoteSyncDataSource? {
            var remote: RemoteSyncDataSource = failing
            override fun invoke(uid: String) = remote
        }
        val controller = SyncController(
            databaseProvider = database,
            sessionStore = session,
            deviceIdProvider = DeviceIdProvider(StubStore()),
            scope = TestScope(UnconfinedTestDispatcher()),
            remoteFactory = flaky,
            dispatcher = UnconfinedTestDispatcher(),
            now = { 1_000L },
        )
        val q = database.getDatabase().budgetMasterDatabaseQueries
        q.insertUser("uid-1", "Cyrille", "c@example.com", "XAF", 0L)
        q.insertAccount("acc1", "uid-1", "Everyday", "CASH", 0.0, "XAF", 0L, 0, 1)

        assertFalse(controller.sync())

        // Nothing was marked pushed, so the retry carries the same rows rather than losing them.
        val healthy = RecordingRemote()
        flaky.remote = healthy
        assertTrue(controller.sync())
        assertTrue(healthy.pushes > 0, "the rows a failed push left dirty must go out on the next pass")
    }
}
