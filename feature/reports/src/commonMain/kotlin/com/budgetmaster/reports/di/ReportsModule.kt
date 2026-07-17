package com.budgetmaster.reports.di

import com.budgetmaster.reports.data.SqlDelightReportsRepository
import com.budgetmaster.reports.domain.repository.ReportsRepository
import com.budgetmaster.reports.domain.usecase.AnswerFinanceQuestionUseCase
import com.budgetmaster.reports.domain.usecase.ExportReportCsvUseCase
import com.budgetmaster.reports.domain.usecase.GenerateNarrativeUseCase
import com.budgetmaster.reports.domain.usecase.ObserveReportUseCase
import com.budgetmaster.reports.presentation.ReportsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module for the Reports feature dependencies.
 */
val reportsModule = module {
    single { SqlDelightReportsRepository(get(), get(), get(), get()) } bind ReportsRepository::class

    factory { ObserveReportUseCase(get()) }
    factory { ExportReportCsvUseCase(get()) }
    factory { GenerateNarrativeUseCase(get()) }
    factory { AnswerFinanceQuestionUseCase(get()) }

    viewModel { ReportsViewModel(get(), get(), get(), get(), get()) }
}
