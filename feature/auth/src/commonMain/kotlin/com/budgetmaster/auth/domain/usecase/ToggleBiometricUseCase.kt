package com.budgetmaster.auth.domain.usecase

import com.budgetmaster.auth.domain.repository.AuthRepository

/**
 * Use case to enable or disable biometric authentication.
 */
class ToggleBiometricUseCase(private val authRepository: AuthRepository) {
    /**
     * Sets the biometric authentication preference.
     *
     * @param enabled True to enable biometric authentication, false to disable.
     */
    suspend operator fun invoke(enabled: Boolean) = authRepository.setBiometricEnabled(enabled)
}
