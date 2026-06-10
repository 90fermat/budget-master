package com.budgetmaster.auth.presentation.register

/**
 * Navigation side-effects for the Register screen.
 */
sealed interface RegisterEffect {
    data object NavigateToHome : RegisterEffect
    data object NavigateToLogin : RegisterEffect
    data class ShowError(val message: String) : RegisterEffect
}
