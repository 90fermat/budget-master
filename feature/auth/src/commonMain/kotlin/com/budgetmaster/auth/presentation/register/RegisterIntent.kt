package com.budgetmaster.auth.presentation.register

/**
 * User actions from the Register screen.
 */
sealed interface RegisterIntent {
    data class EmailChanged(val email: String) : RegisterIntent
    data class PasswordChanged(val password: String) : RegisterIntent
    data class ConfirmPasswordChanged(val confirmPassword: String) : RegisterIntent
    data object RegisterClicked : RegisterIntent
    data object NavigateToLogin : RegisterIntent
}
