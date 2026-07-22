package com.budgetmaster.core.backup

/**
 * Authenticated encryption for the backup file.
 *
 * Unlike the PIN hash — which is pure Kotlin precisely so it could be proven against published
 * vectors — this delegates to the platform's AES-GCM. Hand-writing a block cipher and a GHASH is a
 * far larger and more dangerous surface than a hash function, and the platform implementations are
 * the most heavily exercised code in the ecosystem. The key derivation stays ours: [Pbkdf2] is
 * already vector-checked, so the passphrase is stretched identically everywhere.
 *
 * Real on Android. The other platforms report [isSupported] false and the backup UI hides, the
 * same rule app lock follows — shipping untested crypto to a platform that cannot be exercised
 * here would be worse than shipping nothing.
 */
expect object BackupCrypto {
    /** False where no AES-GCM implementation is wired; the backup UI hides itself. */
    val isSupported: Boolean

    /**
     * Encrypts [plaintext] under a key derived from [key].
     *
     * @return ciphertext with its authentication tag appended, as the platform produces it.
     */
    fun encrypt(key: ByteArray, iv: ByteArray, plaintext: ByteArray): ByteArray

    /**
     * Decrypts, verifying the authentication tag.
     *
     * @return null when the tag does not verify — a wrong passphrase or a tampered file, which are
     *   deliberately indistinguishable to the caller because neither should restore anything.
     */
    fun decrypt(key: ByteArray, iv: ByteArray, ciphertext: ByteArray): ByteArray?
}
