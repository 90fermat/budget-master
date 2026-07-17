package com.budgetmaster.auth.domain.usecase

import com.budgetmaster.auth.domain.model.AuthError
import com.budgetmaster.auth.domain.model.AuthException
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
 * Unit tests for [LoginUseCase].
 * Covers email/password validation and successful authentication delegation.
 */
class LoginUseCaseTest {

    private val fakeUser = User(id = "123", email = "test@example.com", displayName = "Test User", isEmailVerified = true)

    private val fakeRepo = object : AuthRepository {
        override fun getAuthStatus(): Flow<AuthStatus> = flowOf(AuthStatus.Unauthenticated)
        override suspend fun signIn(email: String, password: String): User = fakeUser
        override suspend fun signUp(email: String, password: String): User = fakeUser
        override suspend fun signInWithGoogle(idToken: String): User = fakeUser
        override suspend fun signOut() {}
        override suspend fun deleteAccount() {}
        override suspend fun sendPasswordReset(email: String) {}
        override fun isOnboardingCompleted(): Flow<Boolean> = flowOf(false)
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override fun isBiometricEnabled(): Flow<Boolean> = flowOf(false)
        override suspend fun setBiometricEnabled(enabled: Boolean) {}
    }

    private val useCase = LoginUseCase(fakeRepo)

    @Test
    fun `invoke with valid credentials returns user`() = runTest {
        val result = useCase("test@example.com", "password123")
        assertEquals(fakeUser, result)
    }

    @Test
    fun `invoke with blank email throws EmptyFields`() = runTest {
        val ex = assertFailsWith<AuthException> { useCase("", "password123") }
        assertEquals(AuthError.EmptyFields, ex.error)
    }

    @Test
    fun `invoke with malformed email throws InvalidEmail`() = runTest {
        val ex = assertFailsWith<AuthException> { useCase("not-an-email", "password123") }
        assertEquals(AuthError.InvalidEmail, ex.error)
    }

    @Test
    fun `invoke with short password throws WeakPassword`() = runTest {
        val ex = assertFailsWith<AuthException> { useCase("test@example.com", "123") }
        assertEquals(AuthError.WeakPassword, ex.error)
    }
}
