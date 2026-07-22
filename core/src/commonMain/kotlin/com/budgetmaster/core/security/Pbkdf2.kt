package com.budgetmaster.core.security

/**
 * A pure-Kotlin PBKDF2-HMAC-SHA256, used to store the app-lock PIN as a salted, stretched hash
 * rather than in the clear.
 *
 * Pure Kotlin, in commonMain, on purpose: it is identical on every platform and — the part that
 * matters for security code — it can be proven correct on the host against published test vectors,
 * which a per-platform `javax.crypto` / CryptoKit split cannot be for the targets that only
 * cross-compile here. `Pbkdf2Test` pins it to the RFC 7914 vectors.
 *
 * On threat model: a numeric PIN has a tiny keyspace, so the iteration count is not what protects
 * it against offline brute force — nothing can, once the hash leaks. The real defences are keeping
 * the hash off disk backups (done in Phase 12) and rate-limiting attempts (done in the lock
 * controller). The salt and iterations here stop a *precomputed* attack and make each guess cost
 * real work; they are not asked to do more than that.
 */
object Pbkdf2 {

    /**
     * Derives [keyLengthBytes] bytes from [password] and [salt] over [iterations] rounds.
     */
    fun derive(
        password: ByteArray,
        salt: ByteArray,
        iterations: Int,
        keyLengthBytes: Int,
    ): ByteArray {
        require(iterations > 0) { "iterations must be positive" }
        require(keyLengthBytes > 0) { "keyLengthBytes must be positive" }

        val hLen = 32 // SHA-256 output size
        val blockCount = (keyLengthBytes + hLen - 1) / hLen
        val output = ByteArray(blockCount * hLen)

        for (block in 1..blockCount) {
            val t = deriveBlock(password, salt, iterations, block)
            t.copyInto(output, (block - 1) * hLen)
        }
        return output.copyOf(keyLengthBytes)
    }

    /** One PBKDF2 block: U1 = PRF(P, salt || INT(i)); Un = PRF(P, U(n-1)); T = U1 ^ … ^ Uc. */
    private fun deriveBlock(password: ByteArray, salt: ByteArray, iterations: Int, blockIndex: Int): ByteArray {
        val intBlock = byteArrayOf(
            (blockIndex ushr 24).toByte(),
            (blockIndex ushr 16).toByte(),
            (blockIndex ushr 8).toByte(),
            blockIndex.toByte(),
        )
        var u = HmacSha256.mac(password, salt + intBlock)
        val t = u.copyOf()
        for (round in 2..iterations) {
            u = HmacSha256.mac(password, u)
            for (i in t.indices) t[i] = (t[i].toInt() xor u[i].toInt()).toByte()
        }
        return t
    }
}
