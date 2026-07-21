@file:OptIn(ExperimentalUuidApi::class)

package com.budgetmaster.core.sync

import com.budgetmaster.core.prefs.KeyValueStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * This device's identity for sync, generated once and kept.
 *
 * Deliberately *not* in the database. It is a property of the phone rather than of the user's data,
 * and the database is what backup and restore copy around: a restored backup that carried the
 * device id would give two phones the same identity, which is precisely the thing the id exists to
 * distinguish. Keeping it in [KeyValueStore] means a restore leaves it alone.
 *
 * It is not a secret and not a fingerprint — a random value with no device characteristics in it,
 * used only to break exact timestamp ties consistently. It never leaves the user's own documents.
 */
class DeviceIdProvider(private val store: KeyValueStore) {

    private val mutex = Mutex()
    private var cached: String? = null

    /** The stable id for this install, minting one on first call. */
    suspend fun deviceId(): String = cached ?: mutex.withLock {
        cached ?: run {
            val existing = store.observeString(KEY).first()
            val id = existing ?: Uuid.random().toString().also { store.putString(KEY, it) }
            cached = id
            id
        }
    }

    private companion object {
        const val KEY = "sync_device_id"
    }
}
