package com.budgetmaster.accounts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmaster.accounts.domain.usecase.ArchiveAccountUseCase
import com.budgetmaster.accounts.domain.usecase.DeleteAccountUseCase
import com.budgetmaster.accounts.domain.usecase.ObserveAccountsUseCase
import com.budgetmaster.accounts.domain.usecase.ObserveActiveAccountUseCase
import com.budgetmaster.accounts.domain.usecase.SaveAccountUseCase
import com.budgetmaster.accounts.domain.usecase.SelectActiveAccountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI ViewModel for the Accounts screen and the shared account switcher.
 */
class AccountsViewModel(
    private val observeAccounts: ObserveAccountsUseCase,
    private val observeActiveAccount: ObserveActiveAccountUseCase,
    private val saveAccount: SaveAccountUseCase,
    private val archiveAccount: ArchiveAccountUseCase,
    private val deleteAccount: DeleteAccountUseCase,
    private val selectActiveAccount: SelectActiveAccountUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountsState())
    val state: StateFlow<AccountsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(observeAccounts(), observeActiveAccount()) { accounts, activeId ->
                accounts to activeId
            }.collect { (accounts, activeId) ->
                _state.update {
                    it.copy(accounts = accounts, activeAccountId = activeId, isLoading = false)
                }
            }
        }
    }

    fun onIntent(intent: AccountsIntent) {
        when (intent) {
            AccountsIntent.OpenAdd -> _state.update { it.copy(editorOpen = true, editingAccount = null) }
            is AccountsIntent.OpenEdit ->
                _state.update { it.copy(editorOpen = true, editingAccount = intent.account) }
            AccountsIntent.DismissEditor ->
                _state.update { it.copy(editorOpen = false, editingAccount = null) }
            is AccountsIntent.Submit -> viewModelScope.launch {
                saveAccount(intent.draft)
                _state.update { it.copy(editorOpen = false, editingAccount = null) }
            }
            is AccountsIntent.SetArchived -> viewModelScope.launch {
                archiveAccount(intent.id, intent.archived)
            }
            is AccountsIntent.Delete -> viewModelScope.launch {
                // If the deleted account was active, fall back to the "All accounts" view.
                if (_state.value.activeAccountId == intent.id) selectActiveAccount(null)
                deleteAccount(intent.id)
            }
            is AccountsIntent.SelectActive -> viewModelScope.launch {
                selectActiveAccount(intent.id)
            }
        }
    }
}
