package com.budgetmaster.core.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Android [KeyValueStore] backed by the shared Preferences [DataStore].
 */
class DataStoreKeyValueStore(private val dataStore: DataStore<Preferences>) : KeyValueStore {

    override fun observeString(key: String): Flow<String?> =
        dataStore.data.map { it[stringPreferencesKey(key)] }

    override suspend fun putString(key: String, value: String) {
        dataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    override suspend fun remove(key: String) {
        dataStore.edit { it.remove(stringPreferencesKey(key)) }
    }
}
