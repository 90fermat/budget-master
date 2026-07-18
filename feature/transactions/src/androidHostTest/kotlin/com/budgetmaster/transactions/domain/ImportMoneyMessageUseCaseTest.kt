@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.transactions.domain

import com.budgetmaster.core.sms.OrangeMoneyParser
import com.budgetmaster.transactions.domain.repository.ImportStatus
import com.budgetmaster.transactions.domain.repository.ImportedEntry
import com.budgetmaster.transactions.domain.repository.MoneyImportRepository
import com.budgetmaster.transactions.domain.usecase.ImportMoneyMessageUseCase
import com.budgetmaster.transactions.domain.usecase.ImportOutcome
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * In-memory stand-in that records what the importer asked it to persist.
 *
 * A fake rather than a real database because what matters here is the *decisions* — which dedup
 * layer fired, and what rows were produced — not SQL.
 */
private class FakeImportRepository : MoneyImportRepository {
    val seenHashes = mutableSetOf<String>()
    val byExternalId = mutableMapOf<String, String>()
    var manualDuplicateId: String? = null

    val savedEntries = mutableListOf<ImportedEntry>()
    val outcomes = mutableListOf<ImportStatus>()

    override suspend fun hasSeenMessage(hash: String) = hash in seenHashes

    override suspend fun findTransactionIdByExternalId(externalId: String) = byExternalId[externalId]

    override suspend fun findPossibleManualDuplicate(amount: Double, dayStart: Long, dayEnd: Long) =
        manualDuplicateId

    override suspend fun saveImported(
        hash: String,
        provider: String,
        sender: String,
        receivedAt: Long,
        externalId: String,
        entries: List<ImportedEntry>,
    ): List<String> {
        seenHashes += hash
        savedEntries += entries
        outcomes += ImportStatus.IMPORTED
        entries.forEach { byExternalId[it.externalId] = "tx_${it.externalId}" }
        return entries.map { "tx_${it.externalId}" }
    }

    override suspend fun recordMessageOutcome(
        hash: String,
        provider: String,
        sender: String,
        receivedAt: Long,
        status: ImportStatus,
        externalId: String?,
        transactionId: String?,
    ) {
        seenHashes += hash
        outcomes += status
    }
}

class ImportMoneyMessageUseCaseTest {

    private val sender = "OrangeMoney"
    private val account = "acc_momo"
    private val owner = setOf("659228030")
    private val receivedAt = 1_752_753_600_000L

    private fun useCase(repo: MoneyImportRepository) =
        ImportMoneyMessageUseCase(repo, listOf(OrangeMoneyParser()))

    private val outgoingWithFee =
        "Transfert de 659228030 FOYANG vers 699014204 MIKAM reussi. " +
            "ID transaction: PP260621.0702.D51614, Montant Transaction: 20244 FCFA, " +
            "Frais: 44.48 FCFA, Commission: 0 FCFA, Montant Net: 20288.48 FCFA, " +
            "Nouveau Solde: 29713.53 FCFA."

    private val incoming =
        "Transfert de 690440480 TCHOUKEN vers 659228030 FOYANG reussi. Details: " +
            "ID transaction: PP260322.2159.B05939, Montant Transaction: 100000FCFA, " +
            "Frais: 0 FCFA, Nouveau Solde: 116010.99 FCFA."

    private suspend fun MoneyImportRepository.import(body: String) =
        useCase(this).invoke(sender, body, receivedAt, account, owner)

    // ── Fee splitting ────────────────────────────────────────────────────────

    @Test
    fun `a transfer with a fee produces two entries, principal and fee`() = runBlocking {
        val repo = FakeImportRepository()
        val outcome = repo.import(outgoingWithFee)

        assertTrue(outcome is ImportOutcome.Imported)
        assertEquals(2, repo.savedEntries.size)

        val principal = repo.savedEntries[0]
        val fee = repo.savedEntries[1]

        assertEquals(-20244.0, principal.amount, "money out, principal only")
        assertNull(principal.categoryId, "a transfer could be anything; a wrong category is worse than none")

        assertEquals(-44.48, fee.amount, "a fee is always money out")
        assertEquals("cat_fees", fee.categoryId)
    }

    @Test
    fun `the two entries together equal the balance change the provider reported`() = runBlocking {
        val repo = FakeImportRepository()
        repo.import(outgoingWithFee)

        // Montant Net was 20288.48; the ledger must move by exactly that or the balance drifts.
        assertEquals(-20288.48, repo.savedEntries.sumOf { it.amount })
    }

    @Test
    fun `the fee entry gets its own external id so both rows stay unique and traceable`() = runBlocking {
        val repo = FakeImportRepository()
        repo.import(outgoingWithFee)

        assertEquals("PP260621.0702.D51614", repo.savedEntries[0].externalId)
        assertEquals("PP260621.0702.D51614#fee", repo.savedEntries[1].externalId)
    }

    @Test
    fun `no fee means a single entry`() = runBlocking {
        val repo = FakeImportRepository()
        repo.import(incoming)

        assertEquals(1, repo.savedEntries.size)
        assertEquals(100000.0, repo.savedEntries.single().amount, "money in is positive")
    }

    // ── Dedup, cheapest layer first ──────────────────────────────────────────

    @Test
    fun `the same message twice imports once`() = runBlocking {
        val repo = FakeImportRepository()
        repo.import(outgoingWithFee)
        val second = repo.import(outgoingWithFee)

        assertEquals(ImportOutcome.AlreadySeen, second)
        assertEquals(2, repo.savedEntries.size, "still just the first import's two rows")
    }

    @Test
    fun `a re-sent message with a different arrival time is caught by the transaction id`() = runBlocking {
        val repo = FakeImportRepository()
        repo.import(outgoingWithFee)

        // A different receivedAt changes the fingerprint, so only the provider id can catch this.
        val resent = useCase(repo).invoke(sender, outgoingWithFee, receivedAt + 60_000, account, owner)

        assertTrue(resent is ImportOutcome.AlreadyRecorded)
        assertEquals(2, repo.savedEntries.size)
    }

    @Test
    fun `a matching hand-entered transaction goes to review rather than double-counting`() = runBlocking {
        val repo = FakeImportRepository().apply { manualDuplicateId = "tx_manual_1" }
        val outcome = repo.import(outgoingWithFee)

        assertEquals(ImportOutcome.NeedsReview("tx_manual_1"), outcome)
        assertTrue(repo.savedEntries.isEmpty(), "nothing is written until the user decides")
        assertEquals(ImportStatus.PENDING_REVIEW, repo.outcomes.single())
    }

    // ── Non-transactions ─────────────────────────────────────────────────────

    @Test
    fun `an advert is recorded as ignored so it is never re-examined`() = runBlocking {
        val repo = FakeImportRepository()
        val outcome = repo.import("Rechargez votre compte Orange Money et gagnez des prix!")

        assertEquals(ImportOutcome.NotRecognised, outcome)
        assertEquals(ImportStatus.IGNORED, repo.outcomes.single())
        assertTrue(repo.savedEntries.isEmpty())
    }

    @Test
    fun `a message from an unknown sender is not imported`() = runBlocking {
        val repo = FakeImportRepository()
        val outcome = useCase(repo).invoke("RandomBank", outgoingWithFee, receivedAt, account, owner)

        assertEquals(ImportOutcome.NotRecognised, outcome)
        assertTrue(repo.savedEntries.isEmpty())
    }

    // ── Timing ───────────────────────────────────────────────────────────────

    @Test
    fun `entries are dated from the transaction id, not the arrival time`() = runBlocking {
        val repo = FakeImportRepository()
        repo.import(outgoingWithFee)

        // PP260621.0702 → 2026-06-21 07:02 local, which is nothing like receivedAt. Derived here
        // rather than hardcoded so the assertion holds in any timezone.
        val expected = LocalDateTime(2026, 6, 21, 7, 2)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        assertEquals(expected, repo.savedEntries[0].timestamp)
        assertTrue(repo.savedEntries.none { it.timestamp == receivedAt }, "not the arrival time")
        assertEquals(
            repo.savedEntries[0].timestamp,
            repo.savedEntries[1].timestamp,
            "the fee belongs to the same moment as its transfer",
        )
    }
}
