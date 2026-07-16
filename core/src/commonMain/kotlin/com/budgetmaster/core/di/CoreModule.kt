package com.budgetmaster.core.di

import com.budgetmaster.core.db.AppDataSeeder
import com.budgetmaster.core.db.DatabaseDriverFactory
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.prefs.AppSettingsRepository
import com.budgetmaster.core.prefs.OnboardingPreferences
import com.budgetmaster.core.session.ActiveAccountStore
import com.budgetmaster.core.session.SessionStore
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for core database and infrastructure dependencies.
 */
val coreModule = module {
    single { DatabaseDriverFactory() }
    single { DatabaseProvider(get<DatabaseDriverFactory>()) }
    single { AppDataSeeder(get()) }
    single { AppSettingsRepository(get()) }
    single { OnboardingPreferences(get()) }
    single { SessionStore() }
    single { ActiveAccountStore(get()) }
}

/**
 * Platform-specific core module for dependencies (like DataStore) that vary by platform.
 */
expect val platformCoreModule: Module
