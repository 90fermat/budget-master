package com.budgetmaster.core.di

import com.budgetmaster.core.db.AppDataSeeder
import com.budgetmaster.core.db.DatabaseDriverFactory
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.currency.ExchangeRateFetcher
import com.budgetmaster.core.currency.ExchangeRateRepository
import com.budgetmaster.core.currency.RefreshExchangeRatesUseCase
import com.budgetmaster.core.notifications.NotificationRepository
import com.budgetmaster.core.prefs.AppSettingsRepository
import com.budgetmaster.core.guidance.GuidancePreferences
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
    single { GuidancePreferences(get()) }
    single { SessionStore() }
    single { ActiveAccountStore(get()) }
    single { ExchangeRateRepository(get()) }
    single { ExchangeRateFetcher() }
    factory { RefreshExchangeRatesUseCase(get(), get()) }
    single { NotificationRepository(get(), get()) }
}

/**
 * Platform-specific core module for dependencies (like DataStore) that vary by platform.
 */
expect val platformCoreModule: Module
