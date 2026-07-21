package com.budgetmaster.core.security

/**
 * Whether app lock (biometric / PIN gate) is available on this platform.
 *
 * True on Android, where a released app runs behind Play Integrity and biometrics exist. False on
 * iOS (blocked on tooling) and web (a browser tab where a client-side PIN protects little and the
 * app is already a local-only profile). Settings hides the whole app-lock section where this is
 * false, the same way SMS import and biometrics already hide themselves.
 */
expect val isAppLockSupported: Boolean
