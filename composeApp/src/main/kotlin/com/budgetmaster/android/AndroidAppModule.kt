package com.budgetmaster.android

import com.budgetmaster.shared.MoneyMessageImporter
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android-only wiring that belongs to the app shell rather than a feature: the mobile-money
 * importer (resolved by the SMS receiver) and the system notifier it posts through.
 */
val androidAppModule = module {
    // settingsRepository, walletDirectory, notifications, systemNotifier, importMessage
    single { MoneyMessageImporter(get(), get(), get(), get(), get()) }
}
