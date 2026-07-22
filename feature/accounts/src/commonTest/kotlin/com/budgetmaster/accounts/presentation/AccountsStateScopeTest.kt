package com.budgetmaster.accounts.presentation

import com.budgetmaster.accounts.domain.model.Account
import com.budgetmaster.accounts.domain.model.AccountType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * "Which wallets are shown" and "which wallets are summed" are different questions.
 *
 * They were once the same property, and the result was that excluding a wallet from the combined
 * total removed it from the accounts screen altogether — taking with it the only control that could
 * put it back. The wallet still existed and still held money; there was simply no way to reach it.
 */
class AccountsStateScopeTest {

    private fun account(id: String, balance: Double, archived: Boolean = false, counted: Boolean = true) =
        Account(
            id = id,
            name = id,
            type = AccountType.CASH,
            openingBalance = 0.0,
            currentBalance = balance,
            currency = "XAF",
            isArchived = archived,
            includeInTotals = counted,
        )

    @Test
    fun `a wallet left out of totals is still shown`() {
        val state = AccountsState(
            accounts = listOf(
                account("everyday", 10_000.0),
                account("epargne", 500_000.0, counted = false),
            ),
        )

        assertEquals(
            listOf("everyday", "epargne"),
            state.visibleAccounts.map { it.id },
            "an excluded wallet must stay on screen, or its own switch becomes unreachable",
        )
        assertEquals(listOf("everyday"), state.accountsInTotals.map { it.id })
    }

    @Test
    fun `its balance is kept out of the combined total`() {
        val state = AccountsState(
            accounts = listOf(
                account("everyday", 10_000.0),
                account("epargne", 500_000.0, counted = false),
            ),
        )

        // The whole point of the setting: savings the user does not want folded into everyday money.
        assertEquals(10_000.0, state.netWorth)
    }

    @Test
    fun `archived wallets are hidden, which is a different thing entirely`() {
        val state = AccountsState(
            accounts = listOf(
                account("everyday", 10_000.0),
                account("old", 1.0, archived = true),
            ),
        )

        // Archived means "no longer in use" and belongs in its own section; excluded-from-totals
        // means "in use, but kept apart". Conflating them is what caused the bug above.
        assertEquals(listOf("everyday"), state.visibleAccounts.map { it.id })
        assertTrue(state.accounts.any { it.isArchived })
    }
}
