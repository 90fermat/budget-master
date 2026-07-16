package com.budgetmaster.auth.presentation.register

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.auth_hide_password
import budgetmaster.core.generated.resources.auth_show_password
import budgetmaster.core.generated.resources.login_email_label
import budgetmaster.core.generated.resources.login_password_label
import budgetmaster.core.generated.resources.register_btn_text
import budgetmaster.core.generated.resources.register_confirm_password_label
import budgetmaster.core.generated.resources.register_login_link
import budgetmaster.core.generated.resources.register_subtitle
import budgetmaster.core.generated.resources.register_title
import com.budgetmaster.auth.presentation.localizedMessage
import org.jetbrains.compose.resources.stringResource

/**
 * Register screen for creating a new account.
 *
 * @param viewModel The ViewModel managing registration state.
 * @param onNavigateToHome Called when registration succeeds.
 * @param onNavigateToLogin Called when the user taps "Sign In".
 */
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                RegisterEffect.NavigateToHome -> onNavigateToHome()
                RegisterEffect.NavigateToLogin -> onNavigateToLogin()
                is RegisterEffect.ShowError -> Unit // Errors are shown inline via state.error.
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
            Text(stringResource(Res.string.register_title), style = MaterialTheme.typography.headlineLarge)
            Text(
                stringResource(Res.string.register_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.onIntent(RegisterIntent.EmailChanged(it)) },
                label = { Text(stringResource(Res.string.login_email_label)) },
                singleLine = true,
                isError = state.error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Email input" },
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.onIntent(RegisterIntent.PasswordChanged(it)) },
                label = { Text(stringResource(Res.string.login_password_label)) },
                singleLine = true,
                isError = state.error != null,
                visualTransformation = if (state.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    val desc = stringResource(
                        if (state.isPasswordVisible) Res.string.auth_hide_password else Res.string.auth_show_password,
                    )
                    IconButton(onClick = { viewModel.onIntent(RegisterIntent.TogglePasswordVisibility) }) {
                        Icon(
                            imageVector = if (state.isPasswordVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = desc,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Password input" },
            )

            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = { viewModel.onIntent(RegisterIntent.ConfirmPasswordChanged(it)) },
                label = { Text(stringResource(Res.string.register_confirm_password_label)) },
                singleLine = true,
                isError = state.error != null,
                visualTransformation = if (state.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Confirm password input" },
            )

            AnimatedVisibility(visible = state.error != null) {
                Text(
                    text = state.error?.localizedMessage() ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = { viewModel.onIntent(RegisterIntent.RegisterClicked) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp).semantics { contentDescription = "Register button" },
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.register_btn_text))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { viewModel.onIntent(RegisterIntent.NavigateToLogin) }) {
                    Text(stringResource(Res.string.register_login_link))
                }
            }
        }
    }
}
