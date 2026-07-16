package com.budgetmaster.auth.data.repository

import com.budgetmaster.auth.domain.model.AuthError
import com.budgetmaster.auth.domain.model.AuthException
import com.budgetmaster.auth.domain.model.AuthStatus
import com.budgetmaster.auth.domain.model.User
import com.budgetmaster.auth.domain.repository.AuthRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException
import dev.gitlive.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * iOS Firebase Auth repository backed by the GitLive Firebase Kotlin SDK.
 *
 * Requires `FirebaseApp.configure()` at app launch (from `iosApp` with the bundled
 * `GoogleService-Info.plist`) before `Firebase.auth` is resolved.
 *
 * @param firebaseAuth Firebase Auth instance injected via Koin (`Firebase.auth`).
 */
class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth,
) : AuthRepository {

    override fun getAuthStatus(): Flow<AuthStatus> =
        firebaseAuth.authStateChanged.map { user ->
            if (user == null) AuthStatus.Unauthenticated else AuthStatus.Authenticated(user.toUser())
        }

    override suspend fun signIn(email: String, password: String): User = runMapping {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password)
        (result.user ?: throw AuthException(AuthError.InvalidCredentials)).toUser()
    }

    override suspend fun signUp(email: String, password: String): User = runMapping {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password)
        (result.user ?: throw AuthException(AuthError.Unknown)).toUser()
    }

    override suspend fun signOut() = runMapping { firebaseAuth.signOut() }

    override suspend fun sendPasswordReset(email: String) = runMapping {
        firebaseAuth.sendPasswordResetEmail(email)
    }

    override fun isOnboardingCompleted(): Flow<Boolean> = flowOf(false)
    override suspend fun setOnboardingCompleted(completed: Boolean) {}
    override fun isBiometricEnabled(): Flow<Boolean> = flowOf(false)
    override suspend fun setBiometricEnabled(enabled: Boolean) {}
}

private fun FirebaseUser.toUser(): User = User(
    id = uid,
    email = email ?: "",
    displayName = displayName,
    isEmailVerified = isEmailVerified,
)

/** Runs a Firebase call, translating known SDK failures into a typed [AuthException]. */
private inline fun <T> runMapping(block: () -> T): T =
    try {
        block()
    } catch (e: AuthException) {
        throw e
    } catch (e: FirebaseAuthWeakPasswordException) {
        throw AuthException(AuthError.WeakPassword, e)
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        throw AuthException(AuthError.InvalidCredentials, e)
    } catch (e: FirebaseAuthInvalidUserException) {
        throw AuthException(AuthError.UserNotFound, e)
    } catch (e: FirebaseAuthUserCollisionException) {
        throw AuthException(AuthError.EmailAlreadyInUse, e)
    } catch (e: Exception) {
        throw AuthException(if ((e.message ?: "").contains("network", true)) AuthError.Network else AuthError.Unknown, e)
    }
