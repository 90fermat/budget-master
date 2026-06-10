package com.budgetmaster.auth.presentation.biometric

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Biometric setup screen offering the user the option to enable biometric authentication.
 *
 * @param viewModel The ViewModel managing biometric state.
 * @param onNavigateToHome Called when the user proceeds to the dashboard.
 */
@Composable
fun BiometricScreen(
    viewModel: BiometricViewModel,
    onNavigateToHome: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                BiometricEffect.NavigateToHome -> onNavigateToHome()
                is BiometricEffect.ShowError -> Unit
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "Biometric icon",
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text("Enable Biometric Login", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Use your fingerprint or face to sign in quickly and securely.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            if (state.errorMessage != null) {
                Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.onIntent(BiometricIntent.EnableBiometric) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
                    .semantics { contentDescription = "Enable biometric button" }
            ) {
                Text("Enable Biometric")
            }

            TextButton(
                onClick = { viewModel.onIntent(BiometricIntent.SkipBiometric) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for Now")
            }
        }
    }
}
