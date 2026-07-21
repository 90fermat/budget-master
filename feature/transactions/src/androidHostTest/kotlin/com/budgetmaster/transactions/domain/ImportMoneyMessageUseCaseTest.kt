@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.transactions.domain

import com.budgetmaster.core.sms.OrangeMoneyParser
import com.budgetmaster.transactions.domain.repository.ImportStatus
import com.budgetmaster.transactions.domain.repository.ImportedEntry
import com.budgetmaster.transactions.domain.repository.MoneyImportRepository
import com.budgetmaster.transactions.domain.repository.PendingImport
import com.budgetmaster.transactions.domain.repository.PendingImportDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

    /** What the importer handed over for a deferred review, so the queue can act on it later. */
    var lastPending: PendingImportDetails? = null

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
        pending: PendingImportDetails?,
    ) {
        seenHashes += hash
        outcomes += status
        if (status == ImportStatus.PENDING_REVIEW) lastPending = pending
    }

    override fun observePendingReview(): Flow<List<PendingImport>> = flowOf(emptyList())

    override suspend fun resolvePending(hash: String, keep: Boolean): List<String> = emptyList()
}

class ImportMoneyMessageUseCaseTest {

    private val sender = "OrangeMoney"
    private val account = "acc_momo"

    /** A resolver that sends every provider to the one test wallet. */
    private val toAccount: suspend (String) -> String? = { account }
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
        useCase(this).invoke(sender, body, receivedAt, toAccount, owner)

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
        val resent = useCase(repo).invoke(sender, outgoingWithFee, receivedAt + 60_000, toAccount, owner)

        assertTrue(resent is ImportOutcome.AlreadyRecorded)
        assertEquals(2, repo.savedEntries.size)
    }

    @Test
    fun `a matching hand-entered transaction goes to review rather than double-counting`() = runBlocking {
        val repo = FakeImportRepository().apply { manualDuplicateId = "tx_manual_1" }
        val outcome = repo.import(outgoingWithFee)

        assertTrue(outcome is ImportOutcome.NeedsReview, "got $outcome")
        assertEquals("tx_manual_1", outcome.existingTransactionId)
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
        val outcome = useCase(repo).invoke("RandomBank", outgoingWithFee, receivedAt, toAccount, owner)

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

    /**
     * A pasted or shared message carries no sender, so parser selection has to fall back to the
     * body. Without this the whole paste/share path — the only capture route on platforms with no
     * SMS access — silently imports nothing.
     */
    @Test
    fun importsAMessageWithNoSenderByMatchingOnTheBody(): Unit = runBlocking {
        val repo = FakeImportRepository()

        val outcome = useCase(repo).invoke("", outgoingWithFee, receivedAt, toAccount, owner)

        assertTrue(outcome is ImportOutcome.Imported, "expected an import, got $outcome")
    }

    /** Paste then automatic capture of the same transaction must not double-count. */
    @Test
    fun aPastedMessageAndItsLaterAutomaticCaptureCollapse(): Unit = runBlocking {
        val repo = FakeImportRepository()
        val subject = useCase(repo)

        val pasted = subject.invoke("", outgoingWithFee, receivedAt, toAccount, owner)
        // Same transaction arriving later with a real sender and a different delivery time, so the
        // message fingerprint differs and only the provider id can catch it.
        val captured = subject.invoke(sender, outgoingWithFee, receivedAt + 60_000, toAccount, owner)

        assertTrue(pasted is ImportOutcome.Imported)
        assertTrue(
            captured is ImportOutcome.AlreadyRecorded,
            "the provider id must catch it once the fingerprint differs; got $captured",
        )
    }

    /**
     * The content fallback must not become a hole in the sender allowlist.
     *
     * A parseable body from an *unrecognised* sender still has to be refused: the allowlist is a
     * privacy guarantee that ordinary SMS are never inspected, and the fallback exists only for
     * messages with no sender, which the user pasted in deliberately.
     */
    @Test
    fun aParseableBodyFromAnUnknownSenderIsStillRefused(): Unit = runBlocking {
        val repo = FakeImportRepository()

        val outcome = useCase(repo).invoke("RandomBank", outgoingWithFee, receivedAt, toAccount, owner)

        assertTrue(outcome is ImportOutcome.NotRecognised, "the allowlist must still hold; got $outcome")
    }

    @Test
    fun unrecognisableTextWithNoSenderIsRejectedRatherThanGuessed(): Unit = runBlocking {
        val repo = FakeImportRepository()

        val outcome = useCase(repo).invoke("", "Hey, are we still on for lunch?", receivedAt, toAccount, owner)

        assertTrue(outcome is ImportOutcome.NotRecognised)
    }

    // ── Destination routing (the third device-test bug) ──────────────────────

    @Test
    fun `imports into the wallet the resolver returns for the provider`(): Unit = runBlocking {
        val repo = FakeImportRepository()
        // Orange goes to one wallet, everything else to another; the import must pick the first.
        val resolver: suspend (String) -> String? = { provider ->
            if (provider == "orange_money") "wallet_orange" else "wallet_other"
        }

        val outcome = useCase(repo).invoke(sender, outgoingWithFee, receivedAt, resolver, owner)

        assertTrue(outcome is ImportOutcome.Imported, "got $outcome")
        assertEquals("wallet_orange", outcome.accountId, "the configured destination, not a guess")
        assertTrue(repo.savedEntries.all { it.accountId == "wallet_orange" })
    }

    @Test
    fun `a parsed message with no configured wallet is held, not misfiled`(): Unit = runBlocking {
        val repo = FakeImportRepository()
        // No destination for any provider - the pre-fix code would have used the first wallet.
        val noWallet: suspend (String) -> String? = { null }

        val outcome = useCase(repo).invoke(sender, outgoingWithFee, receivedAt, noWallet, owner)

        assertTrue(outcome is ImportOutcome.NoDestination, "got $outcome")
        assertEquals("orange_money", outcome.provider)
        assertTrue(repo.savedEntries.isEmpty(), "nothing written when there is nowhere to put it")
    }

    @Test
    fun `a held message imports once a wallet is configured and it is re-run`(): Unit = runBlocking {
        val repo = FakeImportRepository()
        var wallet: String? = null
        val resolver: suspend (String) -> String? = { wallet }

        // First pass: no wallet, so it is held and - crucially - NOT recorded as seen, or the
        // re-run below would be swallowed as AlreadySeen and the transaction lost forever.
        val first = useCase(repo).invoke(sender, outgoingWithFee, receivedAt, resolver, owner)
        assertTrue(first is ImportOutcome.NoDestination)

        // User picks a wallet, backfill re-runs the same message.
        wallet = "wallet_orange"
        val second = useCase(repo).invoke(sender, outgoingWithFee, receivedAt, resolver, owner)

        assertTrue(second is ImportOutcome.Imported, "got $second")
        assertEquals("wallet_orange", second.accountId)
    }

    @Test
    fun `the imported outcome carries what a notification needs to say`(): Unit = runBlocking {
        val repo = FakeImportRepository()

        val outcome = useCase(repo).invoke(sender, incoming, receivedAt, toAccount, owner)

        assertTrue(outcome is ImportOutcome.Imported, "got $outcome")
        assertEquals("orange_money", outcome.provider)
        assertEquals(100000.0, outcome.amount, "money in is positive, principal only")
        assertTrue(outcome.description.isNotBlank(), "a notification needs something to name it")
    }
}
