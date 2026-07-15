package com.budgetmaster.core.prefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Wasm [KeyValueStore] backed by the browser `localStorage`, with in-memory
 * [MutableStateFlow]s providing reactivity within the running page.
 */
class LocalStorageKeyValueStore : KeyValueStore {

    private val flows = mutableMapOf<String, MutableStateFlow<String?>>()

    private fun stateFlow(key: String): MutableStateFlow<String?> =
        flows.getOrPut(key) { MutableStateFlow(localStorageGet(key)) }

    override fun observeString(key: String): Flow<String?> = stateFlow(key)

    override suspend fun putString(key: String, value: String) {
        localStorageSet(key, value)
        stateFlow(key).value = value
    }

    override suspend fun remove(key: String) {
        localStorageRemove(key)
        stateFlow(key).value = null
    }
}

private fun localStorageGet(key: String): String? = js("localStorage.getItem(key)")

private fun localStorageSet(key: String, value: String): Unit = js("{ localStorage.setItem(key, value); }")

private fun localStorageRemove(key: String): Unit = js("{ localStorage.removeItem(key); }")
