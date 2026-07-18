package com.budgetmaster.transactions.domain.repository

import kotlinx.coroutines.flow.Flow

/** How a seen message was resolved. Mirrors `ImportedMessageEntity.status`. */
enum class ImportStatus { IMPORTED, PENDING_REVIEW, IGNORED, DUPLICATE }

/** One ledger row to write for an imported message. */
data class ImportedEntry(
    val accountId: String,
    val categoryId: String?,
    /** Signed: negative for money out. */
    val amount: Double,
    val description: String,
    val timestamp: Long,
    /** The provider transaction id; unique across the table. */
    val externalId: String,
)

/**
 * The parsed content of a message that is waiting on the user's answer.
 *
 * Message bodies are never stored, so a pending review has to keep the parsed fields or the
 * question becomes unanswerable: if the user says "no, that is a different transaction", there
 * would be nothing left to create it from. These are the same aggregates the resulting entry
 * would hold, and they are dropped again as soon as the review is resolved.
 */
data class PendingImportDetails(
    val accountId: String,
    /** Signed, principal only. */
    val amount: Double,
    /** Provider fee, 0.0 when none — it becomes its own entry, as at first import. */
    val fee: Double,
    val description: String,
    val occurredAt: Long,
)

/** A row in the review queue: what we parsed, and the entry it might duplicate. */
data class PendingImport(
    val hash: String,
    val provider: String,
    val receivedAt: Long,
    val externalId: String,
    /** The hand-entered transaction that looks like the same event. */
    val existingTransactionId: String,
    val details: PendingImportDetails,
)

/**
 * Persistence for mobile-money message import: the messages seen, and the entries they became.
 *
 * Separate from `TransactionRepository` because the concerns are different — this one exists to
 * make importing *idempotent* and auditable, which normal transaction CRUD has no reason to know
 * about.
 */
interface MoneyImportRepository {
    /** True if this exact message was already processed, whatever the outcome was. */
    suspend fun hasSeenMessage(hash: String): Boolean

    /** The id of the transaction already recorded for [externalId], or null. */
    suspend fun findTransactionIdByExternalId(externalId: String): String?

    /**
     * Looks for a hand-entered transaction that looks like the same event — same day, same
     * magnitude, no provider id of its own.
     *
     * @return the matching transaction id, or null.
     */
    suspend fun findPossibleManualDuplicate(amount: Double, dayStart: Long, dayEnd: Long): String?

    /** Writes [entries] and records the message as handled, in one transaction. */
    suspend fun saveImported(
        hash: String,
        provider: String,
        sender: String,
        receivedAt: Long,
        externalId: String,
        entries: List<ImportedEntry>,
    ): List<String>

    /** Records a message that produced no entries (unrecognised, duplicate, or awaiting review). */
    suspend fun recordMessageOutcome(
        hash: String,
        provider: String,
        sender: String,
        receivedAt: Long,
        status: ImportStatus,
        externalId: String? = null,
        transactionId: String? = null,
        /** Required for [ImportStatus.PENDING_REVIEW]; ignored otherwise. */
        pending: PendingImportDetails? = null,
    )

    /** The review queue, newest first. Empty for the overwhelming majority of users. */
    fun observePendingReview(): Flow<List<PendingImport>>

    /**
     * Answers a pending review.
     *
     * @param keep true when the message is a genuinely separate event, which writes its entries;
     *   false when it is the same event the user already entered, which records it as a duplicate
     *   and writes nothing.
     * @return the ids of any entries written.
     */
    suspend fun resolvePending(hash: String, keep: Boolean): List<String>
}
