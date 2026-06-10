package com.budgetmaster.auth.domain.usecase

import com.budgetmaster.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to check whether the user has completed the onboarding flow.
 */
class CheckFirstLaunchUseCase(private val authRepository: AuthRepository) {
    /**
     * Returns a flow emitting true if the onboarding has been completed.
     */
    operator fun invoke(): Flow<Boolean> = authRepository.isOnboardingCompleted()
}
