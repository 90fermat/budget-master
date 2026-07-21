package com.budgetmaster.core.security

/**
 * Hashes and verifies the app-lock PIN.
 *
 * Stored form is `v1:iterations:saltHex:hashHex` — a versioned string so the parameters can be
 * raised later without invalidating existing PINs by guesswork. Verification is constant-time, so
 * a comparison cannot leak how much of the hash matched.
 *
 * The PIN is never stored, only this derived value, and it never leaves the device.
 */
object PinHasher {

    private const val VERSION = "v1"
    private const val ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val KEY_BYTES = 32

    /** Derives a fresh salted hash for [pin]. */
    fun hash(pin: String): String {
        val salt = secureRandomBytes(SALT_BYTES)
        val derived = Pbkdf2.derive(pin.encodeToByteArray(), salt, ITERATIONS, KEY_BYTES)
        return "$VERSION:$ITERATIONS:${salt.toHex()}:${derived.toHex()}"
    }

    /**
     * Whether [pin] matches [stored]. False for a malformed or unknown-version record rather than
     * throwing, so a corrupted preference can never crash the unlock screen — it just fails to
     * match, and the user re-sets the PIN.
     */
    fun verify(pin: String, stored: String): Boolean {
        val parts = stored.split(':')
        if (parts.size != 4 || parts[0] != VERSION) return false
        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = parts[2].hexToBytesOrNull() ?: return false
        val expected = parts[3].hexToBytesOrNull() ?: return false

        val derived = Pbkdf2.derive(pin.encodeToByteArray(), salt, iterations, expected.size)
        return constantTimeEquals(derived, expected)
    }

    /** XOR-accumulate so timing does not depend on where the first difference is. */
    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    private fun String.hexToBytesOrNull(): ByteArray? {
        if (length % 2 != 0) return null
        return runCatching {
            ByteArray(length / 2) { ((this[it * 2].digitToInt(16) shl 4) or this[it * 2 + 1].digitToInt(16)).toByte() }
        }.getOrNull()
    }
}
