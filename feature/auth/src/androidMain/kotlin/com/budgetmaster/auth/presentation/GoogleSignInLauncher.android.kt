package com.budgetmaster.auth.presentation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.budgetmaster.auth.domain.model.AuthError
import com.budgetmaster.auth.domain.model.AuthException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Android Google sign-in via Credential Manager.
 *
 * Uses the OAuth **web client id** (`default_web_client_id`, generated into resources by the
 * `google-services` plugin from `google-services.json`) as the server client id, so the
 * returned ID token is minted for the Firebase backend.
 */
@Composable
actual fun rememberGoogleSignInLauncher(onResult: (Result<String>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return {
        // Unit-coerced by the declared `() -> Unit` return type; the launch's Job is not needed.
        scope.launch { onResult(requestGoogleIdToken(context)) }
    }
}

/**
 * Asks Credential Manager for a Google ID token.
 *
 * Two things here are deliberate, and both come from the same reported bug: the first tap failed
 * with "Google sign-in is not available on this device" and the second tap worked.
 *
 * **Authorized accounts first, then all accounts.** A single request with
 * `setFilterByAuthorizedAccounts(false)` makes the Google ID provider cold-start and enumerate
 * every account on the device, which is the call most likely to fail the first time. Asking for
 * already-authorized accounts first is the documented order: it is the fast path for returning
 * users, and only falls back to the full enumeration when there is genuinely nothing to offer.
 *
 * **One retry on the transient failures.** `GetCredentialUnknownException` and
 * `GetCredentialInterruptedException` on a cold provider are exactly the "worked the second time"
 * case, so the second time now happens without the user having to think about it.
 */
private suspend fun requestGoogleIdToken(context: Context): Result<String> = try {
    val serverClientId = context.googleServerClientId()
        // Distinct from a transient failure: this is a build/config fault the user cannot retry
        // their way out of, and it must not masquerade as one.
        ?: throw AuthException(AuthError.GoogleUnavailable)

    val credentialManager = CredentialManager.create(context)
    val token = try {
        credentialManager.requestToken(context, serverClientId, authorizedOnly = true)
    } catch (e: NoCredentialException) {
        // Nothing already linked to this app - offer every Google account on the device.
        credentialManager.requestToken(context, serverClientId, authorizedOnly = false)
    }
    Result.success(token)
} catch (e: AuthException) {
    Result.failure(e)
} catch (e: GetCredentialCancellationException) {
    Result.failure(AuthException(AuthError.GoogleCancelled, e))
} catch (e: NoCredentialException) {
    // Genuinely nothing to sign in with: no Google account on the device, or the app's SHA-1 is
    // not registered in the Firebase project.
    Result.failure(AuthException(AuthError.GoogleUnavailable, e))
} catch (e: GetCredentialProviderConfigurationException) {
    Result.failure(AuthException(AuthError.GoogleUnavailable, e))
} catch (e: GetCredentialUnsupportedException) {
    Result.failure(AuthException(AuthError.GoogleUnavailable, e))
} catch (e: GetCredentialException) {
    // Everything else from Credential Manager is treated as retryable rather than terminal.
    // The old code mapped this whole branch to GoogleUnavailable, which is what told users with a
    // perfectly good Google account that their device did not support Google sign-in.
    Result.failure(AuthException(AuthError.GoogleTransient, e))
} catch (e: Exception) {
    Result.failure(AuthException(AuthError.Unknown, e))
}

/** One attempt, plus one retry if the provider failed in a way that tends to resolve itself. */
private suspend fun CredentialManager.requestToken(
    context: Context,
    serverClientId: String,
    authorizedOnly: Boolean,
): String {
    val option = GetGoogleIdOption.Builder()
        .setServerClientId(serverClientId)
        .setFilterByAuthorizedAccounts(authorizedOnly)
        .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

    val credential = try {
        getCredential(context, request).credential
    } catch (e: GetCredentialUnknownException) {
        delay(RETRY_DELAY_MS)
        getCredential(context, request).credential
    } catch (e: GetCredentialInterruptedException) {
        delay(RETRY_DELAY_MS)
        getCredential(context, request).credential
    }

    if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
    // A credential of an unexpected type is a configuration problem, not something to retry.
    throw AuthException(AuthError.GoogleUnavailable)
}

/** Long enough for the provider to finish starting, short enough not to feel like a hang. */
private const val RETRY_DELAY_MS = 300L

/**
 * Resolves `default_web_client_id` at runtime, so `:feature:auth` needs no compile-time
 * dependency on the app module's generated `R` class.
 *
 * Note this looks the resource up under [Context.getPackageName], which is the *applicationId*.
 * They coincide today only because no build type sets an `applicationIdSuffix`; adding one would
 * make this silently return null and surface as GoogleUnavailable.
 */
private fun Context.googleServerClientId(): String? {
    val resId = resources.getIdentifier("default_web_client_id", "string", packageName)
    return if (resId == 0) null else getString(resId)
}
