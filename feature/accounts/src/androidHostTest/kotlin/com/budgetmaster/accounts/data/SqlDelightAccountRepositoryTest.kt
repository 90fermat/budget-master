@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.accounts.data

import com.budgetmaster.accounts.TestDatabaseHelper
import com.budgetmaster.accounts.data.repository.SqlDelightAccountRepository
import com.budgetmaster.accounts.domain.model.AccountDraft
import com.budgetmaster.accounts.domain.model.AccountType
import com.budgetmaster.core.db.AppDataSeeder
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.session.SessionStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
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
        queries.insertTransaction("t1", id, "cat_salary", 50.0, "Pay", now, null, null, 0)
        queries.insertTransaction("t2", id, "cat_food", -20.0, "Lunch", now, null, null, 0)
        // A transaction on another wallet must not affect this one's balance.
        queries.insertTransaction("t3", otherId, "cat_food", -300.0, "Other", now, null, null, 0)

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
