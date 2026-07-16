package com.budgetmaster.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.util.isReducedMotionEnabled

/**
 * The shared shimmer brush for loading placeholders (DESIGN_SYSTEM.md §6).
 *
 * Under reduced motion it returns a flat surface tint rather than a sweeping gradient — the
 * placeholder still reads as "loading" without an animation looping under the user.
 */
@Composable
fun rememberShimmerBrush(sweepWidth: Float = 1000f): Brush {
    val base = MaterialTheme.colorScheme.surfaceVariant
    if (isReducedMotionEnabled()) {
        return Brush.linearGradient(listOf(base.copy(alpha = 0.4f), base.copy(alpha = 0.4f)))
    }

    val colors = listOf(base.copy(alpha = 0.6f), base.copy(alpha = 0.2f), base.copy(alpha = 0.6f))
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate = transition.animateFloat(
        initialValue = 0f,
        targetValue = sweepWidth,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslation",
    )
    return Brush.linearGradient(
        colors = colors,
        start = Offset.Zero,
        end = Offset(x = translate.value, y = translate.value),
    )
}

/**
 * Placeholder rows shaped like the transaction/account lists they stand in for, so the layout
 * doesn't jump when real content arrives.
 *
 * @param rows how many placeholder rows to draw.
 */
@Composable
fun ShimmerListPlaceholder(
    modifier: Modifier = Modifier,
    rows: Int = 5,
) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        repeat(rows) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth().height(64.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Spacer(Modifier.size(40.dp).clip(CircleShape).background(brush))
                Spacer(Modifier.width(Spacing.compact))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Spacer(Modifier.width(160.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                    Spacer(Modifier.width(96.dp).height(10.dp).clip(RoundedCornerShape(5.dp)).background(brush))
                }
            }
        }
    }
}
