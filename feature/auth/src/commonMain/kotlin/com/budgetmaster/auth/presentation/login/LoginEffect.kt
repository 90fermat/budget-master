package com.budgetmaster.auth.presentation.login

/**
 * Single-use side effects for navigating or showing feedback from the Login screen.
 */
sealed interface LoginEffect {
    /** Navigation action to the Home/Dashboard flow. */
    data object NavigateToHome : LoginEffect

    /** Navigation action to the Register flow. */
    data object NavigateToRegister : LoginEffect

    /** Navigation action to the Forgot Password flow. */
    data object NavigateToForgotPassword : LoginEffect

    /**
     * Command to display an error notification.
     * @property message The text content of the error.
     */
    data class ShowError(val message: String) : LoginEffect
}
