package com.budgetmaster.accounts.domain.repository

import com.budgetmaster.accounts.domain.model.Account
import com.budgetmaster.accounts.domain.model.AccountDraft
import kotlinx.coroutines.flow.Flow

/** Data operations for the signed-in user's financial accounts (wallets). */
interface AccountRepository {

    /** Observes all of the current user's accounts (active first) with live balances. */
    fun observeAccounts(): Flow<List<Account>>

    /** Creates (when [AccountDraft.id] is null) or updates an account. Returns its id. */
    suspend fun upsertAccount(draft: AccountDraft): String

    /** Archives ([archived] = true) or restores an account without deleting its history. */
    suspend fun setArchived(id: String, archived: Boolean)

    /** Includes or excludes an account from the consolidated "All accounts" view. */
    suspend fun setIncludedInTotals(id: String, included: Boolean)

    /**
     * Moves [amount] from one wallet to another as a linked pair of transactions sharing a
     * transfer id: an outflow on [fromAccountId] and an inflow on [toAccountId]. Both legs
     * are excluded from income/expense totals and budget spend — this is the user's own
     * money changing pockets, not earning or spending.
     *
     * @throws IllegalArgumentException if the wallets are the same or [amount] is not positive.
     */
    suspend fun transfer(
        fromAccountId: String,
        toAccountId: String,
        amount: Double,
        timestamp: Long,
        note: String? = null,
    )

    /** Reconciles a wallet to [actualBalance] by posting a single adjustment transaction. */
    suspend fun reconcile(accountId: String, actualBalance: Double, timestamp: Long)

    /** Permanently deletes an account and (via cascade) its transactions. */
    suspend fun deleteAccount(id: String)
}
