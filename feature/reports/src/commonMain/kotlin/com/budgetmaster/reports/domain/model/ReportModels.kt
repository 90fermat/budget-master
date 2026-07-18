package com.budgetmaster.reports.domain.model

import kotlinx.datetime.LocalDate

/** The window a report covers. */
enum class ReportRange {
    MONTH,
    QUARTER,
    YEAR,
    ALL,
}

/** One category's share of spending in the period. */
data class CategorySlice(
    val categoryId: String,
    val name: String,
    val colorHex: String,
    val amount: Double,
    /** Fraction of total spending, 0..1. */
    val share: Float,
)

/** Income and expenses bucketed to a day. */
data class TrendPoint(
    val date: LocalDate,
    val income: Double,
    val expenses: Double,
) {
    val net: Double get() = income - expenses
}

/**
 * A finished report for one period.
 *
 * Transfers and balance adjustments are excluded upstream — they move the user's own money
 * and would otherwise inflate both income and expenses.
 *
 * @property previousIncome/[previousExpenses] the immediately preceding period of the same
 * length, for comparison. Zero when there is no prior data.
 */
data class ReportSummary(
    val range: ReportRange,
    val totalIncome: Double,
    val totalExpenses: Double,
    /** Spending by category, largest first. */
    val categories: List<CategorySlice>,
    /**
     * Income by category, largest first — "where does my money come from".
     *
     * Separate from [categories] rather than signed, because the two are never shown together and
     * each share is a fraction of its own total.
     */
    val incomeCategories: List<CategorySlice> = emptyList(),
    val trend: List<TrendPoint>,
    val previousIncome: Double,
    val previousExpenses: Double,
    val currencyCode: String,
) {
    val net: Double get() = totalIncome - totalExpenses

    /** Spend change vs the previous period, as a fraction (+0.2 = 20% more). Null if no basis. */
    val expenseChange: Float?
        get() = if (previousExpenses <= 0.0) null
        else ((totalExpenses - previousExpenses) / previousExpenses).toFloat()

    /** Income change vs the previous period, as a fraction. Null if no basis. */
    val incomeChange: Float?
        get() = if (previousIncome <= 0.0) null
        else ((totalIncome - previousIncome) / previousIncome).toFloat()

    val isEmpty: Boolean get() = totalIncome == 0.0 && totalExpenses == 0.0
}
