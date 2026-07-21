@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.accounts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmaster.accounts.domain.usecase.ArchiveAccountUseCase
import com.budgetmaster.accounts.domain.usecase.SetAccountIncludedInTotalsUseCase
import com.budgetmaster.accounts.domain.usecase.CalculateNetWorthUseCase
import com.budgetmaster.accounts.domain.usecase.DeleteAccountUseCase
import com.budgetmaster.accounts.domain.usecase.ObserveAccountsUseCase
import com.budgetmaster.accounts.domain.usecase.ObserveActiveAccountUseCase
import com.budgetmaster.accounts.domain.usecase.ReconcileAccountUseCase
import com.budgetmaster.accounts.domain.usecase.SaveAccountUseCase
import com.budgetmaster.accounts.domain.usecase.SelectActiveAccountUseCase
import com.budgetmaster.accounts.domain.usecase.TransferBetweenAccountsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * MVI ViewModel for the Accounts screen and the shared account switcher.
 */
class AccountsViewModel(
    private val observeAccounts: ObserveAccountsUseCase,
    private val observeActiveAccount: ObserveActiveAccountUseCase,
    private val saveAccount: SaveAccountUseCase,
    private val archiveAccount: ArchiveAccountUseCase,
    private val setIncludedInTotals: SetAccountIncludedInTotalsUseCase,
    private val deleteAccount: DeleteAccountUseCase,
    private val selectActiveAccount: SelectActiveAccountUseCase,
    private val transferBetweenAccounts: TransferBetweenAccountsUseCase,
    private val reconcileAccount: ReconcileAccountUseCase,
    private val calculateNetWorth: CalculateNetWorthUseCase,
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
                // Convert net worth once the wallets are known; rates come from the local cache.
                val current = _state.value
                val netWorth = calculateNetWorth(current.activeAccounts, current.primaryCurrency)
                _state.update { it.copy(netWorthConverted = netWorth) }
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
            is AccountsIntent.SetIncludedInTotals -> viewModelScope.launch {
                setIncludedInTotals(intent.id, intent.included)
            }
            is AccountsIntent.Delete -> viewModelScope.launch {
                // If the deleted account was active, fall back to the "All accounts" view.
                if (_state.value.activeAccountId == intent.id) selectActiveAccount(null)
                deleteAccount(intent.id)
            }
            is AccountsIntent.SelectActive -> viewModelScope.launch {
                selectActiveAccount(intent.id)
            }
            AccountsIntent.OpenTransfer -> _state.update { it.copy(transferOpen = true, errorMessage = null) }
            AccountsIntent.DismissTransfer -> _state.update { it.copy(transferOpen = false, errorMessage = null) }
            is AccountsIntent.SubmitTransfer -> viewModelScope.launch {
                try {
                    transferBetweenAccounts(
                        fromAccountId = intent.fromAccountId,
                        toAccountId = intent.toAccountId,
                        amount = intent.amount,
                        timestamp = intent.timestamp,
                    )
                    _state.update { it.copy(transferOpen = false, errorMessage = null) }
                } catch (e: IllegalArgumentException) {
                    _state.update { it.copy(errorMessage = e.message) }
                }
            }
            is AccountsIntent.OpenReconcile -> _state.update { it.copy(reconcilingAccount = intent.account) }
            AccountsIntent.DismissReconcile -> _state.update { it.copy(reconcilingAccount = null) }
            is AccountsIntent.SubmitReconcile -> viewModelScope.launch {
                reconcileAccount(
                    accountId = intent.accountId,
                    actualBalance = intent.actualBalance,
                    timestamp = Clock.System.now().toEpochMilliseconds(),
                )
                _state.update { it.copy(reconcilingAccount = null) }
            }
        }
    }
}
