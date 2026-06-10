package com.budgetmaster.auth.data.repository

import com.budgetmaster.auth.domain.model.AuthStatus
import com.budgetmaster.auth.domain.model.User
import com.budgetmaster.auth.domain.repository.AuthRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * iOS Firebase Auth repository implementation.
 *
 * @param firebaseAuth Firebase Auth instance injected via Koin.
 * @param settings Additional platform settings (reserved for DataStore etc.).
 */
class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val settings: Any? = null
) : AuthRepository {

    override fun getAuthStatus(): Flow<AuthStatus> = flowOf(AuthStatus.Unauthenticated)

    override suspend fun signIn(email: String, password: String): User {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password)
        val fbUser = result.user ?: throw RuntimeException("Authentication failed")
        return User(
            id = fbUser.uid,
            email = fbUser.email ?: email,
            displayName = fbUser.displayName,
            isEmailVerified = fbUser.isEmailVerified
        )
    }

    override suspend fun signUp(email: String, password: String): User {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password)
        val fbUser = result.user ?: throw RuntimeException("Registration failed")
        return User(
            id = fbUser.uid,
            email = fbUser.email ?: email,
            displayName = fbUser.displayName,
            isEmailVerified = fbUser.isEmailVerified
        )
    }

    override suspend fun signOut() = firebaseAuth.signOut()
    override suspend fun sendPasswordReset(email: String) = firebaseAuth.sendPasswordResetEmail(email)
    override fun isOnboardingCompleted(): Flow<Boolean> = flowOf(false)
    override suspend fun setOnboardingCompleted(completed: Boolean) {}
    override fun isBiometricEnabled(): Flow<Boolean> = flowOf(false)
    override suspend fun setBiometricEnabled(enabled: Boolean) {}
}
