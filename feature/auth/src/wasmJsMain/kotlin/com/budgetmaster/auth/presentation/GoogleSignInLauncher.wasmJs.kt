package com.budgetmaster.auth.presentation

import androidx.compose.runtime.Composable
import com.budgetmaster.auth.domain.model.AuthError
import com.budgetmaster.auth.domain.model.AuthException

/**
 * Unreachable on Web — [com.budgetmaster.auth.domain.isGoogleSignInSupported] is false, so the
 * Login screen hides the Google button. Reports unavailability rather than throwing.
 */
@Composable
actual fun rememberGoogleSignInLauncher(onResult: (Result<String>) -> Unit): () -> Unit = {
    onResult(Result.failure(AuthException(AuthError.GoogleUnavailable)))
}
