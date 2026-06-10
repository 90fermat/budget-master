package com.budgetmaster.auth.domain.usecase

import com.budgetmaster.auth.domain.repository.AuthRepository

/**
 * Use case to request a password reset email.
 */
class ResetPasswordUseCase(private val authRepository: AuthRepository) {
    /**
     * Executes the password reset operation.
     */
    suspend operator fun invoke(email: String) = authRepository.sendPasswordReset(email)
}
