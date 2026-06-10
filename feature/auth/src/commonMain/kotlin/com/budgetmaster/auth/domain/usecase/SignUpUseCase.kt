package com.budgetmaster.auth.domain.usecase

import com.budgetmaster.auth.domain.model.User
import com.budgetmaster.auth.domain.repository.AuthRepository

/**
 * Use case to register a new user with email and password.
 */
class SignUpUseCase(private val authRepository: AuthRepository) {
    /**
     * Executes the sign-up operation.
     */
    suspend operator fun invoke(email: String, password: String): User =
        authRepository.signUp(email, password)
}
