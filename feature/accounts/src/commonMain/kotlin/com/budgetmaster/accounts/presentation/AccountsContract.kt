package com.budgetmaster.accounts.presentation

import com.budgetmaster.accounts.domain.model.Account
import com.budgetmaster.accounts.domain.model.AccountDraft
import com.budgetmaster.accounts.domain.model.NetWorth

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
    /**
     * Net worth converted into [primaryCurrency] using cached exchange rates. Null until the
     * first calculation completes; the card falls back to the raw sum meanwhile.
     */
    val netWorthConverted: NetWorth? = null,
) {
    /**
     * The accounts the overview total is built from: not archived, and counted in totals.
     *
     * Both exclusions matter and they mean different things — archived is "no longer in use",
     * excluded-from-totals is "in use, but kept apart".
     */
    val activeAccounts: List<Account> = accounts.filter { !it.isArchived && it.includeInTotals }

    /** Currency to label the overview with (the most common among active accounts). */
    val primaryCurrency: String =
        activeAccounts.groupingBy { it.currency }.eachCount().maxByOrNull { it.value }?.key ?: "USD"

    /** Net worth (assets − liabilities), converted when rates allow. */
    val netWorth: Double = netWorthConverted?.total ?: activeAccounts.sumOf { it.currentBalance }

    val isMultiCurrency: Boolean = activeAccounts.map { it.currency }.distinct().size > 1

    /** True when currencies are mixed and at least one had no rate, so the total is fuzzy. */
    val isNetWorthApproximate: Boolean =
        isMultiCurrency && (netWorthConverted?.hasUnconvertedAccounts ?: true)
}

/** User actions from the Accounts screen / switcher. */
sealed interface AccountsIntent {
    data object OpenAdd : AccountsIntent
    data class OpenEdit(val account: Account) : AccountsIntent
    data object DismissEditor : AccountsIntent
    data class Submit(val draft: AccountDraft) : AccountsIntent
    data class SetArchived(val id: String, val archived: Boolean) : AccountsIntent

    /** Includes or excludes a wallet from the consolidated "All accounts" view. */
    data class SetIncludedInTotals(val id: String, val included: Boolean) : AccountsIntent
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
