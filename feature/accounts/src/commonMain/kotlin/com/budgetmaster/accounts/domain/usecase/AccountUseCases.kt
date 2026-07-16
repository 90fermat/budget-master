package com.budgetmaster.accounts.domain.usecase

import com.budgetmaster.accounts.domain.model.Account
import com.budgetmaster.accounts.domain.model.AccountDraft
import com.budgetmaster.accounts.domain.repository.AccountRepository
import com.budgetmaster.core.session.ActiveAccountStore
import kotlinx.coroutines.flow.Flow

/** Observes the current user's accounts with live balances. */
class ObserveAccountsUseCase(private val repository: AccountRepository) {
    operator fun invoke(): Flow<List<Account>> = repository.observeAccounts()
}

/** Creates or updates an account. */
class SaveAccountUseCase(private val repository: AccountRepository) {
    suspend operator fun invoke(draft: AccountDraft): String = repository.upsertAccount(draft)
}

/** Archives or restores an account (keeps its transaction history). */
class ArchiveAccountUseCase(private val repository: AccountRepository) {
    suspend operator fun invoke(id: String, archived: Boolean) = repository.setArchived(id, archived)
}

/** Permanently deletes an account and its transactions. */
class DeleteAccountUseCase(
    private val repository: AccountRepository,
    private val activeAccountStore: ActiveAccountStore,
) {
    suspend operator fun invoke(id: String) {
        repository.deleteAccount(id)
    }
}

/** Observes the active-account selection (`null` = "All accounts"). */
class ObserveActiveAccountUseCase(private val activeAccountStore: ActiveAccountStore) {
    operator fun invoke(): Flow<String?> = activeAccountStore.activeAccountId
}

/** Selects the active account, or `null` to view all accounts consolidated. */
class SelectActiveAccountUseCase(private val activeAccountStore: ActiveAccountStore) {
    suspend operator fun invoke(id: String?) = activeAccountStore.setActiveAccount(id)
}
