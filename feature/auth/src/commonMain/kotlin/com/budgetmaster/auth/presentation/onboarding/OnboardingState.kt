package com.budgetmaster.auth.presentation.onboarding

/**
 * UI state for the Onboarding screen.
 *
 * @property currentPage The currently displayed onboarding page index.
 * @property totalPages Total number of onboarding pages.
 */
data class OnboardingState(
    val currentPage: Int = 0,
    val totalPages: Int = 3
)
