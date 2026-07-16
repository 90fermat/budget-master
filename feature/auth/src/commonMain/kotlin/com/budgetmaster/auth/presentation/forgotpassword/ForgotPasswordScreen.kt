package com.budgetmaster.auth.presentation.forgotpassword

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.forgot_password_btn_text
import budgetmaster.core.generated.resources.forgot_password_login_link
import budgetmaster.core.generated.resources.forgot_password_subtitle
import budgetmaster.core.generated.resources.forgot_password_success_msg
import budgetmaster.core.generated.resources.forgot_password_title
import budgetmaster.core.generated.resources.login_email_label
import com.budgetmaster.auth.presentation.localizedMessage
import org.jetbrains.compose.resources.stringResource

/**
 * Forgot Password screen that sends a reset link via email.
 *
 * @param viewModel The ViewModel managing the reset state.
 * @param onNavigateToLogin Called when the user returns to login.
 */
@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    onNavigateToLogin: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ForgotPasswordEffect.NavigateToLogin -> onNavigateToLogin()
                is ForgotPasswordEffect.ShowMessage -> Unit // Success shown inline via state.isSuccess.
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
        ) {
            Text(stringResource(Res.string.forgot_password_title), style = MaterialTheme.typography.headlineLarge)
            Text(
                stringResource(Res.string.forgot_password_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.onIntent(ForgotPasswordIntent.EmailChanged(it)) },
                label = { Text(stringResource(Res.string.login_email_label)) },
                singleLine = true,
                isError = state.error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Email input" },
            )

            AnimatedVisibility(visible = state.error != null) {
                Text(
                    state.error?.localizedMessage() ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            AnimatedVisibility(visible = state.isSuccess) {
                Text(
                    stringResource(Res.string.forgot_password_success_msg),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = { viewModel.onIntent(ForgotPasswordIntent.SendResetClicked) },
                enabled = !state.isLoading && !state.isSuccess,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).semantics { contentDescription = "Send reset button" },
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.forgot_password_btn_text))
                }
            }

            TextButton(onClick = { viewModel.onIntent(ForgotPasswordIntent.NavigateToLogin) }) {
                Text(stringResource(Res.string.forgot_password_login_link))
            }
        }
    }
}
