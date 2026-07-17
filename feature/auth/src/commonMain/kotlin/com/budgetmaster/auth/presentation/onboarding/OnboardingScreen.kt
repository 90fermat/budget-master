package com.budgetmaster.auth.presentation.onboarding

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.onboarding_get_started
import budgetmaster.core.generated.resources.onboarding_next
import budgetmaster.core.generated.resources.onboarding_page0_desc
import budgetmaster.core.generated.resources.onboarding_page0_title
import budgetmaster.core.generated.resources.onboarding_page1_desc
import budgetmaster.core.generated.resources.onboarding_page1_title
import budgetmaster.core.generated.resources.onboarding_page2_desc
import budgetmaster.core.generated.resources.onboarding_page2_title
import budgetmaster.core.generated.resources.onboarding_skip
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Which accent color role from the theme an onboarding page uses. */
private enum class Accent { PRIMARY, TERTIARY, SECONDARY }

private data class OnboardingPage(
    val titleRes: StringResource,
    val descRes: StringResource,
    val icon: ImageVector,
    val accent: Accent,
)

private val onboardingPages = listOf(
    OnboardingPage(Res.string.onboarding_page0_title, Res.string.onboarding_page0_desc, Icons.AutoMirrored.Filled.ReceiptLong, Accent.PRIMARY),
    OnboardingPage(Res.string.onboarding_page1_title, Res.string.onboarding_page1_desc, Icons.Filled.PieChart, Accent.TERTIARY),
    OnboardingPage(Res.string.onboarding_page2_title, Res.string.onboarding_page2_desc, Icons.Filled.Savings, Accent.SECONDARY),
)

/**
 * Onboarding screen that walks the user through 3 feature-highlight slides with
 * themed vector illustrations and localized copy.
 *
 * @param viewModel The ViewModel managing page state and navigation.
 * @param onNavigateToBiometric Called on completion when biometric setup is available.
 * @param onNavigateToLogin Called on completion when biometric setup is skipped (e.g. Web).
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onNavigateToBiometric: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingEffect.NavigateToBiometric -> onNavigateToBiometric()
                OnboardingEffect.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(
            onClick = { viewModel.onIntent(OnboardingIntent.Skip) },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(stringResource(Res.string.onboarding_skip))
        }

        val page = onboardingPages[state.currentPage]
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.animateContentSize(),
        ) {
            OnboardingIllustration(icon = page.icon, accent = page.accent.color())
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = stringResource(page.titleRes),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(page.descRes),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(state.totalPages) { index ->
                    Surface(
                        shape = CircleShape,
                        color = if (index == state.currentPage) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(if (index == state.currentPage) 10.dp else 8.dp),
                    ) {}
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.onIntent(OnboardingIntent.NextPage) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text(
                    text = stringResource(
                        if (state.currentPage == state.totalPages - 1) Res.string.onboarding_get_started
                        else Res.string.onboarding_next
                    ),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** A large rounded illustration badge: a soft accent gradient with the page's icon. */
@Composable
private fun OnboardingIllustration(icon: ImageVector, accent: Color) {
    Box(
        modifier = Modifier
            .size(150.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.20f), accent.copy(alpha = 0.06f))
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(68.dp),
        )
    }
}

@Composable
private fun Accent.color(): Color = when (this) {
    Accent.PRIMARY -> MaterialTheme.colorScheme.primary
    Accent.TERTIARY -> MaterialTheme.colorScheme.tertiary
    Accent.SECONDARY -> MaterialTheme.colorScheme.secondary
}
