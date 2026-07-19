package com.budgetmaster.core.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for the Authentication, Onboarding, and Main flow.
 */
sealed interface AuthRoute {

    /**
     * Route representing the Onboarding screen.
     */
    @Serializable
    data object Onboarding : AuthRoute

    /**
     * Route representing the Login screen.
     */
    @Serializable
    data object Login : AuthRoute

    /**
     * Route representing the Registration screen.
     */
    @Serializable
    data object Register : AuthRoute

    /**
     * Route representing the Forgot Password screen.
     */
    @Serializable
    data object ForgotPassword : AuthRoute

    /**
     * Route representing the Biometric setup screen.
     */
    @Serializable
    data object Biometric : AuthRoute

    /**
     * Route representing the Dashboard screen.
     */
    @Serializable
    data object Dashboard : AuthRoute

    /**
     * Route representing the Splash screen.
     */
    @Serializable
    data object Splash : AuthRoute

    /**
     * Route representing the Transactions screen.
     *
     * @param openEditorFor when non-null, the screen opens its editor on arrival, pre-set to this
     *   kind. This is what makes the Dashboard's "Add expense" / "Add income" quick actions work:
     *   before it existed the editor was only reachable from this screen's own FAB, so the
     *   Dashboard had a button with nowhere to send the user.
     */
    @Serializable
    data class Transactions(val openEditorFor: TransactionKind? = null) : AuthRoute

    /**
     * Route representing the Budgets screen.
     */
    @Serializable
    data object Budgets : AuthRoute

    /**
     * Route representing the Goals screen.
     */
    @Serializable
    data object Goals : AuthRoute

    /**
     * Route representing the Reports screen.
     */
    @Serializable
    data object Reports : AuthRoute

    /**
     * Route representing the Settings screen.
     */
    @Serializable
    data object Settings : AuthRoute

    /**
     * Route representing the Accounts (wallets) management screen.
     *
     * @param openTransfer opens the transfer sheet on arrival. Transfers live here rather than in
     *   the transaction editor because they move money between the user's own wallets and write
     *   two linked legs, which the editor has no concept of.
     */
    @Serializable
    data class Accounts(val openTransfer: Boolean = false) : AuthRoute

    /**
     * Route representing the recurring-schedules management screen.
     */
    @Serializable
    data object Recurring : AuthRoute
}
