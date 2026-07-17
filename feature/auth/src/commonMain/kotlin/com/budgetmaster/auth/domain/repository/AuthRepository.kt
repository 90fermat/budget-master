package com.budgetmaster.auth.domain.repository

import com.budgetmaster.auth.domain.model.AuthStatus
import com.budgetmaster.auth.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Interface representing authentication, onboarding, and security preferences.
 */
interface AuthRepository {
    /**
     * Observes the current authentication status of the user.
     */
    fun getAuthStatus(): Flow<AuthStatus>

    /**
     * Signs in a user using email and password.
     */
    suspend fun signIn(email: String, password: String): User

    /**
     * Registers a new user with email and password.
     */
    suspend fun signUp(email: String, password: String): User

    /**
     * Exchanges a Google ID token (obtained from the platform sign-in flow) for a Firebase
     * session. Only reachable where [com.budgetmaster.auth.domain.isGoogleSignInSupported].
     */
    suspend fun signInWithGoogle(idToken: String): User

    /**
     * Signs out the currently authenticated user.
     */
    suspend fun signOut()

    /**
     * Permanently deletes the currently authenticated user's account with the auth provider.
     *
     * This removes the *credential*; wiping the user's local data is a separate step the caller
     * owns. May fail with a re-authentication requirement if the session is old — the caller
     * should surface that and ask the user to sign in again. A no-op where there is no remote
     * account (the Web local-only profile).
     */
    suspend fun deleteAccount()

    /**
     * Sends a password reset link to the specified email address.
     */
    suspend fun sendPasswordReset(email: String)

    /**
     * Observes whether the onboarding process has been completed.
     */
    fun isOnboardingCompleted(): Flow<Boolean>

    /**
     * Sets the status of onboarding completion.
     */
    suspend fun setOnboardingCompleted(completed: Boolean)

    /**
     * Observes whether biometric authentication is enabled.
     */
    fun isBiometricEnabled(): Flow<Boolean>

    /**
     * Sets the state of biometric authentication permission/preference.
     */
    suspend fun setBiometricEnabled(enabled: Boolean)
}
