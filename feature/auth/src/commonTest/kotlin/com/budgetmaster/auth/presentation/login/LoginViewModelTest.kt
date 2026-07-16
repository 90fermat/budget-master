package com.budgetmaster.auth.presentation.login

import com.budgetmaster.auth.domain.model.AuthError
import com.budgetmaster.auth.domain.model.AuthException
import com.budgetmaster.auth.domain.model.AuthStatus
import com.budgetmaster.auth.domain.model.User
import com.budgetmaster.auth.domain.repository.AuthRepository
import com.budgetmaster.auth.domain.usecase.CheckBiometricSupportUseCase
import com.budgetmaster.auth.domain.usecase.LoginUseCase
import com.budgetmaster.auth.domain.usecase.SignInWithGoogleUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [LoginViewModel] covering all intents and state transitions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeUser = User(
        id = "user1",
        email = "user@example.com",
        displayName = "User",
        isEmailVerified = true
    )

    private val successRepo = object : AuthRepository {
        override fun getAuthStatus(): Flow<AuthStatus> = flowOf(AuthStatus.Unauthenticated)
        override suspend fun signIn(email: String, password: String): User = fakeUser
        override suspend fun signUp(email: String, password: String): User = fakeUser
        override suspend fun signInWithGoogle(idToken: String): User = fakeUser
        override suspend fun signOut() {}
        override suspend fun sendPasswordReset(email: String) {}
        override fun isOnboardingCompleted(): Flow<Boolean> = flowOf(true)
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override fun isBiometricEnabled(): Flow<Boolean> = flowOf(false)
        override suspend fun setBiometricEnabled(enabled: Boolean) {}
    }

    private val failureRepo = object : AuthRepository {
        override fun getAuthStatus(): Flow<AuthStatus> = flowOf(AuthStatus.Unauthenticated)
        override suspend fun signIn(email: String, password: String): User = throw RuntimeException("Invalid credentials")
        override suspend fun signUp(email: String, password: String): User = throw RuntimeException("Registration failed")
        override suspend fun signInWithGoogle(idToken: String): User = throw RuntimeException("Google failed")
        override suspend fun signOut() {}
        override suspend fun sendPasswordReset(email: String) {}
        override fun isOnboardingCompleted(): Flow<Boolean> = flowOf(false)
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override fun isBiometricEnabled(): Flow<Boolean> = flowOf(false)
        override suspend fun setBiometricEnabled(enabled: Boolean) {}
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty email and password`() = runTest {
        val vm = LoginViewModel(LoginUseCase(successRepo), CheckBiometricSupportUseCase(successRepo), SignInWithGoogleUseCase(successRepo))
        val state = vm.state.value
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `EmailChanged intent updates email in state`() = runTest {
        val vm = LoginViewModel(LoginUseCase(successRepo), CheckBiometricSupportUseCase(successRepo), SignInWithGoogleUseCase(successRepo))
        vm.onIntent(LoginIntent.EmailChanged("hello@test.com"))
        assertEquals("hello@test.com", vm.state.value.email)
    }

    @Test
    fun `PasswordChanged intent updates password in state`() = runTest {
        val vm = LoginViewModel(LoginUseCase(successRepo), CheckBiometricSupportUseCase(successRepo), SignInWithGoogleUseCase(successRepo))
        vm.onIntent(LoginIntent.PasswordChanged("secret123"))
        assertEquals("secret123", vm.state.value.password)
    }

    @Test
    fun `LoginClicked with valid credentials emits NavigateToHome effect`() = runTest {
        val vm = LoginViewModel(LoginUseCase(successRepo), CheckBiometricSupportUseCase(successRepo), SignInWithGoogleUseCase(successRepo))
        val effects = mutableListOf<LoginEffect>()
        val job = launch { vm.effects.toList(effects) }

        vm.onIntent(LoginIntent.EmailChanged("user@example.com"))
        vm.onIntent(LoginIntent.PasswordChanged("password123"))
        vm.onIntent(LoginIntent.LoginClicked)
        advanceUntilIdle()

        assertTrue(effects.any { it is LoginEffect.NavigateToHome })
        job.cancel()
    }

    @Test
    fun `LoginClicked with invalid email sets errorMessage in state`() = runTest {
        val vm = LoginViewModel(LoginUseCase(successRepo), CheckBiometricSupportUseCase(successRepo), SignInWithGoogleUseCase(successRepo))
        vm.onIntent(LoginIntent.EmailChanged("not-an-email"))
        vm.onIntent(LoginIntent.PasswordChanged("password123"))
        vm.onIntent(LoginIntent.LoginClicked)
        advanceUntilIdle()

        assertEquals(AuthError.InvalidEmail, vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `LoginClicked with server error sets Unknown error in state`() = runTest {
        val vm = LoginViewModel(LoginUseCase(failureRepo), CheckBiometricSupportUseCase(failureRepo), SignInWithGoogleUseCase(failureRepo))
        vm.onIntent(LoginIntent.EmailChanged("user@example.com"))
        vm.onIntent(LoginIntent.PasswordChanged("password123"))
        vm.onIntent(LoginIntent.LoginClicked)
        advanceUntilIdle()

        assertEquals(AuthError.Unknown, vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `LoginClicked maps typed AuthException to state error`() = runTest {
        val invalidCredsRepo = object : AuthRepository {
            override fun getAuthStatus(): Flow<AuthStatus> = flowOf(AuthStatus.Unauthenticated)
            override suspend fun signIn(email: String, password: String): User =
                throw AuthException(AuthError.InvalidCredentials)
            override suspend fun signUp(email: String, password: String): User = fakeUser
            override suspend fun signInWithGoogle(idToken: String): User = fakeUser
            override suspend fun signOut() {}
            override suspend fun sendPasswordReset(email: String) {}
            override fun isOnboardingCompleted(): Flow<Boolean> = flowOf(false)
            override suspend fun setOnboardingCompleted(completed: Boolean) {}
            override fun isBiometricEnabled(): Flow<Boolean> = flowOf(false)
            override suspend fun setBiometricEnabled(enabled: Boolean) {}
        }
        val vm = LoginViewModel(LoginUseCase(invalidCredsRepo), CheckBiometricSupportUseCase(invalidCredsRepo), SignInWithGoogleUseCase(invalidCredsRepo))
        vm.onIntent(LoginIntent.EmailChanged("user@example.com"))
        vm.onIntent(LoginIntent.PasswordChanged("password123"))
        vm.onIntent(LoginIntent.LoginClicked)
        advanceUntilIdle()

        assertEquals(AuthError.InvalidCredentials, vm.state.value.error)
    }

    @Test
    fun `GoogleIdTokenReceived signs in and emits NavigateToHome`() = runTest {
        val vm = LoginViewModel(LoginUseCase(successRepo), CheckBiometricSupportUseCase(successRepo), SignInWithGoogleUseCase(successRepo))
        val effects = mutableListOf<LoginEffect>()
        val job = launch { vm.effects.toList(effects) }

        vm.onIntent(LoginIntent.GoogleIdTokenReceived("a-google-id-token"))
        advanceUntilIdle()

        assertTrue(effects.any { it is LoginEffect.NavigateToHome })
        assertNull(vm.state.value.error)
        job.cancel()
    }

    @Test
    fun `GoogleIdTokenReceived surfaces backend failure as Unknown`() = runTest {
        val vm = LoginViewModel(LoginUseCase(failureRepo), CheckBiometricSupportUseCase(failureRepo), SignInWithGoogleUseCase(failureRepo))
        vm.onIntent(LoginIntent.GoogleIdTokenReceived("a-google-id-token"))
        advanceUntilIdle()

        assertEquals(AuthError.Unknown, vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `GoogleIdTokenReceived with a blank token reports GoogleUnavailable`() = runTest {
        val vm = LoginViewModel(LoginUseCase(successRepo), CheckBiometricSupportUseCase(successRepo), SignInWithGoogleUseCase(successRepo))
        vm.onIntent(LoginIntent.GoogleIdTokenReceived("  "))
        advanceUntilIdle()

        assertEquals(AuthError.GoogleUnavailable, vm.state.value.error)
    }

    @Test
    fun `GoogleSignInFailed with cancellation is not surfaced as an error`() = runTest {
        val vm = LoginViewModel(LoginUseCase(successRepo), CheckBiometricSupportUseCase(successRepo), SignInWithGoogleUseCase(successRepo))
        vm.onIntent(LoginIntent.GoogleSignInFailed(AuthError.GoogleCancelled))
        advanceUntilIdle()

        assertNull(vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `GoogleSignInFailed with a real error is surfaced`() = runTest {
        val vm = LoginViewModel(LoginUseCase(successRepo), CheckBiometricSupportUseCase(successRepo), SignInWithGoogleUseCase(successRepo))
        vm.onIntent(LoginIntent.GoogleSignInFailed(AuthError.GoogleUnavailable))
        advanceUntilIdle()

        assertEquals(AuthError.GoogleUnavailable, vm.state.value.error)
    }

    @Test
    fun `TogglePasswordVisibility flips the flag`() = runTest {
        val vm = LoginViewModel(LoginUseCase(successRepo), CheckBiometricSupportUseCase(successRepo), SignInWithGoogleUseCase(successRepo))
        assertFalse(vm.state.value.isPasswordVisible)
        vm.onIntent(LoginIntent.TogglePasswordVisibility)
        assertTrue(vm.state.value.isPasswordVisible)
        vm.onIntent(LoginIntent.TogglePasswordVisibility)
        assertFalse(vm.state.value.isPasswordVisible)
    }

    @Test
    fun `NavigateToRegister intent emits NavigateToRegister effect`() = runTest {
        val vm = LoginViewModel(LoginUseCase(successRepo), CheckBiometricSupportUseCase(successRepo), SignInWithGoogleUseCase(successRepo))
        val effects = mutableListOf<LoginEffect>()
        val job = launch { vm.effects.toList(effects) }

        vm.onIntent(LoginIntent.NavigateToRegister)
        advanceUntilIdle()

        assertTrue(effects.any { it is LoginEffect.NavigateToRegister })
        job.cancel()
    }

    @Test
    fun `NavigateToForgotPassword intent emits NavigateToForgotPassword effect`() = runTest {
        val vm = LoginViewModel(LoginUseCase(successRepo), CheckBiometricSupportUseCase(successRepo), SignInWithGoogleUseCase(successRepo))
        val effects = mutableListOf<LoginEffect>()
        val job = launch { vm.effects.toList(effects) }

        vm.onIntent(LoginIntent.NavigateToForgotPassword)
        advanceUntilIdle()

        assertTrue(effects.any { it is LoginEffect.NavigateToForgotPassword })
        job.cancel()
    }
}
