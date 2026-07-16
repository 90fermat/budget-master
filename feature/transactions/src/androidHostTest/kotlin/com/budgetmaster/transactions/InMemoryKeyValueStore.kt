package com.budgetmaster.transactions

import com.budgetmaster.core.prefs.KeyValueStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [KeyValueStore] for tests (stands in for DataStore / localStorage). */
class InMemoryKeyValueStore : KeyValueStore {

    private val entries = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun observeString(key: String): Flow<String?> = entries.map { it[key] }

    override suspend fun putString(key: String, value: String) {
        entries.value = entries.value + (key to value)
    }

    override suspend fun remove(key: String) {
        entries.value = entries.value - key
    }
}
