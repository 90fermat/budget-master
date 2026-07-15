package com.budgetmaster.dashboard.domain.model

/**
 * Indicates the directional trend of the account balance.
 */
enum class BalanceTrend {
    /**
     * Positive financial trend, typically where net savings or income is growing.
     */
    POSITIVE,

    /**
     * Negative financial trend, typically where spending exceeds income.
     */
    NEGATIVE,

    /**
     * Neutral financial trend, where net savings or income remains relatively flat.
     */
    NEUTRAL
}

/**
 * Domain representation of the user's overall balance overview.
 *
 * @property totalBalance The current net balance across all linked accounts.
 * @property monthlyIncome The total income generated during the current month.
 * @property monthlyExpenses The total expenses incurred during the current month.
 * @property balanceTrend The calculated trend direction of the balance (POSITIVE, NEGATIVE, or NEUTRAL).
 * @property trendPercentage The percentage change showing the trend velocity or magnitude.
 */
data class BalanceSummary(
    val totalBalance: Double,
    val monthlyIncome: Double,
    val monthlyExpenses: Double,
    val balanceTrend: BalanceTrend,
    val trendPercentage: Double
)
