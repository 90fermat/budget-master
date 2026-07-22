package com.budgetmaster.core.navigation

import kotlinx.serialization.Serializable

/**
 * Which kind of entry a screen should open its editor for.
 *
 * Lives in `:core` rather than in a feature because it crosses a feature boundary: the Dashboard
 * asks for it and Transactions honours it, and the architecture tests forbid one feature importing
 * another. It is deliberately *not* the Dashboard's own `TransactionType`, which also has a
 * `TRANSFER` case — a transfer is not an entry the transaction editor can create. Transfers move
 * money between the user's own wallets and are handled by the Accounts screen, so representing
 * them here would create a state the editor cannot serve.
 */
@Serializable
enum class TransactionKind {
    EXPENSE,
    INCOME,
    ;

    companion object {
        /**
         * The kind with this [name], or null for anything unrecognised.
         *
         * Routes carry the name as a string, so this is the only place a bad value can arrive —
         * from a stale deep link, or a saved back stack written by an older build. Null means "open
         * the screen without opening the editor", which is the harmless reading; throwing would
         * turn a stale link into a crash.
         */
        fun byNameOrNull(name: String?): TransactionKind? = entries.firstOrNull { it.name == name }
    }
}
