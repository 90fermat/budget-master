package com.budgetmaster.auth.domain.usecase

import com.budgetmaster.core.prefs.OnboardingPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Use case to check whether the user has completed the onboarding flow.
 *
 * Backed by [OnboardingPreferences] (persisted in `:core`) so the result is durable
 * across launches and consistent on every platform.
 */
class CheckFirstLaunchUseCase(private val onboardingPreferences: OnboardingPreferences) {
    /** Returns a flow emitting true once onboarding has been completed. */
    operator fun invoke(): Flow<Boolean> = onboardingPreferences.isCompleted
}
