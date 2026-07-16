package com.budgetmaster.auth.presentation

import androidx.compose.runtime.Composable

/**
 * Returns a callback that starts the platform's Google sign-in flow.
 *
 * [onResult] receives the Google **ID token** on success, or a failure carrying an
 * [com.budgetmaster.auth.domain.model.AuthException] (e.g. `GoogleCancelled` when the user
 * dismisses the sheet). The token is then exchanged for a Firebase session by
 * [com.budgetmaster.auth.domain.usecase.SignInWithGoogleUseCase].
 */
@Composable
expect fun rememberGoogleSignInLauncher(onResult: (Result<String>) -> Unit): () -> Unit
