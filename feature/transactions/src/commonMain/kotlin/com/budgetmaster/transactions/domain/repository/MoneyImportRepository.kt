package com.budgetmaster.transactions.domain.repository

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
    )
}
