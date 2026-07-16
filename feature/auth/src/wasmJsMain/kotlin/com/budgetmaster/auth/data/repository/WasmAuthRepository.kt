package com.budgetmaster.auth.data.repository

import com.budgetmaster.auth.domain.model.AuthStatus
import com.budgetmaster.auth.domain.model.User
import com.budgetmaster.auth.domain.repository.AuthRepository
import com.budgetmaster.core.prefs.KeyValueStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Web (Wasm) auth repository running in **local-only mode**.
 *
 * The GitLive Firebase SDK has no Wasm target, so instead of a throwing stub the web build
 * keeps a durable local profile in `localStorage` (via [KeyValueStore]). Sign-in/up create
 * or reuse that profile; credential shape is validated upstream in the use cases. This keeps
 * every auth path non-throwing and the app fully usable offline on the web, while the
 * account is scoped to the browser only (no remote sync).
 *
 * @param store Cross-platform key-value persistence (`localStorage` on Wasm).
 */
class WasmAuthRepository(
    private val store: KeyValueStore,
) : AuthRepository {

    override fun getAuthStatus(): Flow<AuthStatus> =
        store.observeString(KEY_EMAIL).map { email ->
            if (email.isNullOrBlank()) AuthStatus.Unauthenticated else AuthStatus.Authenticated(localUser(email))
        }

    override suspend fun signIn(email: String, password: String): User = persist(email)

    override suspend fun signUp(email: String, password: String): User = persist(email)

    override suspend fun signOut() {
        store.remove(KEY_EMAIL)
    }

    override suspend fun sendPasswordReset(email: String) {
        // Local-only mode: no email is actually sent; treated as a successful no-op.
    }

    override fun isOnboardingCompleted(): Flow<Boolean> = flowOf(false)
    override suspend fun setOnboardingCompleted(completed: Boolean) {}
    override fun isBiometricEnabled(): Flow<Boolean> = flowOf(false)
    override suspend fun setBiometricEnabled(enabled: Boolean) {}

    private suspend fun persist(email: String): User {
        val normalized = email.trim()
        store.putString(KEY_EMAIL, normalized)
        return localUser(normalized)
    }

    private fun localUser(email: String): User = User(
        id = "local_${email.lowercase().hashCode()}",
        email = email,
        displayName = email.substringBefore('@'),
        isEmailVerified = false,
    )

    private companion object {
        const val KEY_EMAIL = "auth.local_email"
    }
}
