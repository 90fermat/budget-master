package com.budgetmaster.dashboard.domain.model

import kotlinx.datetime.LocalDate

/**
 * Domain model representing a single data point in a financial chart.
 *
 * @property date The specific date of this data point.
 * @property income The total income recorded for this date.
 * @property expenses The total expenses recorded for this date.
 * @property balance The net cumulative or daily balance on this date.
 */
data class ChartPoint(
    val date: LocalDate,
    val income: Double,
    val expenses: Double,
    val balance: Double
)
