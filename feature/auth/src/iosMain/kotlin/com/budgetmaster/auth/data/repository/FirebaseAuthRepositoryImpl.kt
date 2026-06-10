package com.budgetmaster.auth.data.repository

import com.budgetmaster.auth.domain.model.AuthStatus
import com.budgetmaster.auth.domain.model.User
import com.budgetmaster.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * iOS Firebase Auth repository implementation.
 */
class FirebaseAuthRepositoryImpl : AuthRepository {

    override fun getAuthStatus(): Flow<AuthStatus> = flowOf(AuthStatus.Unauthenticated)

    override suspend fun signIn(email: String, password: String): User {
        throw UnsupportedOperationException("Firebase Auth not configured for iOS")
    }

    override suspend fun signUp(email: String, password: String): User {
        throw UnsupportedOperationException("Firebase Auth not configured for iOS")
    }

    override suspend fun signOut() {}

    override suspend fun sendPasswordReset(email: String) {}

    override fun isOnboardingCompleted(): Flow<Boolean> = flowOf(false)

    override suspend fun setOnboardingCompleted(completed: Boolean) {}

    override fun isBiometricEnabled(): Flow<Boolean> = flowOf(false)

    override suspend fun setBiometricEnabled(enabled: Boolean) {}
}
