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

    /** Permanently deletes an account and (via cascade) its transactions. */
    suspend fun deleteAccount(id: String)
}
