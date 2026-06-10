package com.budgetmaster.auth.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG

/**
 * Android actual implementation of [BiometricAuthenticator].
 * Checks hardware availability and enrolled biometrics via [BiometricManager].
 */
actual class BiometricAuthenticator {
    private var context: Context? = null

    /**
     * Injects the application context required for Android biometric checks.
     */
    fun setContext(context: Context) {
        this.context = context
    }

    /**
     * Returns true if strong biometric authentication is available and enrolled.
     */
    actual fun isAvailable(): Boolean {
        val ctx = context ?: return false
        val manager = BiometricManager.from(ctx)
        return manager.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }
}
