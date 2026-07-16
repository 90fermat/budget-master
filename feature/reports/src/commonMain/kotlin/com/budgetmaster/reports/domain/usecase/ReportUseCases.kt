package com.budgetmaster.reports.domain.usecase

import com.budgetmaster.reports.domain.model.ReportRange
import com.budgetmaster.reports.domain.model.ReportSummary
import com.budgetmaster.reports.domain.repository.ReportsRepository
import com.budgetmaster.reports.domain.shareCsv
import kotlinx.coroutines.flow.Flow

/** Observes the report for a range. */
class ObserveReportUseCase(private val repository: ReportsRepository) {
    operator fun invoke(range: ReportRange): Flow<ReportSummary> = repository.observeReport(range)
}

/**
 * Exports the period's transactions to CSV and hands them to the platform.
 *
 * @return true when the share/download started.
 */
class ExportReportCsvUseCase(private val repository: ReportsRepository) {
    suspend operator fun invoke(range: ReportRange): Boolean {
        val csv = repository.exportCsv(range)
        return shareCsv(fileName = "budget-master-${range.name.lowercase()}.csv", content = csv)
    }
}
