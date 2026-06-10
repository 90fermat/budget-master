package com.budgetmaster.auth.presentation.onboarding

/**
 * User actions from the Onboarding screen.
 */
sealed interface OnboardingIntent {
    /** Navigate to the next page. */
    data object NextPage : OnboardingIntent

    /** Navigate to the previous page. */
    data object PreviousPage : OnboardingIntent

    /** Skip the onboarding flow entirely. */
    data object Skip : OnboardingIntent

    /** Complete the onboarding flow. */
    data object Finish : OnboardingIntent
}
