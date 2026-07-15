package com.budgetmaster.core.util

/**
 * Whether the current platform can offer biometric authentication (fingerprint / Face ID).
 * `true` on Android and iOS, `false` on the Web — used to skip biometric setup on Wasm.
 */
expect val isBiometricAuthSupported: Boolean
