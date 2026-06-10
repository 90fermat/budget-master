package com.budgetmaster.auth.domain.model

/**
 * Represents the current authentication status of the application.
 */
sealed interface AuthStatus {
    /**
     * Authentication status is currently being checked (e.g., initial app load).
     */
    data object Checking : AuthStatus

    /**
     * User is authenticated.
     *
     * @property user The authenticated user's details.
     */
    data class Authenticated(val user: User) : AuthStatus

    /**
     * User is not authenticated.
     */
    data object Unauthenticated : AuthStatus
}
