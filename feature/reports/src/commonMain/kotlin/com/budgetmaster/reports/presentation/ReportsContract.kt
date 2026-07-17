package com.budgetmaster.reports.presentation

import com.budgetmaster.reports.domain.model.ReportRange
import com.budgetmaster.reports.domain.model.ReportSummary

/** User actions on the Reports screen. */
sealed interface ReportsIntent {
    data class RangeChanged(val range: ReportRange) : ReportsIntent
    data object ExportCsvClicked : ReportsIntent

    /** Ask the AI to write the narrative summary for the current report. */
    data object GenerateNarrative : ReportsIntent

    /** Ask a free-text finance question about the current report. */
    data class AskQuestion(val question: String) : ReportsIntent
}

/**
 * A one-shot AI result: loading → either text or a typed message. Kept distinct from the report
 * itself so it can be requested on demand and re-run without touching the report state.
 */
sealed interface AiText {
    data object Idle : AiText
    data object Loading : AiText
    data class Ready(val text: String) : AiText
    data class Failed(val message: String) : AiText
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
    /** True when an AI provider exists and the user opted in; gates the narrative and Q&A UI. */
    val aiEnabled: Boolean = false,
    val narrative: AiText = AiText.Idle,
    val answer: AiText = AiText.Idle,
) {
    val isEmpty: Boolean get() = !isLoading && (report?.isEmpty ?: true)
}
