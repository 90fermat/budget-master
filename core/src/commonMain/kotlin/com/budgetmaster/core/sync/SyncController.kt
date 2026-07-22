@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.core.sync

import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.session.SessionStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** What sync is doing, and what it last did. */
sealed interface SyncStatus {
    /** Nothing has been attempted yet in this session. */
    data object Idle : SyncStatus

    data object Syncing : SyncStatus

    /** @property at epoch milliseconds of the last successful pass. */
    data class Synced(val at: Long) : SyncStatus

    /**
     * The last attempt failed.
     *
     * @property reason the underlying message, kept for display. Sync failing is ordinary — no
     *   network, no signal on a bus — so this is information rather than an error state to dramatise.
     */
    data class Failed(val reason: String?) : SyncStatus

    /** Nobody is signed in, so there is nowhere to sync to. */
    data object SignedOut : SyncStatus

    /** This platform has no durable local database — see [createRemoteSyncDataSource]. */
    data object Unsupported : SyncStatus
}

/**
 * Owns when sync runs and what the UI is told about it.
 *
 * One per process, because two passes running at once would have each pushing rows the other is
 * still reconciling; [mutex] makes a second request wait rather than interleave.
 *
 * Failures are swallowed into [SyncStatus.Failed] rather than thrown. A sync that cannot reach the
 * network is the normal condition of a phone, not an incident, and the local database is complete
 * on its own — nothing the user is doing needs to stop because a background pass did not land.
 */
class SyncController(
    private val databaseProvider: DatabaseProvider,
    private val sessionStore: SessionStore,
    private val deviceIdProvider: DeviceIdProvider,
    private val scope: CoroutineScope,
    private val remoteFactory: (String) -> RemoteSyncDataSource? = ::createRemoteSyncDataSource,
    // Passed through to the engine rather than left to its default, so a test measuring the
    // timeout and the work it is timing share one clock. With the engine on a real dispatcher and
    // the timeout on a virtual one, the timeout always wins instantly and proves nothing.
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val mutex = Mutex()

    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    /** Runs a pass in the background. Safe to call from anywhere, including UI callbacks. */
    fun requestSync() {
        scope.launch { sync() }
    }

    /**
     * Runs one full pass, waiting for any pass already in flight.
     *
     * @return true when data was reconciled — false when there was nobody to sync for, no remote,
     *   or the attempt failed. Callers that show a result need to distinguish those.
     */
    suspend fun sync(): Boolean = mutex.withLock {
        val uid = sessionStore.currentUserId.value
        if (uid == null) {
            _status.value = SyncStatus.SignedOut
            return@withLock false
        }
        val remote = remoteFactory(uid)
        if (remote == null) {
            _status.value = SyncStatus.Unsupported
            return@withLock false
        }

        _status.value = SyncStatus.Syncing
        return@withLock try {
            // A hard ceiling, because the failure mode here is not an exception but silence.
            // Firestore resolves a write only when the server acknowledges it, so a request that
            // is rejected upstream, or a device that cannot reach the backend, leaves the call
            // suspended rather than throwing. Without this the pass never ends, the mutex is never
            // released, and every later attempt - including the user pressing "Sync now" - waits
            // behind it forever on a spinner that cannot resolve.
            val finished = withTimeoutOrNull(TIMEOUT_MILLIS) {
                SyncEngine(databaseProvider, remote, deviceIdProvider.deviceId(), dispatcher).sync()
                true
            } ?: false
            if (!finished) {
                _status.value = SyncStatus.Failed("timed out after ${TIMEOUT_MILLIS / 1000}s")
                return@withLock false
            }
            _status.value = SyncStatus.Synced(now())
            true
        } catch (failure: Exception) {
            // Deliberately broad. Everything below here is network and backend behaviour, and the
            // honest local response to all of it is identical: keep the local database, say so,
            // and try again next time. Rows stay dirty and tombstones unpushed, so nothing is lost
            // by failing — the next pass picks up exactly where this one stopped.
            _status.value = SyncStatus.Failed(failure.message ?: failure::class.simpleName)
            false
        }
    }

    private companion object {
        /**
         * Long enough for a slow connection and a large first push, short enough that a user
         * watching the spinner gets an answer rather than a permanent "syncing".
         */
        const val TIMEOUT_MILLIS = 90_000L
    }
}
