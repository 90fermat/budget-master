package com.budgetmaster.core.di

import com.budgetmaster.core.backup.BackupService
import com.budgetmaster.core.db.AppDataSeeder
import com.budgetmaster.core.db.DatabaseDriverFactory
import com.budgetmaster.core.db.UserDataEraser
import com.budgetmaster.core.db.WalletDirectory
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.ai.GenAiClient
import com.budgetmaster.core.ai.createGenAiClient
import com.budgetmaster.core.config.createRemoteFeatureFlags
import com.budgetmaster.core.ocr.createReceiptTextRecognizer
import com.budgetmaster.core.currency.ExchangeRateFetcher
import com.budgetmaster.core.currency.ExchangeRateRepository
import com.budgetmaster.core.currency.RefreshExchangeRatesUseCase
import com.budgetmaster.core.notifications.NotificationRepository
import com.budgetmaster.core.prefs.AppSettingsRepository
import com.budgetmaster.core.guidance.GuidancePreferences
import com.budgetmaster.core.prefs.OnboardingPreferences
import com.budgetmaster.core.session.ActiveAccountStore
import com.budgetmaster.core.security.AppLockController
import com.budgetmaster.core.security.BiometricPrompter
import com.budgetmaster.core.session.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    single { UserDataEraser(get()) }
    single { WalletDirectory(get(), get()) }
    single { ActiveAccountStore(get()) }
    single { ExchangeRateRepository(get()) }
    single { ExchangeRateFetcher() }
    factory { RefreshExchangeRatesUseCase(get(), get()) }

    // The platform's AI client: Firebase AI Logic on Android, "unavailable" elsewhere until those
    // SDKs are bridged. Lives in core so every feature (dashboard insights, transaction quick-add)
    // shares one instance. No API key is involved on any target.
    single { createRemoteFeatureFlags() }
    single { createReceiptTextRecognizer() }
    // The AI client consults the remote kill-switch, so every AI surface (which all check
    // isAvailable) respects it with no per-feature wiring.
    single<GenAiClient> { createGenAiClient(get()) }
    single { NotificationRepository(get(), get()) }
    single { BiometricPrompter() }
    single { BackupService(get()) }
    // One controller for the process: the lock state must outlive any screen that shows it, and
    // the failed-attempt count must survive the unlock UI being closed and reopened.
    single { AppLockController(get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
}

/**
 * Platform-specific core module for dependencies (like DataStore) that vary by platform.
 */
expect val platformCoreModule: Module
