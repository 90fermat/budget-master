package com.budgetmaster.auth.presentation.onboarding

/**
 * Navigation side-effects for the Onboarding screen.
 */
sealed interface OnboardingEffect {
    /** Navigate to the biometric setup screen. */
    data object NavigateToBiometric : OnboardingEffect
}
