package com.budgetmaster.reports.presentation

import com.budgetmaster.reports.domain.model.ReportRange
import com.budgetmaster.reports.domain.model.ReportSummary

/** User actions on the Reports screen. */
sealed interface ReportsIntent {
    data class RangeChanged(val range: ReportRange) : ReportsIntent
    data object ExportCsvClicked : ReportsIntent
}

/** One-shot side effects. */
sealed interface ReportsEffect {
    data object ExportStarted : ReportsEffect
    data object ExportUnavailable : ReportsEffect
    data class ShowError(val message: String) : ReportsEffect
}

/** Immutable UI state of the Reports screen. */
data class ReportsState(
    val isLoading: Boolean = true,
    val range: ReportRange = ReportRange.MONTH,
    val report: ReportSummary? = null,
    val isExporting: Boolean = false,
) {
    val isEmpty: Boolean get() = !isLoading && (report?.isEmpty ?: true)
}
