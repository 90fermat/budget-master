package com.budgetmaster.auth.presentation.splash

/**
 * Single-use navigation effects emitted from the Splash screen.
 */
sealed interface SplashEffect {
    /** Navigate to the onboarding flow (first launch). */
    data object NavigateToOnboarding : SplashEffect

    /** Navigate to the login screen. */
    data object NavigateToLogin : SplashEffect

    /** Navigate directly to the dashboard (already authenticated). */
    data object NavigateToDashboard : SplashEffect
}
