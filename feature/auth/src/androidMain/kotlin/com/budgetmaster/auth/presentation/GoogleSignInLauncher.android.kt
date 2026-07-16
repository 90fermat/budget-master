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
import androidx.credentials.exceptions.NoCredentialException
import com.budgetmaster.auth.domain.model.AuthError
import com.budgetmaster.auth.domain.model.AuthException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
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
        scope.launch { onResult(requestGoogleIdToken(context)) }
        Unit
    }
}

private suspend fun requestGoogleIdToken(context: Context): Result<String> = try {
    val serverClientId = context.googleServerClientId()
        ?: throw AuthException(AuthError.GoogleUnavailable)
    val googleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(serverClientId)
        // Also offer accounts that have not yet been used with this app.
        .setFilterByAuthorizedAccounts(false)
        .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
    val credential = CredentialManager.create(context).getCredential(context, request).credential

    if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        Result.success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
    } else {
        Result.failure(AuthException(AuthError.GoogleUnavailable))
    }
} catch (e: AuthException) {
    Result.failure(e)
} catch (e: GetCredentialCancellationException) {
    Result.failure(AuthException(AuthError.GoogleCancelled, e))
} catch (e: NoCredentialException) {
    // No Google account on the device, or the app's SHA-1 is not registered in Firebase.
    Result.failure(AuthException(AuthError.GoogleUnavailable, e))
} catch (e: GetCredentialException) {
    Result.failure(AuthException(AuthError.GoogleUnavailable, e))
} catch (e: Exception) {
    Result.failure(AuthException(AuthError.Unknown, e))
}

/**
 * Resolves `default_web_client_id` at runtime, so `:feature:auth` needs no compile-time
 * dependency on the app module's generated `R` class.
 */
private fun Context.googleServerClientId(): String? {
    val resId = resources.getIdentifier("default_web_client_id", "string", packageName)
    return if (resId == 0) null else getString(resId)
}
