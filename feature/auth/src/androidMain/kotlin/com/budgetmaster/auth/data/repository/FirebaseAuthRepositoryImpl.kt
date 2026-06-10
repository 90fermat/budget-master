package com.budgetmaster.auth.data.repository

import com.budgetmaster.auth.domain.model.AuthStatus
import com.budgetmaster.auth.domain.model.User
import com.budgetmaster.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Android Firebase Auth repository implementation.
 * Full implementation integrates with Firebase Kotlin SDK.
 */
class FirebaseAuthRepositoryImpl : AuthRepository {

    override fun getAuthStatus(): Flow<AuthStatus> = flowOf(AuthStatus.Unauthenticated)

    override suspend fun signIn(email: String, password: String): User {
        // TODO: Integrate with Firebase Auth SDK
        throw UnsupportedOperationException("Firebase Auth not configured")
    }

    override suspend fun signUp(email: String, password: String): User {
        // TODO: Integrate with Firebase Auth SDK
        throw UnsupportedOperationException("Firebase Auth not configured")
    }

    override suspend fun signOut() {
        // TODO: Integrate with Firebase Auth SDK
    }

    override suspend fun sendPasswordReset(email: String) {
        // TODO: Integrate with Firebase Auth SDK
    }

    override fun isOnboardingCompleted(): Flow<Boolean> = flowOf(false)

    override suspend fun setOnboardingCompleted(completed: Boolean) {}

    override fun isBiometricEnabled(): Flow<Boolean> = flowOf(false)

    override suspend fun setBiometricEnabled(enabled: Boolean) {}
}
