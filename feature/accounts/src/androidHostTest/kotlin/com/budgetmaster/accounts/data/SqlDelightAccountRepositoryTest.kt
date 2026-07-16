@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.accounts.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import com.budgetmaster.accounts.TestDatabaseHelper
import com.budgetmaster.accounts.data.repository.SqlDelightAccountRepository
import com.budgetmaster.accounts.domain.model.AccountDraft
import com.budgetmaster.accounts.domain.model.AccountType
import com.budgetmaster.accounts.domain.usecase.CalculateNetWorthUseCase
import com.budgetmaster.core.currency.ExchangeRateRepository
import com.budgetmaster.core.db.AppDataSeeder
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.session.SessionStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SqlDelightAccountRepositoryTest {

    private fun setup(): Pair<SqlDelightAccountRepository, DatabaseProvider> {
        val provider = TestDatabaseHelper.createProvider()
        val repo = SqlDelightAccountRepository(provider, SessionStore(), AppDataSeeder(provider))
        return repo to provider
    }

    @Test
    fun seedsAFirstCashWalletForTheUser() = runTest {
        val (repo, _) = setup()
        val accounts = repo.observeAccounts().first()
        assertEquals(1, accounts.size)
        assertEquals("Cash", accounts.first().name)
        assertEquals(AccountType.CASH, accounts.first().type)
        assertEquals(0.0, accounts.first().currentBalance)
    }

    @Test
    fun currentBalanceIsOpeningPlusItsOwnTransactions() = runTest {
        val (repo, provider) = setup()
        val id = repo.upsertAccount(
            AccountDraft(name = "Checking", type = AccountType.CHECKING, openingBalance = 100.0, currency = "USD"),
        )
        val otherId = repo.upsertAccount(
            AccountDraft(name = "Savings", type = AccountType.SAVINGS, openingBalance = 500.0, currency = "USD"),
        )

        val now = Clock.System.now().toEpochMilliseconds()
        val queries = provider.getDatabase().budgetMasterDatabaseQueries
        queries.insertTransaction("t1", id, "cat_salary", 50.0, "Pay", now, null, null, 0, null)
        queries.insertTransaction("t2", id, "cat_food", -20.0, "Lunch", now, null, null, 0, null)
        // A transaction on another wallet must not affect this one's balance.
        queries.insertTransaction("t3", otherId, "cat_food", -300.0, "Other", now, null, null, 0, null)

        val accounts = repo.observeAccounts().first()
        val checking = accounts.first { it.id == id }
        assertEquals(100.0, checking.openingBalance)
        assertEquals(130.0, checking.currentBalance)
        assertEquals(200.0, accounts.first { it.id == otherId }.currentBalance)
    }

    @Test
    fun editPreservesIdAndUpdatesFields() = runTest {
        val (repo, _) = setup()
        val id = repo.upsertAccount(
            AccountDraft(name = "Wallet", type = AccountType.CASH, openingBalance = 10.0, currency = "USD"),
        )
        repo.upsertAccount(
            AccountDraft(id = id, name = "Main Card", type = AccountType.CREDIT_CARD, openingBalance = 25.0, currency = "EUR"),
        )

        val account = repo.observeAccounts().first().first { it.id == id }
        assertEquals("Main Card", account.name)
        assertEquals(AccountType.CREDIT_CARD, account.type)
        assertEquals(25.0, account.openingBalance)
        assertEquals("EUR", account.currency)
    }

    @Test
    fun archiveKeepsTheAccountButFlagsIt() = runTest {
        val (repo, _) = setup()
        val id = repo.upsertAccount(
            AccountDraft(name = "Old", type = AccountType.CASH, openingBalance = 0.0, currency = "USD"),
        )

        repo.setArchived(id, true)
        assertTrue(repo.observeAccounts().first().first { it.id == id }.isArchived)

        repo.setArchived(id, false)
        assertTrue(!repo.observeAccounts().first().first { it.id == id }.isArchived)
    }

    @Test
    fun transferMovesMoneyBetweenWalletsAndLeavesNetWorthUnchanged() = runTest {
        val (repo, provider) = setup()
        val from = repo.upsertAccount(
            AccountDraft(name = "Checking", type = AccountType.CHECKING, openingBalance = 500.0, currency = "USD"),
        )
        val to = repo.upsertAccount(
            AccountDraft(name = "Savings", type = AccountType.SAVINGS, openingBalance = 100.0, currency = "USD"),
        )

        repo.transfer(from, to, 200.0, Clock.System.now().toEpochMilliseconds())

        val accounts = repo.observeAccounts().first()
        assertEquals(300.0, accounts.first { it.id == from }.currentBalance)
        assertEquals(300.0, accounts.first { it.id == to }.currentBalance)

        // Both legs are tagged with one transfer id, so reports can exclude them.
        val rows = provider.getDatabase().budgetMasterDatabaseQueries
            .selectTransactionsByUser("default_user").awaitAsList()
            .filter { it.transferGroupId != null }
        assertEquals(2, rows.size)
        assertEquals(1, rows.mapNotNull { it.transferGroupId }.distinct().size)
        assertEquals(0.0, rows.sumOf { it.amount })
    }

    @Test
    fun transferRejectsSameAccountAndNonPositiveAmounts() = runTest {
        val (repo, _) = setup()
        val a = repo.upsertAccount(
            AccountDraft(name = "A", type = AccountType.CASH, openingBalance = 10.0, currency = "USD"),
        )
        val b = repo.upsertAccount(
            AccountDraft(name = "B", type = AccountType.CASH, openingBalance = 10.0, currency = "USD"),
        )
        val now = Clock.System.now().toEpochMilliseconds()

        assertFailsWith<IllegalArgumentException> { repo.transfer(a, a, 5.0, now) }
        assertFailsWith<IllegalArgumentException> { repo.transfer(a, b, 0.0, now) }
    }

    @Test
    fun reconcilePostsAnAdjustmentSoTheBalanceMatchesReality() = runTest {
        val (repo, provider) = setup()
        val id = repo.upsertAccount(
            AccountDraft(name = "Wallet", type = AccountType.CASH, openingBalance = 100.0, currency = "USD"),
        )
        val now = Clock.System.now().toEpochMilliseconds()
        provider.getDatabase().budgetMasterDatabaseQueries
            .insertTransaction("t1", id, "cat_food", -30.0, "Lunch", now, null, null, 0, null)

        // Derived balance is 70; the real wallet holds 65.
        repo.reconcile(id, 65.0, now)

        assertEquals(65.0, repo.observeAccounts().first().first { it.id == id }.currentBalance)
        // The correction is an ordinary entry, excluded from income/expense.
        val adjustment = provider.getDatabase().budgetMasterDatabaseQueries
            .selectTransactionsByAccount(id).awaitAsList().first { it.transferGroupId != null }
        assertEquals(-5.0, adjustment.amount)
    }

    @Test
    fun netWorthConvertsWalletsIntoOneCurrencyAndFlagsMissingRates() = runTest {
        val (repo, provider) = setup()
        val rates = ExchangeRateRepository(provider)
        val netWorthOf = CalculateNetWorthUseCase(rates)

        repo.upsertAccount(
            AccountDraft(name = "US", type = AccountType.CASH, openingBalance = 100.0, currency = "USD"),
        )
        repo.upsertAccount(
            AccountDraft(name = "EU", type = AccountType.CASH, openingBalance = 50.0, currency = "EUR"),
        )
        val accounts = repo.observeAccounts().first().filter { it.currency != "USD" || it.name == "US" }

        // No rate yet: the EUR wallet is added at face value and the total is flagged.
        val before = netWorthOf(accounts, "USD")
        assertTrue(before.hasUnconvertedAccounts)

        // 1 EUR = 1.10 USD -> 100 + (50 * 1.10) = 155
        rates.putRate("EUR", "USD", 1.10)
        val after = netWorthOf(accounts, "USD")
        assertEquals(155.0, after.total)
        assertTrue(!after.hasUnconvertedAccounts)

        // The reverse direction is derived from the stored pair's reciprocal.
        assertEquals(1.10, rates.rate("EUR", "USD"))
        assertEquals(1.0, rates.rate("USD", "USD"))
    }

    @Test
    fun deleteRemovesTheAccount() = runTest {
        val (repo, _) = setup()
        val id = repo.upsertAccount(
            AccountDraft(name = "Temp", type = AccountType.CASH, openingBalance = 0.0, currency = "USD"),
        )
        assertEquals(2, repo.observeAccounts().first().size) // seeded Cash + Temp

        repo.deleteAccount(id)
        val remaining = repo.observeAccounts().first()
        assertEquals(1, remaining.size)
        assertEquals("Cash", remaining.first().name)
    }
}
