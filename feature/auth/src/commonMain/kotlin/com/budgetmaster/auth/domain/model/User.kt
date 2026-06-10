package com.budgetmaster.auth.domain.model

/**
 * Domain representation of an authenticated user.
 *
 * @property id Unique identifier of the user.
 * @property email The email address of the user.
 * @property displayName The display name of the user, if available.
 * @property isEmailVerified Indicates if the user's email has been verified.
 */
data class User(
    val id: String,
    val email: String,
    val displayName: String?,
    val isEmailVerified: Boolean
)
