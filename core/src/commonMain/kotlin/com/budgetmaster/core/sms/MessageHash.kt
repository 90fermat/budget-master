package com.budgetmaster.core.sms

/**
 * A stable fingerprint for a provider message.
 *
 * Used so the importer can recognise a message it has already handled — providers re-send, a
 * notification and an SMS can describe the same event, and the user may re-share one by hand.
 *
 * Deliberately a hash and not the text: it is enough to say "seen this", while the message itself
 * — which names people, amounts and balances — is never written to disk.
 *
 * FNV-1a rather than a crypto hash: this guards against accidental reprocessing, not an adversary,
 * and it needs no dependency in `commonMain`. Collisions across one user's message history are
 * vanishingly unlikely at 64 bits, and the transaction-id check catches a collision anyway.
 */
fun messageFingerprint(sender: String, body: String, receivedAt: Long): String {
    var hash = FNV_OFFSET_BASIS
    "$sender|$body|$receivedAt".encodeToByteArray().forEach { byte ->
        hash = hash xor (byte.toLong() and 0xFF)
        hash *= FNV_PRIME
    }
    return hash.toULong().toString(16).padStart(16, '0')
}

private const val FNV_OFFSET_BASIS = -3750763034362895579L // 14695981039346656037 unsigned
private const val FNV_PRIME = 1099511628211L
