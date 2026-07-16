package com.budgetmaster.auth.domain.usecase

import com.budgetmaster.auth.domain.model.AuthError
import com.budgetmaster.auth.domain.model.AuthException
import com.budgetmaster.auth.domain.model.User
import com.budgetmaster.auth.domain.repository.AuthRepository

/**
 * Exchanges a Google ID token from the platform sign-in flow for a Firebase session.
 *
 * @throws AuthException with [AuthError.GoogleUnavailable] when the token is blank.
 */
class SignInWithGoogleUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(idToken: String): User {
        if (idToken.isBlank()) throw AuthException(AuthError.GoogleUnavailable)
        return authRepository.signInWithGoogle(idToken)
    }
}
