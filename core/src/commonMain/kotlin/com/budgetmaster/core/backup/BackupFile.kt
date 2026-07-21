@file:OptIn(ExperimentalEncodingApi::class)

package com.budgetmaster.core.backup

import com.budgetmaster.core.security.Pbkdf2
import com.budgetmaster.core.security.secureRandomBytes
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Why a backup file could not be read. Distinguished so the UI can say something useful. */
sealed interface BackupReadError {
    /** Not a Budget Master backup, or corrupted beyond recognition. */
    data object NotABackup : BackupReadError

    /** Written by a newer app version whose format this one does not know. */
    data class UnsupportedVersion(val fileVersion: Int) : BackupReadError

    /** The passphrase did not decrypt it — or the file was altered. Indistinguishable by design. */
    data object WrongPassphrase : BackupReadError
}

/**
 * Serialises a [BackupEnvelope] to an encrypted, self-describing text file, and back.
 *
 * The layout is `BMBAK1.<base64 salt>.<base64 iv>.<base64 ciphertext>` — text rather than binary
 * so it survives being mailed to oneself, put in a notes app, or synced through a file service
 * that mangles line endings, which is realistically where people put their backups.
 *
 * The header is outside the ciphertext on purpose: reading it tells us the file is ours and which
 * format it is *before* asking the user for a passphrase, so a wrong-file mistake is reported as a
 * wrong file rather than as a wrong passphrase.
 *
 * The salt is per-file, so the same passphrase used twice produces unrelated keys and two backups
 * cannot be compared for equality.
 */
object BackupFile {

    private const val MAGIC = "BMBAK"
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BYTES = 32

    /**
     * High enough to make a guess expensive, low enough that a phone completes it while the user
     * watches a spinner. Recorded in the format version, not the file, so it can be raised.
     */
    private const val ITERATIONS = 210_000

    private val json = Json {
        // A file written by a newer build may carry fields this one does not know; ignoring them
        // lets an older app still restore what it does understand instead of refusing outright.
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Encrypts [envelope] under [passphrase]. */
    fun write(envelope: BackupEnvelope, passphrase: String): String {
        val salt = secureRandomBytes(SALT_BYTES)
        val iv = secureRandomBytes(IV_BYTES)
        val key = Pbkdf2.derive(passphrase.encodeToByteArray(), salt, ITERATIONS, KEY_BYTES)
        val plaintext = json.encodeToString(BackupEnvelope.serializer(), envelope).encodeToByteArray()
        val ciphertext = BackupCrypto.encrypt(key, iv, plaintext)

        return listOf(
            MAGIC + BackupEnvelope.CURRENT_FORMAT_VERSION,
            Base64.encode(salt),
            Base64.encode(iv),
            Base64.encode(ciphertext),
        ).joinToString(".")
    }

    /**
     * Decrypts and parses [content].
     *
     * @return the envelope, or a [BackupReadError] describing why not.
     */
    fun read(content: String, passphrase: String): Result<BackupEnvelope> {
        val parts = content.trim().split('.')
        if (parts.size != 4 || !parts[0].startsWith(MAGIC)) {
            return Result.failure(BackupException(BackupReadError.NotABackup))
        }
        val version = parts[0].removePrefix(MAGIC).toIntOrNull()
            ?: return Result.failure(BackupException(BackupReadError.NotABackup))
        if (version > BackupEnvelope.CURRENT_FORMAT_VERSION) {
            return Result.failure(BackupException(BackupReadError.UnsupportedVersion(version)))
        }

        val salt = parts[1].decodeBase64OrNull()
        val iv = parts[2].decodeBase64OrNull()
        val ciphertext = parts[3].decodeBase64OrNull()
        if (salt == null || iv == null || ciphertext == null) {
            return Result.failure(BackupException(BackupReadError.NotABackup))
        }

        val key = Pbkdf2.derive(passphrase.encodeToByteArray(), salt, ITERATIONS, KEY_BYTES)
        val plaintext = BackupCrypto.decrypt(key, iv, ciphertext)
            ?: return Result.failure(BackupException(BackupReadError.WrongPassphrase))

        return runCatching {
            json.decodeFromString(BackupEnvelope.serializer(), plaintext.decodeToString())
        }.recoverCatching {
            // Decryption succeeded but the contents are not an envelope: the tag verified, so this
            // is our file with our passphrase and something structural is wrong, not a bad guess.
            throw BackupException(BackupReadError.NotABackup)
        }
    }

    private fun String.decodeBase64OrNull(): ByteArray? = runCatching { Base64.decode(this) }.getOrNull()
}

/** Carries a [BackupReadError] out of a [Result]. */
class BackupException(val error: BackupReadError) : Exception(error.toString())
