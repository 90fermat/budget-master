package com.budgetmaster.accounts.presentation

import com.budgetmaster.accounts.domain.model.Account
import com.budgetmaster.accounts.domain.model.AccountDraft

/** UI state for the Accounts screen and the account switcher. */
data class AccountsState(
    val accounts: List<Account> = emptyList(),
    val activeAccountId: String? = null,
    val isLoading: Boolean = true,
    val editorOpen: Boolean = false,
    val editingAccount: Account? = null,
    val transferOpen: Boolean = false,
    val reconcilingAccount: Account? = null,
    val errorMessage: String? = null,
) {
    /** The active accounts (archived excluded) used for the overview total. */
    val activeAccounts: List<Account> = accounts.filter { !it.isArchived }

    /** Net worth across active accounts (assets − liabilities). */
    val netWorth: Double = activeAccounts.sumOf { it.currentBalance }

    /** Currency to label the overview with (the most common among active accounts). */
    val primaryCurrency: String =
        activeAccounts.groupingBy { it.currency }.eachCount().maxByOrNull { it.value }?.key ?: "USD"

    val isMultiCurrency: Boolean = activeAccounts.map { it.currency }.distinct().size > 1
}

/** User actions from the Accounts screen / switcher. */
sealed interface AccountsIntent {
    data object OpenAdd : AccountsIntent
    data class OpenEdit(val account: Account) : AccountsIntent
    data object DismissEditor : AccountsIntent
    data class Submit(val draft: AccountDraft) : AccountsIntent
    data class SetArchived(val id: String, val archived: Boolean) : AccountsIntent
    data class Delete(val id: String) : AccountsIntent
    data class SelectActive(val id: String?) : AccountsIntent

    data object OpenTransfer : AccountsIntent
    data object DismissTransfer : AccountsIntent
    data class SubmitTransfer(
        val fromAccountId: String,
        val toAccountId: String,
        val amount: Double,
        val timestamp: Long,
    ) : AccountsIntent

    data class OpenReconcile(val account: Account) : AccountsIntent
    data object DismissReconcile : AccountsIntent
    data class SubmitReconcile(val accountId: String, val actualBalance: Double) : AccountsIntent
}
