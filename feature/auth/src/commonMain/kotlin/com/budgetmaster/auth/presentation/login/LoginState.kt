package com.budgetmaster.auth.presentation.login

import com.budgetmaster.auth.domain.model.AuthError

/**
 * UI state for the Login screen.
 *
 * @property email The current email input value.
 * @property password The current password input value.
 * @property isLoading Indicates if a login operation is in progress.
 * @property error The active typed error, if any (resolved to a localized string in the UI).
 * @property isPasswordVisible Whether the password field is shown in clear text.
 */
data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val isPasswordVisible: Boolean = false,
)
