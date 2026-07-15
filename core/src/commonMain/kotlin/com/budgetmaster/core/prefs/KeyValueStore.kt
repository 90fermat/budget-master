package com.budgetmaster.core.prefs

import kotlinx.coroutines.flow.Flow

/**
 * Minimal reactive key-value persistence abstraction.
 *
 * Platform bindings: Preferences DataStore on Android/iOS, `localStorage` on Wasm.
 * Registered in each platform's `platformCoreModule`.
 */
interface KeyValueStore {
    /** Emits the current value for [key] and every subsequent change (`null` when unset). */
    fun observeString(key: String): Flow<String?>

    /** Persists [value] under [key]. */
    suspend fun putString(key: String, value: String)

    /** Removes [key]. */
    suspend fun remove(key: String)
}
