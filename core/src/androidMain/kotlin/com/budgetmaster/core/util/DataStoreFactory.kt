package com.budgetmaster.core.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.budgetmaster.core.db.AppContextHolder
import okio.Path.Companion.toPath

/**
 * Android factory for creating [DataStore] preferences.
 */
class DataStoreFactory {
    /**
     * Creates and returns a [DataStore] preferences instance for Android.
     */
    fun create(): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                AppContextHolder.context.filesDir.resolve("budgetmaster.preferences_pb").absolutePath.toPath()
            }
        )
    }
}
