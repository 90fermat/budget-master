package com.budgetmaster.core.security

/**
 * Cryptographically strong random bytes, for the PIN salt.
 *
 * App lock ships on Android, where the actual is `java.security.SecureRandom`. The iOS and web
 * actuals fall back to the platform default RNG; they exist so the shared code compiles, not to be
 * relied on, because app lock reports itself unsupported there (see [AppLockPlatform]).
 */
expect fun secureRandomBytes(size: Int): ByteArray
