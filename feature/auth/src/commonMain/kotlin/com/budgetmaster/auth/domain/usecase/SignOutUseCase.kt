package com.budgetmaster.auth.domain.usecase

import com.budgetmaster.auth.domain.repository.AuthRepository

/**
 * Use case to sign out the currently authenticated user.
 */
class SignOutUseCase(private val authRepository: AuthRepository) {
    /**
     * Executes the sign-out operation.
     */
    suspend operator fun invoke() = authRepository.signOut()
}
