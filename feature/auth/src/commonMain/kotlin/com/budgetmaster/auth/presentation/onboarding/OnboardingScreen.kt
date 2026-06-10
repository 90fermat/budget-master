package com.budgetmaster.auth.presentation.onboarding

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

private val onboardingPages = listOf(
    Triple("Track Every Penny", "Effortlessly record income and expenses across all your accounts.", "💰"),
    Triple("Smart Budgets", "Set intelligent budgets with AI-powered recommendations.", "📊"),
    Triple("Reach Your Goals", "Define savings goals and track your progress in real-time.", "🎯"),
)

/**
 * Onboarding screen that walks the user through 3 feature highlight slides.
 *
 * @param viewModel The ViewModel managing page state and navigation.
 * @param onNavigateToBiometric Called when the user completes or skips onboarding.
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onNavigateToBiometric: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingEffect.NavigateToBiometric -> onNavigateToBiometric()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(
            onClick = { viewModel.onIntent(OnboardingIntent.Skip) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Skip")
        }

        val page = onboardingPages[state.currentPage]
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.animateContentSize()
        ) {
            Text(text = page.third, style = MaterialTheme.typography.displayLarge)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = page.first,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = page.second,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(state.totalPages) { index ->
                    Surface(
                        shape = CircleShape,
                        color = if (index == state.currentPage) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(if (index == state.currentPage) 10.dp else 8.dp)
                    ) {}
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.onIntent(OnboardingIntent.NextPage) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
                    .semantics { contentDescription = "Next onboarding page" }
            ) {
                Text(if (state.currentPage == state.totalPages - 1) "Get Started" else "Next")
            }
        }
    }
}
