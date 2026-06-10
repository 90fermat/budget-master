package com.budgetmaster.auth.domain.usecase

import com.budgetmaster.auth.domain.model.User
import com.budgetmaster.auth.domain.repository.AuthRepository

/**
 * Use case to sign in a user with email and password.
 */
class SignInUseCase(private val authRepository: AuthRepository) {
    /**
     * Executes the sign-in operation.
     */
    suspend operator fun invoke(email: String, password: String): User =
        authRepository.signIn(email, password)
}
