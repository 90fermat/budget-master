package com.budgetmaster.dashboard.presentation

import com.budgetmaster.dashboard.domain.model.Insight

/**
 * Represents the asynchronous loading states for AI-generated insights on the Dashboard.
 *
 * Used as the type of [DashboardState.insights] to express whether insights are
 * currently loading, successfully loaded, or failed.
 */
sealed interface InsightsState {
    /**
     * Insights are being fetched from the AI service or local cache.
     */
    data object Loading : InsightsState

    /**
     * Insights were successfully retrieved.
     *
     * @property data The list of AI-generated [Insight] items to display.
     */
    data class Success(val data: List<Insight>) : InsightsState

    /**
     * Insight retrieval failed.
     *
     * @property message A human-readable error description.
     */
    data class Error(val message: String) : InsightsState
}
