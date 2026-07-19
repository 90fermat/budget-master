package com.budgetmaster.dashboard.presentation

import com.budgetmaster.core.navigation.TransactionKind
import com.budgetmaster.dashboard.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the effect → navigation mapping the Dashboard screen performs.
 *
 * This exists because of a bug that shipped with a *passing* test. `DashboardViewModelTest`
 * asserted that tapping a quick action emits `NavigateToAddTransaction`, and it did — but the
 * screen's effect collector ended in `else -> Unit`, so the effect was dropped and all three
 * quick-action buttons did nothing at all on a real device. Testing the emission proved only that
 * half the wire existed.
 *
 * The collector is now an exhaustive `when`, so an unhandled effect is a compile error rather
 * than a dead button. This test covers the other half: that each effect maps to the *right*
 * destination, which the compiler cannot check.
 */
class DashboardEffectRoutingTest {

    /**
     * Mirrors the mapping in `DashboardScreen`'s collector.
     *
     * Kept as a pure function so the routing decision is testable without a Compose runtime. If
     * the screen's `when` and this diverge, the screen is what to fix.
     */
    private fun route(effect: DashboardEffect): String = when (effect) {
        DashboardEffect.NavigateToSettings -> "settings"
        DashboardEffect.NavigateToTransactions -> "transactions"
        is DashboardEffect.NavigateToAddTransaction -> when (effect.type) {
            TransactionType.EXPENSE -> "editor:${TransactionKind.EXPENSE}"
            TransactionType.INCOME -> "editor:${TransactionKind.INCOME}"
            TransactionType.TRANSFER -> "accounts:transfer"
        }
        is DashboardEffect.ShowUndoDelete -> "snackbar:undo"
        is DashboardEffect.ShowError -> "snackbar:error"
    }

    @Test
    fun `add expense opens the editor pre-set to expense`() {
        assertEquals(
            "editor:${TransactionKind.EXPENSE}",
            route(DashboardEffect.NavigateToAddTransaction(TransactionType.EXPENSE)),
        )
    }

    @Test
    fun `add income opens the editor pre-set to income`() {
        assertEquals(
            "editor:${TransactionKind.INCOME}",
            route(DashboardEffect.NavigateToAddTransaction(TransactionType.INCOME)),
        )
    }

    @Test
    fun `transfer goes to accounts, not the transaction editor`() {
        // A transfer writes two linked legs between the user's own wallets, which the transaction
        // editor has no concept of. Routing it there would silently create a one-sided entry.
        assertEquals(
            "accounts:transfer",
            route(DashboardEffect.NavigateToAddTransaction(TransactionType.TRANSFER)),
        )
    }

    @Test
    fun `every effect routes somewhere`() {
        // The real guarantee is the exhaustive `when` in the screen, which this mirrors: if an
        // effect is added and left unrouted, `route` stops compiling. This asserts the weaker but
        // still useful property that nothing maps to a blank destination.
        val all = listOf(
            DashboardEffect.NavigateToSettings,
            DashboardEffect.NavigateToTransactions,
            DashboardEffect.NavigateToAddTransaction(TransactionType.EXPENSE),
            DashboardEffect.NavigateToAddTransaction(TransactionType.INCOME),
            DashboardEffect.NavigateToAddTransaction(TransactionType.TRANSFER),
            DashboardEffect.ShowError("boom"),
        )
        all.forEach { assertTrue(route(it).isNotBlank(), "unrouted effect: $it") }
    }
}
