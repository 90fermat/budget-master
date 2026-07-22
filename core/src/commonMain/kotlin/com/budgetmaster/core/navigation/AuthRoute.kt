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
     * @param openEditorFor when non-null, the name of the [TransactionKind] whose editor the
     *   screen opens on arrival. This is what makes the Dashboard's "Add expense" / "Add income"
     *   quick actions work: before it existed the editor was only reachable from this screen's own
     *   FAB, so the Dashboard had a button with nowhere to send the user.
     *
     *   A `String?` rather than the enum itself, and not by preference. Type-safe routes infer a
     *   `NavType` for custom types by reflection, which does not exist on Kotlin/Wasm — so the web
     *   build threw "could not find any NavType for argument openEditorFor" on startup and never
     *   rendered a frame. Passing the name and resolving it with [TransactionKind.byNameOrNull]
     *   keeps the type safety at both ends and needs no NavType at all.
     */
    @Serializable
    data class Transactions(val openEditorFor: String? = null) : AuthRoute

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

    /**
     * Route representing the notifications inbox, reached from the dashboard bell.
     */
    @Serializable
    data object Notifications : AuthRoute
}
