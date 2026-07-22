package com.budgetmaster.auth.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.splash_credit_from
import com.budgetmaster.core.designsystem.AppLogoMark
import com.budgetmaster.core.designsystem.AppWordmark
import com.budgetmaster.core.designsystem.financialColors
import com.budgetmaster.core.util.isReducedMotionEnabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

/** Minimum time the splash stays visible so its entrance animation is appreciated. */
private const val MIN_SPLASH_MILLIS = 2000L

/**
 * Premium animated splash: the brand mark scales in with a soft overshoot over a glowing
 * backdrop, the wordmark and an accent line reveal in sequence, and a "by FoyangTech"
 * credit fades in — then the resolved destination is navigated to.
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
    // Respect the OS "reduce motion" setting: start fully revealed and skip the staggered
    // reveal entirely rather than animating to the same place.
    val reducedMotion = isReducedMotionEnabled()

    val markScale = remember(reducedMotion) { Animatable(if (reducedMotion) 1f else 0.7f) }
    val contentAlpha = remember(reducedMotion) { Animatable(if (reducedMotion) 1f else 0f) }
    val wordmarkAlpha = remember(reducedMotion) { Animatable(if (reducedMotion) 1f else 0f) }
    val accentScale = remember(reducedMotion) { Animatable(if (reducedMotion) 1f else 0f) }
    val creditAlpha = remember(reducedMotion) { Animatable(if (reducedMotion) 1f else 0f) }

    // Gentle infinite glow pulse behind the mark; held steady under reduced motion.
    val animatedGlow by rememberInfiniteTransition(label = "glow").animateFloatValue()
    val glow = if (reducedMotion) 1f else animatedGlow

    LaunchedEffect(reducedMotion) {
        if (!reducedMotion) {
            launch {
                markScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
            }
            launch { contentAlpha.animateTo(1f, tween(600, easing = EaseOutCubic)) }
            launch {
                delay(280.milliseconds)
                wordmarkAlpha.animateTo(1f, tween(500, easing = EaseOutCubic))
            }
            launch {
                delay(520.milliseconds)
                accentScale.animateTo(1f, tween(500, easing = EaseOutCubic))
            }
            launch {
                delay(820.milliseconds)
                creditAlpha.animateTo(1f, tween(500))
            }
        }

        delay(MIN_SPLASH_MILLIS.milliseconds)
        viewModel.effects.collect { effect ->
            when (effect) {
                SplashEffect.NavigateToOnboarding -> onNavigateToOnboarding()
                SplashEffect.NavigateToLogin -> onNavigateToLogin()
                SplashEffect.NavigateToDashboard -> onNavigateToDashboard()
            }
        }
    }

    SplashContent(
        markScale = markScale.value,
        contentAlpha = contentAlpha.value,
        wordmarkAlpha = wordmarkAlpha.value,
        accentScale = accentScale.value,
        creditAlpha = creditAlpha.value,
        glow = glow,
    )
}

/**
 * The splash picture, with the reveal expressed as plain numbers.
 *
 * Separated from [SplashScreen] so it can be rendered without a ViewModel, a coroutine or a clock —
 * which is what lets a screenshot test pin it. Nothing here decides anything; the defaults are the
 * fully revealed end state, which is the frame worth pinning.
 */
@Composable
fun SplashContent(
    markScale: Float = 1f,
    contentAlpha: Float = 1f,
    wordmarkAlpha: Float = 1f,
    accentScale: Float = 1f,
    creditAlpha: Float = 1f,
    glow: Float = 1f,
) {
    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.16f * glow), background.copy(alpha = 0f)),
                        center = Offset(size.width / 2f, size.height * 0.42f),
                        radius = size.minDimension * 0.75f,
                    )
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppLogoMark(
                modifier = Modifier
                    .size(104.dp)
                    .graphicsLayer {
                        scaleX = markScale
                        scaleY = markScale
                        alpha = contentAlpha
                    }
            )
            Spacer(Modifier.height(24.dp))
            AppWordmark(
                modifier = Modifier.alpha(wordmarkAlpha),
                fontSize = 34.sp,
            )
            Spacer(Modifier.height(14.dp))
            // Accent line that draws in beneath the wordmark.
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(4.dp)
                    .graphicsLayer { scaleX = accentScale }
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(primary, MaterialTheme.financialColors.income)
                        )
                    )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .alpha(creditAlpha),
        ) {
            Text(
                text = stringResource(Res.string.splash_credit_from),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "FoyangTech",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    brush = Brush.horizontalGradient(
                        listOf(primary, MaterialTheme.financialColors.income)
                    ),
                ),
            )
        }
    }
}

/** A 0.6→1→0.6 eased pulse used for the background glow alpha. */
@Composable
private fun androidx.compose.animation.core.InfiniteTransition.animateFloatValue() =
    animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )
