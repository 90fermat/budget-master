@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.transactions.domain.usecase

import com.budgetmaster.core.sms.MoneyMessageParser
import com.budgetmaster.core.sms.MoneyMessageType
import com.budgetmaster.core.sms.ParsedMoneyMessage
import com.budgetmaster.core.sms.messageFingerprint
import com.budgetmaster.core.util.DateUtils
import com.budgetmaster.transactions.domain.repository.ImportStatus
import com.budgetmaster.transactions.domain.repository.ImportedEntry
import com.budgetmaster.transactions.domain.repository.MoneyImportRepository
import com.budgetmaster.transactions.domain.repository.PendingImportDetails
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlin.time.ExperimentalTime

/** What happened to a message the importer was handed. */
sealed class ImportOutcome {
    /** Entries were written. */
    data class Imported(val transactionIds: List<String>) : ImportOutcome()

    /** This exact message was already processed. */
    data object AlreadySeen : ImportOutcome()

    /** This provider transaction is already in the ledger, from an earlier copy of the message. */
    data class AlreadyRecorded(val transactionId: String) : ImportOutcome()

    /**
     * A hand-entered transaction looks like the same event. Surfaced rather than merged or
     * double-counted: guessing wrong either way corrupts the ledger silently.
     */
    data class NeedsReview(val existingTransactionId: String) : ImportOutcome()

    /** Not a transaction message — an advert, a balance check, an OTP, an unknown provider. */
    data object NotRecognised : ImportOutcome()
}

/**
 * Turns a provider message into ledger entries, exactly once.
 *
 * Three dedup layers, cheapest first: the message fingerprint (have we handled this text?), the
 * provider transaction id (is this event already recorded, perhaps from a re-send?), and finally a
 * same-day same-amount check against hand-entered rows (did the user beat us to it?).
 *
 * **Fees become their own entry.** An Orange transfer of 20 244 with 44.48 in fees moves 20 288.48
 * out of the account; recording that single figure would both hide the fee and make it
 * unrecoverable later. Two rows keep the balance reconciling *and* make "what did mobile money
 * cost me this month" answerable — which in XAF/NGN markets is a question worth answering.
 */
class ImportMoneyMessageUseCase(
    private val repository: MoneyImportRepository,
    private val parsers: List<MoneyMessageParser>,
) {
    suspend operator fun invoke(
        sender: String,
        body: String,
        receivedAt: Long,
        accountId: String,
        ownerMsisdns: Set<String>,
    ): ImportOutcome {
        val hash = messageFingerprint(sender, body, receivedAt)
        if (repository.hasSeenMessage(hash)) return ImportOutcome.AlreadySeen

        // Sender-first. The allowlist is a privacy guarantee, not an optimisation: a message from
        // an unrecognised sender is never inspected, so ordinary SMS are not read.
        //
        // The one exception is a message with *no* sender, which only happens when the user
        // pasted or shared it in deliberately. There is no allowlist to apply, and they have
        // already chosen to hand us this text, so the body decides which parser can read it.
        val bySender = parsers.firstOrNull { it.handlesSender(sender) }
        var parser = bySender
        var parsed = bySender?.parse(body, ownerMsisdns, receivedAt)
        if (parsed == null && sender.isBlank()) {
            for (candidate in parsers) {
                val attempt = candidate.parse(body, ownerMsisdns, receivedAt) ?: continue
                parser = candidate
                parsed = attempt
                break
            }
        }
        if (parsed == null) {
            // Recorded so an unparseable message is not re-examined on every future pass.
            repository.recordMessageOutcome(hash, parser?.provider ?: UNKNOWN_PROVIDER, sender, receivedAt, ImportStatus.IGNORED)
            return ImportOutcome.NotRecognised
        }

        repository.findTransactionIdByExternalId(parsed.externalId)?.let { existing ->
            repository.recordMessageOutcome(
                hash, parsed.provider, sender, receivedAt,
                ImportStatus.DUPLICATE, parsed.externalId, existing,
            )
            return ImportOutcome.AlreadyRecorded(existing)
        }

        val occurredAt = parsed.occurredAt ?: receivedAt
        val (dayStart, dayEnd) = dayBounds(occurredAt)
        repository.findPossibleManualDuplicate(parsed.amount, dayStart, dayEnd)?.let { existing ->
            repository.recordMessageOutcome(
                hash, parsed.provider, sender, receivedAt,
                ImportStatus.PENDING_REVIEW, parsed.externalId, existing,
                pending = PendingImportDetails(
                    accountId = accountId,
                    amount = if (parsed.isOutflow) -parsed.amount else parsed.amount,
                    fee = parsed.fee,
                    description = parsed.describe(),
                    occurredAt = occurredAt,
                ),
            )
            return ImportOutcome.NeedsReview(existing)
        }

        val ids = repository.saveImported(
            hash = hash,
            provider = parsed.provider,
            sender = sender,
            receivedAt = receivedAt,
            externalId = parsed.externalId,
            entries = parsed.toEntries(accountId, occurredAt),
        )
        return ImportOutcome.Imported(ids)
    }

    /** The principal, plus a separate fee row when the provider charged one. */
    private fun ParsedMoneyMessage.toEntries(accountId: String, occurredAt: Long): List<ImportedEntry> =
        ImportEntryFactory.build(
            accountId = accountId,
            amount = if (isOutflow) -amount else amount,
            fee = fee,
            description = describe(),
            occurredAt = occurredAt,
            externalId = externalId,
        )

    private fun ParsedMoneyMessage.describe(): String =
        counterpartyName?.takeIf { it.isNotBlank() } ?: when (type) {
            MoneyMessageType.TRANSFER_IN -> "Transfer received"
            MoneyMessageType.TRANSFER_OUT -> "Transfer sent"
            MoneyMessageType.PAYMENT -> "Payment"
            MoneyMessageType.WITHDRAWAL -> "Cash withdrawal"
            MoneyMessageType.DEPOSIT -> "Cash deposit"
        }

    /** Midnight-to-midnight around [timestamp], for the fuzzy duplicate window. */
    private fun dayBounds(timestamp: Long): Pair<Long, Long> {
        val zone = TimeZone.currentSystemDefault()
        val day = DateUtils.toLocalDate(timestamp)
        val start = day.atStartOfDayIn(zone).toEpochMilliseconds()
        val end = day.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds() - 1
        return start to end
    }

    private companion object {
        const val UNKNOWN_PROVIDER = "unknown"
    }
}
