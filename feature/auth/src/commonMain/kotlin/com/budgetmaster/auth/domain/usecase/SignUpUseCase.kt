package com.budgetmaster.auth.domain.usecase

import com.budgetmaster.auth.domain.model.AuthError
import com.budgetmaster.auth.domain.model.AuthException
import com.budgetmaster.auth.domain.model.User
import com.budgetmaster.auth.domain.repository.AuthRepository

/**
 * Use case to register a new user with email and password.
 *
 * Applies the same shape validation as [LoginUseCase] before delegating to the
 * [AuthRepository], reporting failures as a typed [AuthException].
 */
class SignUpUseCase(private val authRepository: AuthRepository) {
    /**
     * Validates [email]/[password], then executes the sign-up operation.
     *
     * @throws AuthException with [AuthError.EmptyFields], [AuthError.InvalidEmail] or
     * [AuthError.WeakPassword] when the input is invalid.
     */
    suspend operator fun invoke(email: String, password: String): User {
        if (email.isBlank() || password.isBlank()) throw AuthException(AuthError.EmptyFields)
        if (!email.isValidEmail()) throw AuthException(AuthError.InvalidEmail)
        if (password.length < MIN_PASSWORD_LENGTH) throw AuthException(AuthError.WeakPassword)
        return authRepository.signUp(email.trim(), password)
    }
}
