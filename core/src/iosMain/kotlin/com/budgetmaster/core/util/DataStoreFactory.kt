package com.budgetmaster.core.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * iOS factory for creating [DataStore] preferences.
 */
class DataStoreFactory {
    /**
     * Creates and returns a [DataStore] preferences instance for iOS.
     */
    fun create(): DataStore<Preferences> {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )
        return PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                (documentDirectory!!.path + "/budgetmaster.preferences_pb").toPath()
            }
        )
    }
}
