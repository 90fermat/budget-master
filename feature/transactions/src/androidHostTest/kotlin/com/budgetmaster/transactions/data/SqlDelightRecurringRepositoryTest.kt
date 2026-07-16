@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.transactions.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import com.budgetmaster.core.db.AppDataSeeder
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.db.DefaultData
import com.budgetmaster.core.session.SessionStore
import com.budgetmaster.transactions.TestDatabaseHelper
import com.budgetmaster.transactions.domain.model.Frequency
import com.budgetmaster.transactions.domain.model.RecurringDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlDelightRecurringRepositoryTest {

    private val dayMs = 24L * 60 * 60 * 1000

    private suspend fun setup(): Pair<SqlDelightRecurringRepository, DatabaseProvider> {
        val provider = TestDatabaseHelper.createProvider()
        val seeder = AppDataSeeder(provider)
        val repo = SqlDelightRecurringRepository(provider, seeder, SessionStore())
        seeder.seedForUser(DefaultData.DEFAULT_USER_ID)
        return repo to provider
    }

    private suspend fun transactionsOf(provider: DatabaseProvider) =
        provider.getDatabase().budgetMasterDatabaseQueries.selectAllTransactions().awaitAsList()

    @Test
    fun materializeCreatesOneTransactionPerMissedPeriod() = runTest {
        val (repo, provider) = setup()
        val start = 1_000_000_000_000L
        repo.upsertRecurring(
            RecurringDraft(
                id = "r1",
                categoryId = "cat_housing",
                amountAbs = 900.0,
                isExpense = true,
                description = "Rent",
                frequency = Frequency.DAILY,
                startDate = start,
            ),
        )

        // Three days have passed since the start date: expect the start + 3 catch-ups.
        val created = repo.materializeDue(start + 3 * dayMs)
        assertEquals(4, created)

        val rows = transactionsOf(provider)
        assertEquals(4, rows.size)
        assertTrue(rows.all { it.amount == -900.0 && it.description == "Rent" })
        // Each entry is flagged recurring and lands on its own occurrence date.
        assertTrue(rows.all { it.isRecurring == 1L })
        assertEquals(4, rows.map { it.timestamp }.distinct().size)
    }

    @Test
    fun materializeIsIdempotentAndDoesNotDuplicateOnRerun() = runTest {
        val (repo, provider) = setup()
        val start = 1_000_000_000_000L
        repo.upsertRecurring(
            RecurringDraft(
                id = "r1", categoryId = null, amountAbs = 10.0, isExpense = true,
                description = "Coffee plan", frequency = Frequency.DAILY, startDate = start,
            ),
        )

        val now = start + 2 * dayMs
        repo.materializeDue(now)
        val afterFirst = transactionsOf(provider).size

        // Re-running at the same instant must create nothing further.
        val secondRun = repo.materializeDue(now)
        assertEquals(0, secondRun)
        assertEquals(afterFirst, transactionsOf(provider).size)
    }

    @Test
    fun pausedSchedulesProduceNothing() = runTest {
        val (repo, provider) = setup()
        val start = 1_000_000_000_000L
        repo.upsertRecurring(
            RecurringDraft(
                id = "r1", categoryId = null, amountAbs = 50.0, isExpense = true,
                description = "Gym", frequency = Frequency.WEEKLY, startDate = start,
            ),
        )
        repo.setActive("r1", false)

        assertEquals(0, repo.materializeDue(start + 90 * dayMs))
        assertTrue(transactionsOf(provider).isEmpty())
        assertTrue(!repo.observeRecurring().first().first().isActive)
    }

    @Test
    fun futureSchedulesAreNotMaterializedYet() = runTest {
        val (repo, provider) = setup()
        val start = 1_000_000_000_000L
        repo.upsertRecurring(
            RecurringDraft(
                id = "r1", categoryId = null, amountAbs = 20.0, isExpense = false,
                description = "Payday", frequency = Frequency.MONTHLY, startDate = start,
            ),
        )

        assertEquals(0, repo.materializeDue(start - dayMs))
        assertTrue(transactionsOf(provider).isEmpty())
    }

    @Test
    fun monthlyFrequencyStepsByCalendarMonthNotFixedDays() {
        // 2024-01-31T00:00Z -> Feb 29 (leap year), not "31 days later".
        val jan31 = 1_706_659_200_000L
        val next = Frequency.MONTHLY.next(jan31, TimeZone.UTC)
        val date = Instant.fromEpochMilliseconds(next).toLocalDateTime(TimeZone.UTC).date
        assertEquals("2024-02-29", date.toString())
    }
}
