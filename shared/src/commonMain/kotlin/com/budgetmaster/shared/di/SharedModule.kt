package com.budgetmaster.shared.di

import com.budgetmaster.core.di.coreModule
import com.budgetmaster.core.di.platformCoreModule
import com.budgetmaster.auth.di.authModule
import com.budgetmaster.auth.di.platformAuthModule
import com.budgetmaster.dashboard.di.dashboardModule
import com.budgetmaster.transactions.di.transactionsModule
import com.budgetmaster.budgets.di.budgetsModule
import com.budgetmaster.accounts.di.accountsModule
import com.budgetmaster.reports.di.reportsModule
import com.budgetmaster.settings.di.settingsModule
import com.budgetmaster.shared.notifications.presentation.NotificationsViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Koin module for presentation/shared UI layer dependencies.
 */
val sharedModule = module {
    // The notifications inbox is a cross-cutting surface over the :core notification store, so it
    // is orchestrated here in the shell rather than owned by any one feature.
    viewModel { NotificationsViewModel(get()) }
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
        accountsModule,
        reportsModule,
        settingsModule,
        sharedModule
    )
}
