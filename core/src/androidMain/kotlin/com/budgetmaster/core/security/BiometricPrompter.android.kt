package com.budgetmaster.core.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.budgetmaster.core.db.AppContextHolder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android biometrics via [BiometricPrompt].
 *
 * Reads the application context from [AppContextHolder] rather than expecting a caller to inject
 * it — the class this replaced took the context through a `setContext` method that nothing ever
 * called, so its availability check silently returned false forever.
 */
actual class BiometricPrompter actual constructor() {

    actual fun isAvailable(): Boolean {
        val context = runCatching { AppContextHolder.context }.getOrNull() ?: return false
        return BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    actual suspend fun authenticate(
        title: String,
        subtitle: String,
        cancelLabel: String,
    ): BiometricOutcome {
        // BiometricPrompt is hosted by a FragmentActivity; without a live one there is nothing to
        // show the sheet on, so the caller falls back to the PIN.
        val activity = CurrentActivityHolder.activity as? FragmentActivity
            ?: return BiometricOutcome.Unavailable
        if (!isAvailable()) return BiometricOutcome.Unavailable

        return suspendCancellableCoroutine { continuation ->
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (continuation.isActive) continuation.resume(BiometricOutcome.Success)
                    }

                    override fun onAuthenticationError(code: Int, message: CharSequence) {
                        // The user dismissing the sheet or tapping the PIN fallback is a choice,
                        // not a failure, so it must not read as one to the lock screen.
                        val outcome = when (code) {
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_USER_CANCELED,
                            BiometricPrompt.ERROR_CANCELED,
                            -> BiometricOutcome.Cancelled

                            BiometricPrompt.ERROR_NO_BIOMETRICS,
                            BiometricPrompt.ERROR_HW_NOT_PRESENT,
                            BiometricPrompt.ERROR_HW_UNAVAILABLE,
                            -> BiometricOutcome.Unavailable

                            else -> BiometricOutcome.Failed
                        }
                        if (continuation.isActive) continuation.resume(outcome)
                    }

                    // Deliberately not resumed: a single unrecognised finger leaves the sheet up
                    // so the user can try again. Only an error or success ends the prompt.
                    override fun onAuthenticationFailed() = Unit
                },
            )

            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(cancelLabel)
                .setAllowedAuthenticators(BIOMETRIC_STRONG)
                .setConfirmationRequired(false)
                .build()

            prompt.authenticate(info)
            continuation.invokeOnCancellation { prompt.cancelAuthentication() }
        }
    }
}
