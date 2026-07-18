@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.transactions.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import com.budgetmaster.core.db.AppDataSeeder
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.db.DefaultData
import com.budgetmaster.core.session.SessionStore
import com.budgetmaster.transactions.TestDatabaseHelper
import com.budgetmaster.transactions.data.repository.SqlDelightMoneyImportRepository
import com.budgetmaster.transactions.domain.repository.ImportStatus
import com.budgetmaster.transactions.domain.repository.PendingImportDetails
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The review queue, end to end against a real database.
 *
 * Worth the real driver rather than a fake: the whole point of this queue is that the parsed
 * fields *survive* being written and read back. A fake that hands its own objects back would
 * prove nothing about the columns, which is exactly where this could fail.
 */
class SqlDelightMoneyImportRepositoryTest {

    private val occurredAt = 1_752_753_600_000L

    private suspend fun setup(): Pair<SqlDelightMoneyImportRepository, DatabaseProvider> {
        val provider = TestDatabaseHelper.createProvider()
        val seeder = AppDataSeeder(provider)
        val repo = SqlDelightMoneyImportRepository(provider, SessionStore(), seeder)
        seeder.seedForUser(DefaultData.DEFAULT_USER_ID)
        return repo to provider
    }

    private suspend fun accountId(provider: DatabaseProvider): String =
        provider.getDatabase().budgetMasterDatabaseQueries
            .selectAccountsByUserId(DefaultData.DEFAULT_USER_ID)
            .awaitAsList()
            .first()
            .id

    private suspend fun transactionsOf(provider: DatabaseProvider) =
        provider.getDatabase().budgetMasterDatabaseQueries.selectAllTransactions().awaitAsList()

    private suspend fun SqlDelightMoneyImportRepository.defer(
        provider: DatabaseProvider,
        hash: String = "h1",
        fee: Double = 0.0,
    ) = recordMessageOutcome(
        hash = hash,
        provider = "orange_money",
        sender = "OrangeMoney",
        receivedAt = occurredAt,
        status = ImportStatus.PENDING_REVIEW,
        externalId = "OM250717.1200.A1",
        transactionId = "tx_manual",
        pending = PendingImportDetails(
            accountId = accountId(provider),
            amount = -20_244.0,
            fee = fee,
            description = "FOYANG CYRILLE",
            occurredAt = occurredAt,
        ),
    )

    @Test
    fun aDeferredMessageComesBackWithEverythingNeededToActOnIt() = runTest {
        val (repo, provider) = setup()
        repo.defer(provider, fee = 44.48)

        val queue = repo.observePendingReview().first()
        assertEquals(1, queue.size)
        val pending = queue.single()
        assertEquals("orange_money", pending.provider)
        assertEquals("tx_manual", pending.existingTransactionId)
        assertEquals(-20_244.0, pending.details.amount)
        assertEquals(44.48, pending.details.fee)
        assertEquals("FOYANG CYRILLE", pending.details.description)
    }

    @Test
    fun keepingAReviewWritesTheEntriesTheImporterHeldBack() = runTest {
        val (repo, provider) = setup()
        repo.defer(provider, fee = 44.48)
        val before = transactionsOf(provider).size

        val ids = repo.resolvePending("h1", keep = true)

        // Two rows, not one: the fee stays its own entry exactly as it would at first import.
        assertEquals(2, ids.size)
        val written = transactionsOf(provider).filter { it.id in ids }
        assertEquals(2, written.size)
        assertEquals(before + 2, transactionsOf(provider).size)
        assertEquals(-20_244.0, written.first { it.externalId == "OM250717.1200.A1" }.amount)
        assertEquals(-44.48, written.first { it.externalId!!.endsWith("#fee") }.amount)

        // And the question is closed, so it is never asked twice.
        assertTrue(repo.observePendingReview().first().isEmpty())
    }

    @Test
    fun rejectingAReviewWritesNothingAndClosesIt() = runTest {
        val (repo, provider) = setup()
        repo.defer(provider)
        val before = transactionsOf(provider).size

        val ids = repo.resolvePending("h1", keep = false)

        assertTrue(ids.isEmpty())
        assertEquals(before, transactionsOf(provider).size)
        assertTrue(repo.observePendingReview().first().isEmpty())
    }

    @Test
    fun aResolvedMessageIsStillRememberedAsSeen() = runTest {
        val (repo, provider) = setup()
        repo.defer(provider)
        repo.resolvePending("h1", keep = false)

        // Otherwise the next copy of the same SMS would re-open the review the user just answered.
        assertTrue(repo.hasSeenMessage("h1"))
    }
}
