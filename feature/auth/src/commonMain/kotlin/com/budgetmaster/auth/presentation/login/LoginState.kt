package com.budgetmaster.auth.presentation.login

/**
 * UI state for the Login screen.
 *
 * @property email The current email input value.
 * @property password The current password input value.
 * @property isLoading Indicates if a login operation is in progress.
 * @property errorMessage The active error message, if any.
 * @property isPasswordVisible Whether the password field is visible.
 */
data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isPasswordVisible: Boolean = false
)
