package com.budgetmaster.auth.presentation.login

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.auth_hide_password
import budgetmaster.core.generated.resources.auth_show_password
import budgetmaster.core.generated.resources.login_biometric_btn_text
import budgetmaster.core.generated.resources.login_btn_text
import budgetmaster.core.generated.resources.login_email_label
import budgetmaster.core.generated.resources.login_forgot_password_link
import budgetmaster.core.generated.resources.login_password_label
import budgetmaster.core.generated.resources.login_register_link
import budgetmaster.core.generated.resources.login_subtitle
import budgetmaster.core.generated.resources.login_title
import com.budgetmaster.auth.presentation.localizedMessage
import org.jetbrains.compose.resources.stringResource

/**
 * Composable login screen that collects email/password and dispatches [LoginIntent]s.
 *
 * @param viewModel The ViewModel driving this screen.
 * @param onNavigateToHome Called when authentication succeeds.
 * @param onNavigateToRegister Called when the user taps "Sign Up".
 * @param onNavigateToForgotPassword Called when the user taps "Forgot Password".
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LoginEffect.NavigateToHome -> onNavigateToHome()
                LoginEffect.NavigateToRegister -> onNavigateToRegister()
                LoginEffect.NavigateToForgotPassword -> onNavigateToForgotPassword()
                is LoginEffect.ShowError -> Unit // Errors are shown inline via state.error.
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
            Text(
                text = stringResource(Res.string.login_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(Res.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.onIntent(LoginIntent.EmailChanged(it)) },
                label = { Text(stringResource(Res.string.login_email_label)) },
                singleLine = true,
                isError = state.error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Email input field" },
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.onIntent(LoginIntent.PasswordChanged(it)) },
                label = { Text(stringResource(Res.string.login_password_label)) },
                singleLine = true,
                isError = state.error != null,
                visualTransformation = if (state.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    viewModel.onIntent(LoginIntent.LoginClicked)
                }),
                trailingIcon = {
                    val desc = stringResource(
                        if (state.isPasswordVisible) Res.string.auth_hide_password else Res.string.auth_show_password,
                    )
                    IconButton(onClick = { viewModel.onIntent(LoginIntent.TogglePasswordVisibility) }) {
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
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Password input field" },
            )

            AnimatedVisibility(visible = state.error != null) {
                Text(
                    text = state.error?.localizedMessage() ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            TextButton(
                onClick = { viewModel.onIntent(LoginIntent.NavigateToForgotPassword) },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(Res.string.login_forgot_password_link))
            }

            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.onIntent(LoginIntent.LoginClicked)
                },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp).semantics { contentDescription = "Login button" },
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.login_btn_text))
                }
            }

            OutlinedButton(
                onClick = { viewModel.onIntent(LoginIntent.BiometricLoginClicked) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
                    .semantics { contentDescription = "Biometric login button" },
            ) {
                Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(text = stringResource(Res.string.login_biometric_btn_text))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { viewModel.onIntent(LoginIntent.NavigateToRegister) }) {
                    Text(stringResource(Res.string.login_register_link))
                }
            }
        }
    }
}
