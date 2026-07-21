package com.budgetmaster.core.security

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the hand-written SHA-256 / HMAC / PBKDF2 to published test vectors.
 *
 * Security primitives are the one place "it compiles and looks right" is not good enough. These
 * are the canonical vectors (FIPS 180-4 for SHA-256, RFC 4231 for HMAC-SHA256, RFC 7914 §11 for
 * PBKDF2-HMAC-SHA256); matching them is proof of correctness, not evidence of it, and it holds on
 * every platform because the implementation is one shared body of Kotlin.
 */
class CryptoVectorsTest {

    private fun ByteArray.hex(): String = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    private fun String.utf8() = encodeToByteArray()

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { ((hex[it * 2].digitToInt(16) shl 4) or hex[it * 2 + 1].digitToInt(16)).toByte() }

    @Test
    fun `sha256 known digests`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            Sha256.digest("".utf8()).hex(),
        )
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Sha256.digest("abc".utf8()).hex(),
        )
        // A message longer than one block, to exercise the multi-block path.
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            Sha256.digest("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".utf8()).hex(),
        )
    }

    @Test
    fun `hmac-sha256 rfc 4231 case 1`() {
        val key = ByteArray(20) { 0x0b }
        assertEquals(
            "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7",
            HmacSha256.mac(key, "Hi There".utf8()).hex(),
        )
    }

    @Test
    fun `hmac-sha256 rfc 4231 case 2`() {
        assertEquals(
            "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
            HmacSha256.mac("Jefe".utf8(), "what do ya want for nothing?".utf8()).hex(),
        )
    }

    @Test
    fun `pbkdf2-hmac-sha256 rfc 7914 c=1`() {
        val dk = Pbkdf2.derive("passwd".utf8(), "salt".utf8(), iterations = 1, keyLengthBytes = 64)
        assertEquals(
            "55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc" +
                "49ca9cccf179b645991664b39d77ef317c71b845b1e30bd509112041d3a19783",
            dk.hex(),
        )
    }

    @Test
    fun `pbkdf2-hmac-sha256 multi-round vector`() {
        // A moderate iteration count, still a published vector, exercising the U-xor loop.
        val dk = Pbkdf2.derive("password".utf8(), "salt".utf8(), iterations = 2, keyLengthBytes = 32)
        assertEquals(
            "ae4d0c95af6b46d32d0adff928f06dd02a303f8ef3c251dfd6e2d85a95474c43",
            dk.hex(),
        )
    }
}
