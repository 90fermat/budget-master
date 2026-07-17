package com.budgetmaster.dashboard

import com.budgetmaster.core.prefs.KeyValueStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [KeyValueStore] for tests (stands in for DataStore / localStorage).
 *
 * @param initial Seed values, for tests that need a preference already set before the subject
 *   first reads it.
 */
class InMemoryKeyValueStore(initial: Map<String, String> = emptyMap()) : KeyValueStore {

    private val entries = MutableStateFlow(initial)

    override fun observeString(key: String): Flow<String?> = entries.map { it[key] }

    override suspend fun putString(key: String, value: String) {
        entries.value = entries.value + (key to value)
    }

    override suspend fun remove(key: String) {
        entries.value = entries.value - key
    }
}
