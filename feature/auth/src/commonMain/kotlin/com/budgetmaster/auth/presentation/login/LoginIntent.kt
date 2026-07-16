package com.budgetmaster.auth.presentation.login

/**
 * User actions and events originating from the Login screen.
 */
sealed interface LoginIntent {
    /**
     * Triggered when the user types an email.
     * @property email The updated email string.
     */
    data class EmailChanged(val email: String) : LoginIntent

    /**
     * Triggered when the user types a password.
     * @property password The updated password string.
     */
    data class PasswordChanged(val password: String) : LoginIntent

    /** Triggered when the user clicks the login button. */
    data object LoginClicked : LoginIntent

    /** Triggered when the user toggles password visibility. */
    data object TogglePasswordVisibility : LoginIntent

    /** The platform Google flow returned an ID token; exchange it for a session. */
    data class GoogleIdTokenReceived(val idToken: String) : LoginIntent

    /** The platform Google flow failed or was cancelled. */
    data class GoogleSignInFailed(val error: com.budgetmaster.auth.domain.model.AuthError) : LoginIntent

    /** Triggered when the user clicks the biometric login option. */
    data object BiometricLoginClicked : LoginIntent

    /** Triggered when the user requests navigation to the register screen. */
    data object NavigateToRegister : LoginIntent

    /** Triggered when the user requests navigation to the forgot password screen. */
    data object NavigateToForgotPassword : LoginIntent
}
