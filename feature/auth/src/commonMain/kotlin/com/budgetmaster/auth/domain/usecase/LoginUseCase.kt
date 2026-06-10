package com.budgetmaster.auth.domain.usecase

import com.budgetmaster.auth.domain.model.User
import com.budgetmaster.auth.domain.repository.AuthRepository

/** Minimum password length constant. */
private const val MIN_PASSWORD_LENGTH = 6

/**
 * Use case responsible for validating credentials and signing in the user.
 *
 * Validates the email format and minimum password length before delegating
 * to the [AuthRepository].
 *
 * @param authRepository The repository handling the authentication network call.
 */
class LoginUseCase(private val authRepository: AuthRepository) {

    /**
     * Validates [email] and [password], then executes the sign-in operation.
     *
     * @throws IllegalArgumentException if the email is blank, malformed, or the password is too short.
     */
    suspend operator fun invoke(email: String, password: String): User {
        require(email.isNotBlank()) { "Email must not be empty." }
        require(email.contains('@') && email.contains('.')) { "Email format is invalid." }
        require(password.length >= MIN_PASSWORD_LENGTH) {
            "Password must be at least $MIN_PASSWORD_LENGTH characters."
        }
        return authRepository.signIn(email.trim(), password)
    }
}
