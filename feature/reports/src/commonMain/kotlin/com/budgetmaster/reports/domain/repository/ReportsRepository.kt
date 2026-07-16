package com.budgetmaster.reports.domain.repository

import com.budgetmaster.reports.domain.model.ReportRange
import com.budgetmaster.reports.domain.model.ReportSummary
import kotlinx.coroutines.flow.Flow

/** Aggregated spending reports for the signed-in user. */
interface ReportsRepository {

    /**
     * Observes the report for [range], scoped to the active wallet (or all of the user's
     * wallets under "All accounts").
     */
    fun observeReport(range: ReportRange): Flow<ReportSummary>

    /**
     * Renders the period's transactions as CSV (RFC 4180 quoting), newest first.
     *
     * Unlike the charts this **includes** transfers, flagged in a `Transfer` column — an
     * export is a record of what happened to the money, not an income/expense analysis.
     */
    suspend fun exportCsv(range: ReportRange): String
}
