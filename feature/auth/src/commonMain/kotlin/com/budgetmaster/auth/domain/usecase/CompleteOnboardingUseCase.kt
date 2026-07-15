package com.budgetmaster.auth.domain.usecase

import com.budgetmaster.core.prefs.OnboardingPreferences

/**
 * Marks the onboarding flow as completed so it is not shown on subsequent launches.
 */
class CompleteOnboardingUseCase(private val onboardingPreferences: OnboardingPreferences) {
    suspend operator fun invoke() = onboardingPreferences.setCompleted(true)
}
