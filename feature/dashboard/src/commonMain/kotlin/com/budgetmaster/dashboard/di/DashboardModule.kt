package com.budgetmaster.dashboard.di

import com.budgetmaster.core.notifications.NotificationRepository
import com.budgetmaster.dashboard.data.repository.SqlDelightDashboardRepository
import com.budgetmaster.dashboard.data.service.GeminiInsightsService
import com.budgetmaster.dashboard.domain.repository.DashboardRepository
import com.budgetmaster.dashboard.domain.usecase.GetAiInsightsUseCase
import com.budgetmaster.dashboard.domain.usecase.GetBalanceSummaryUseCase
import com.budgetmaster.dashboard.domain.usecase.GetBudgetProgressUseCase
import com.budgetmaster.dashboard.domain.usecase.GetChartDataUseCase
import com.budgetmaster.dashboard.domain.usecase.GetTopTransactionsUseCase
import com.budgetmaster.dashboard.presentation.DashboardViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for the Dashboard feature dependencies.
 */
val dashboardModule = module {
    // Data & Service Layer
    // GenAiClient is provided by coreModule (shared across features); we just consume it here.
    single { GeminiInsightsService(databaseProvider = get(), genAiClient = get()) }
    single<DashboardRepository> { SqlDelightDashboardRepository(get(), get(), get(), get()) }

    // Domain Use Cases — stateless, factory scoped
    factory { GetBalanceSummaryUseCase(get()) }
    factory { GetChartDataUseCase(get()) }
    factory { GetBudgetProgressUseCase(get()) }
    factory { GetTopTransactionsUseCase(get()) }
    factory { GetAiInsightsUseCase(get()) }

    // Presentation — lifecycle-aware ViewModel
    viewModel {
        DashboardViewModel(
            repository = get(),
            getBalanceSummary = get(),
            getChartData = get(),
            getBudgetProgress = get(),
            getTopTransactions = get(),
            getAiInsights = get(),
            settingsRepository = get(),
            sessionStore = get(),
            unreadNotifications = get<NotificationRepository>().observeUnreadCount(),
        )
    }
}
