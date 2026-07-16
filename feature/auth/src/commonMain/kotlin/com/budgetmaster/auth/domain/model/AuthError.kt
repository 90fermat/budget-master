package com.budgetmaster.auth.domain.model

/**
 * Platform-agnostic classification of an authentication failure.
 *
 * Repositories map platform exceptions (Firebase, validation) onto these cases so the
 * presentation layer can resolve a **localized** message without depending on any
 * platform SDK or hardcoding English text.
 */
enum class AuthError {
    /** Fields required for the action were left blank. */
    EmptyFields,

    /** The email address is malformed. */
    InvalidEmail,

    /** The chosen password does not meet the minimum length/strength. */
    WeakPassword,

    /** The two password entries do not match (registration). */
    PasswordMismatch,

    /** Email/password combination was rejected by the backend. */
    InvalidCredentials,

    /** No account exists for the supplied email. */
    UserNotFound,

    /** An account already exists for the supplied email (registration). */
    EmailAlreadyInUse,

    /** Too many attempts in a short window; the backend is rate-limiting. */
    TooManyRequests,

    /** The request could not reach the backend. */
    Network,

    /** Any failure that does not map to a more specific case. */
    Unknown,
}

/**
 * Carries a typed [AuthError] out of the domain/data layers so ViewModels can surface it
 * without inspecting exception messages.
 */
class AuthException(
    val error: AuthError,
    override val cause: Throwable? = null,
) : Exception(error.name, cause)
