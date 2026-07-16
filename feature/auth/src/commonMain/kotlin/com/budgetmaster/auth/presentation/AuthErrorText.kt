package com.budgetmaster.auth.presentation

import androidx.compose.runtime.Composable
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.auth_error_email_in_use
import budgetmaster.core.generated.resources.auth_error_empty_fields
import budgetmaster.core.generated.resources.auth_error_invalid_credentials
import budgetmaster.core.generated.resources.auth_error_invalid_email
import budgetmaster.core.generated.resources.auth_error_network
import budgetmaster.core.generated.resources.auth_error_password_mismatch
import budgetmaster.core.generated.resources.auth_error_too_many_requests
import budgetmaster.core.generated.resources.auth_error_unknown
import budgetmaster.core.generated.resources.auth_error_user_not_found
import budgetmaster.core.generated.resources.auth_error_weak_password
import com.budgetmaster.auth.domain.model.AuthError
import org.jetbrains.compose.resources.stringResource

/**
 * Resolves a typed [AuthError] to a localized, user-facing message (EN/FR).
 *
 * Keeps error copy out of the ViewModels so the same error renders in whichever
 * language the app is currently set to.
 */
@Composable
fun AuthError.localizedMessage(): String = stringResource(
    when (this) {
        AuthError.EmptyFields -> Res.string.auth_error_empty_fields
        AuthError.InvalidEmail -> Res.string.auth_error_invalid_email
        AuthError.WeakPassword -> Res.string.auth_error_weak_password
        AuthError.PasswordMismatch -> Res.string.auth_error_password_mismatch
        AuthError.InvalidCredentials -> Res.string.auth_error_invalid_credentials
        AuthError.UserNotFound -> Res.string.auth_error_user_not_found
        AuthError.EmailAlreadyInUse -> Res.string.auth_error_email_in_use
        AuthError.TooManyRequests -> Res.string.auth_error_too_many_requests
        AuthError.Network -> Res.string.auth_error_network
        AuthError.Unknown -> Res.string.auth_error_unknown
    },
)
