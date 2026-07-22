package com.budgetmaster.auth.presentation.onboarding

/**
 * Navigation side-effects for the Onboarding screen.
 */
sealed interface OnboardingEffect {
    /** Navigate to the biometric setup screen (platforms with biometric support). */

    /** Skip biometric setup and proceed to sign-in (e.g. on Web). */
    data object NavigateToLogin : OnboardingEffect
}
