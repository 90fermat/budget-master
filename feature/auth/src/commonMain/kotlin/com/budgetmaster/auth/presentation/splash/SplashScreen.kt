package com.budgetmaster.auth.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight

/**
 * Animated splash screen that resolves the initial navigation destination.
 *
 * @param viewModel The ViewModel managing auth/onboarding state.
 * @param onNavigateToOnboarding Called on first launch.
 * @param onNavigateToLogin Called when not authenticated.
 * @param onNavigateToDashboard Called when already authenticated.
 */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: () -> Unit,
) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(800, easing = EaseOutCubic))
        viewModel.effects.collect { effect ->
            when (effect) {
                SplashEffect.NavigateToOnboarding -> onNavigateToOnboarding()
                SplashEffect.NavigateToLogin -> onNavigateToLogin()
                SplashEffect.NavigateToDashboard -> onNavigateToDashboard()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().alpha(alpha.value),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "BudgetMaster",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
