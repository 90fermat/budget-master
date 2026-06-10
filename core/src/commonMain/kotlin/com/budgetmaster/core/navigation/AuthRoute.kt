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
     */
    @Serializable
    data object Transactions : AuthRoute

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
}
