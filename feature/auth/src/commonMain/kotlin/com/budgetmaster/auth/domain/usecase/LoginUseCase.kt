package com.budgetmaster.auth.domain.usecase

import com.budgetmaster.auth.domain.model.AuthError
import com.budgetmaster.auth.domain.model.AuthException
import com.budgetmaster.auth.domain.model.User
import com.budgetmaster.auth.domain.repository.AuthRepository

/** Minimum password length constant. */
internal const val MIN_PASSWORD_LENGTH = 6

/**
 * Use case responsible for validating credentials and signing in the user.
 *
 * Validates the email format and minimum password length before delegating
 * to the [AuthRepository]. Validation failures are reported as a typed
 * [AuthException] so the UI can render a localized message.
 *
 * @param authRepository The repository handling the authentication network call.
 */
class LoginUseCase(private val authRepository: AuthRepository) {

    /**
     * Validates [email] and [password], then executes the sign-in operation.
     *
     * @throws AuthException with [AuthError.EmptyFields], [AuthError.InvalidEmail] or
     * [AuthError.WeakPassword] when the input is invalid.
     */
    suspend operator fun invoke(email: String, password: String): User {
        if (email.isBlank() || password.isBlank()) throw AuthException(AuthError.EmptyFields)
        if (!email.isValidEmail()) throw AuthException(AuthError.InvalidEmail)
        if (password.length < MIN_PASSWORD_LENGTH) throw AuthException(AuthError.WeakPassword)
        return authRepository.signIn(email.trim(), password)
    }
}

/** Lightweight email shape check shared by the auth use cases. */
internal fun String.isValidEmail(): Boolean {
    val at = indexOf('@')
    val dot = lastIndexOf('.')
    return at > 0 && dot > at + 1 && dot < length - 1
}
