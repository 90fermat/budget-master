@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.dashboard.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Categorization of the financial insight.
 */
enum class InsightType {
    /**
     * Insights regarding user spending patterns, anomalies, or recommendations.
     */
    SPENDING,

    /**
     * Insights helping the user identify potential savings or budget adjustment opportunities.
     */
    SAVING,

    /**
     * Insights analyzing historical trends, forecasted behaviors, or general financial health.
     */
    TREND
}

/**
 * Domain representation of an AI-generated personalized financial insight.
 *
 * @property id The unique identifier of this insight.
 * @property type The type classification of the insight (SPENDING, SAVING, or TREND).
 * @property message The text content of the insight description.
 * @property actionRoute An optional navigation route linked to this insight to guide the user to resolve the issue or review details.
 * @property generatedAt The timestamp representing when this insight was calculated.
 */
data class Insight(
    val id: String,
    val type: InsightType,
    val message: String,
    val actionRoute: String?,
    val generatedAt: Instant
)
