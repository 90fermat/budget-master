package com.budgetmaster.auth.domain.usecase

import com.budgetmaster.auth.domain.model.AuthStatus
import com.budgetmaster.auth.domain.model.User
import com.budgetmaster.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Unit tests for [SignUpUseCase].
 */
class RegisterUseCaseTest {

    private val fakeUser = User(id = "456", email = "new@example.com", displayName = null, isEmailVerified = false)

    private val fakeRepo = object : AuthRepository {
        override fun getAuthStatus(): Flow<AuthStatus> = flowOf(AuthStatus.Unauthenticated)
        override suspend fun signIn(email: String, password: String): User = fakeUser
        override suspend fun signUp(email: String, password: String): User = fakeUser
        override suspend fun signInWithGoogle(idToken: String): User = fakeUser
        override suspend fun signOut() {}
        override suspend fun sendPasswordReset(email: String) {}
        override fun isOnboardingCompleted(): Flow<Boolean> = flowOf(false)
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override fun isBiometricEnabled(): Flow<Boolean> = flowOf(false)
        override suspend fun setBiometricEnabled(enabled: Boolean) {}
    }

    private val useCase = SignUpUseCase(fakeRepo)

    @Test
    fun `invoke with valid credentials returns new user`() = runTest {
        val result = useCase("new@example.com", "securePass123")
        assertEquals(fakeUser, result)
    }

    @Test
    fun `invoke propagates exceptions from repository`() = runTest {
        val errorRepo = object : AuthRepository {
            override fun getAuthStatus(): Flow<AuthStatus> = flowOf(AuthStatus.Unauthenticated)
            override suspend fun signIn(email: String, password: String): User = throw RuntimeException("Auth error")
            override suspend fun signUp(email: String, password: String): User = throw RuntimeException("Registration failed")
            override suspend fun signInWithGoogle(idToken: String): User = fakeUser
            override suspend fun signOut() {}
            override suspend fun sendPasswordReset(email: String) {}
            override fun isOnboardingCompleted(): Flow<Boolean> = flowOf(false)
            override suspend fun setOnboardingCompleted(completed: Boolean) {}
            override fun isBiometricEnabled(): Flow<Boolean> = flowOf(false)
            override suspend fun setBiometricEnabled(enabled: Boolean) {}
        }
        val errorUseCase = SignUpUseCase(errorRepo)
        assertFailsWith<RuntimeException> {
            errorUseCase("new@example.com", "securePass123")
        }
    }
}
