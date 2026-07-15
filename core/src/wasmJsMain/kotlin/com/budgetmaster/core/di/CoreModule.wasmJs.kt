package com.budgetmaster.core.di

import com.budgetmaster.core.prefs.KeyValueStore
import com.budgetmaster.core.prefs.LocalStorageKeyValueStore
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * WasmJs implementation of [platformCoreModule] providing a `localStorage`-backed
 * key-value store for user preferences.
 */
actual val platformCoreModule: Module = module {
    single<KeyValueStore> { LocalStorageKeyValueStore() }
}
