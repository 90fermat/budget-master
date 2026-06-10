package com.budgetmaster.core.di

import com.budgetmaster.core.util.DataStoreFactory
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android implementation of [platformCoreModule] providing Preferences DataStore.
 */
actual val platformCoreModule: Module = module {
    single { DataStoreFactory().create() }
}
