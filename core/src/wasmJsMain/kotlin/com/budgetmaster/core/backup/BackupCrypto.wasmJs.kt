package com.budgetmaster.core.backup

/**
 * No AES-GCM is wired on this platform, so backup reports itself unsupported and the UI hides.
 * Deliberately not a hand-rolled fallback: weak-but-present encryption on a file the user believes
 * is protected would be worse than the feature being absent.
 */
actual object BackupCrypto {
    actual val isSupported: Boolean = false

    actual fun encrypt(key: ByteArray, iv: ByteArray, plaintext: ByteArray): ByteArray =
        error("Backup encryption is not available on this platform")

    actual fun decrypt(key: ByteArray, iv: ByteArray, ciphertext: ByteArray): ByteArray? = null
}
