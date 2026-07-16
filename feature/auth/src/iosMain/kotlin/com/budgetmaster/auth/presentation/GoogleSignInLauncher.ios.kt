package com.budgetmaster.auth.presentation

import androidx.compose.runtime.Composable
import com.budgetmaster.auth.domain.model.AuthError
import com.budgetmaster.auth.domain.model.AuthException

/**
 * Unreachable while [com.budgetmaster.auth.domain.isGoogleSignInSupported] is false on iOS —
 * the Login screen hides the Google button. Reports unavailability rather than throwing.
 */
@Composable
actual fun rememberGoogleSignInLauncher(onResult: (Result<String>) -> Unit): () -> Unit = {
    onResult(Result.failure(AuthException(AuthError.GoogleUnavailable)))
}
