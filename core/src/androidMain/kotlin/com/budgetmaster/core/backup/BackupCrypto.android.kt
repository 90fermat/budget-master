package com.budgetmaster.core.backup

import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val TAG_BITS = 128

actual object BackupCrypto {
    actual val isSupported: Boolean = true

    actual fun encrypt(key: ByteArray, iv: ByteArray, plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(plaintext)
    }

    actual fun decrypt(key: ByteArray, iv: ByteArray, ciphertext: ByteArray): ByteArray? = try {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        cipher.doFinal(ciphertext)
    } catch (e: GeneralSecurityException) {
        // A failing tag is the *expected* path for a wrong passphrase or an edited file, not an
        // exceptional one — so it returns null rather than throwing, and the caller cannot tell
        // the two apart, which is the point.
        null
    }
}
