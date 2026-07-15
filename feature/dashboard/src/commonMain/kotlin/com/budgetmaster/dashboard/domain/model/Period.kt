package com.budgetmaster.dashboard.domain.model

/**
 * Represents the time period for filtering or grouping dashboard financial data.
 */
enum class Period {
    /**
     * Filters dashboard data to the current or last 7 days (Week).
     */
    WEEK,

    /**
     * Filters dashboard data to the current or last 30 days (Month).
     */
    MONTH,

    /**
     * Filters dashboard data to the current or last 365 days (Year).
     */
    YEAR,

    /**
     * Fetches all available historical dashboard data.
     */
    ALL
}
