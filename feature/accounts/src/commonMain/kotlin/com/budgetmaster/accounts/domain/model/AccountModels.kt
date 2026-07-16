package com.budgetmaster.accounts.domain.model

/**
 * The kind of financial account (wallet). Drives the icon and how the balance is
 * interpreted — a [CREDIT_CARD] balance is a liability (typically negative net worth).
 */
enum class AccountType {
    CASH,
    CHECKING,
    SAVINGS,
    CREDIT_CARD,
    INVESTMENT,
}

/**
 * A financial account (wallet) owned by the signed-in user.
 *
 * @property openingBalance the balance the account started with (stored).
 * @property currentBalance [openingBalance] + the sum of this account's transactions
 * (computed live, never persisted, so it can't drift).
 */
data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val openingBalance: Double,
    val currentBalance: Double,
    val currency: String,
    val isArchived: Boolean,
)

/** Editable payload for creating (id == null) or updating an account. */
data class AccountDraft(
    val id: String? = null,
    val name: String,
    val type: AccountType,
    val openingBalance: Double,
    val currency: String,
)

/**
 * Net worth across a user's wallets, expressed in one currency.
 *
 * @property hasUnconvertedAccounts true when at least one wallet's currency had no known
 * rate and was added at face value — the total is then approximate.
 */
data class NetWorth(
    val total: Double,
    val currency: String,
    val hasUnconvertedAccounts: Boolean,
)

/** Aggregate across a user's active accounts (net worth = assets − liabilities). */
data class AccountsOverview(
    val accounts: List<Account>,
    val netWorth: Double,
    val primaryCurrency: String,
) {
    /** True when accounts span more than one currency (net worth is then approximate). */
    val isMultiCurrency: Boolean = accounts.map { it.currency }.distinct().size > 1
}
