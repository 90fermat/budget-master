package com.budgetmaster.auth.domain.usecase

import com.budgetmaster.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to check whether biometric authentication is currently enabled.
 */
class CheckBiometricSupportUseCase(private val authRepository: AuthRepository) {
    /**
     * Returns a flow emitting true if biometric authentication is enabled.
     */
    operator fun invoke(): Flow<Boolean> = authRepository.isBiometricEnabled()
}
