package com.budgetmaster.shared.di

import com.budgetmaster.core.di.coreModule
import com.budgetmaster.core.di.platformCoreModule
import com.budgetmaster.auth.di.authModule
import com.budgetmaster.auth.di.platformAuthModule
import com.budgetmaster.dashboard.di.dashboardModule
import com.budgetmaster.transactions.di.transactionsModule
import com.budgetmaster.budgets.di.budgetsModule
import com.budgetmaster.reports.di.reportsModule
import com.budgetmaster.settings.di.settingsModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Koin module for presentation/shared UI layer dependencies.
 */
val sharedModule = module {
    // Register ViewModels and UI-specific components here
}

/**
 * Bootstraps Koin for dependency injection across all feature modules.
 *
 * @param appDeclaration Platform-specific configuration (e.g., passing Android context).
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(
        coreModule,
        platformCoreModule,
        authModule,
        platformAuthModule,
        dashboardModule,
        transactionsModule,
        budgetsModule,
        reportsModule,
        settingsModule,
        sharedModule
    )
}
