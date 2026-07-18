package com.budgetmaster.android

import com.budgetmaster.shared.MoneyMessageImporter
import org.koin.dsl.module

/**
 * Android-only wiring that belongs to the app shell rather than a feature — currently the
 * mobile-money importer, which the SMS receiver resolves from Koin.
 */
val androidAppModule = module {
    single { MoneyMessageImporter(get(), get(), get()) }
}
