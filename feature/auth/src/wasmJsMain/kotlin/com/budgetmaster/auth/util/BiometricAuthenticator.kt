package com.budgetmaster.auth.util

/**
 * WasmJs actual implementation of [BiometricAuthenticator].
 * Biometric authentication is not supported in this environment yet.
 */
actual class BiometricAuthenticator actual constructor() {
    /**
     * Returns false as biometric hardware access is not standard in browsers.
     */
    actual fun isAvailable(): Boolean = false
}
