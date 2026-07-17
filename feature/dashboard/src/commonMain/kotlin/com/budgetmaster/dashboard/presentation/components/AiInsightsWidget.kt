@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.dashboard.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.dashboard_ai_insights
import budgetmaster.core.generated.resources.dashboard_no_insights
import budgetmaster.core.generated.resources.action_retry
import budgetmaster.core.generated.resources.dashboard_insight_open
import budgetmaster.core.generated.resources.dashboard_insight_dismiss
import org.jetbrains.compose.resources.stringResource
import com.budgetmaster.core.designsystem.components.rememberShimmerBrush
import com.budgetmaster.core.designsystem.financialColors
import com.budgetmaster.dashboard.domain.model.Insight
import com.budgetmaster.dashboard.domain.model.InsightType
import com.budgetmaster.dashboard.presentation.InsightsState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Personal finance AI Insights widget.
 * Dynamically switches layouts based on the load state of insights (Loading / Success / Error).
 *
 * @param insightsState The current asynchronous state of the AI insights.
 * @param onInsightClicked Callback containing navigation route triggered on insight tap.
 * @param onInsightDismissed Callback containing insight ID to trigger dismissal.
 * @param onRetry Callback invoked to refresh/retry fetching insights.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
fun AiInsightsWidget(
    insightsState: InsightsState,
    onInsightClicked: (String) -> Unit,
    onInsightDismissed: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.dashboard_ai_insights),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        when (insightsState) {
            // The dashboard omits this whole widget when AI is unavailable; the branch exists so
            // the `when` stays exhaustive if some other caller renders it directly.
            is InsightsState.Unavailable -> Unit
            is InsightsState.Loading -> {
                ShimmerCards()
            }
            is InsightsState.Success -> {
                if (insightsState.data.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.dashboard_no_insights),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        insightsState.data.forEach { insight ->
                            InsightCard(
                                insight = insight,
                                onClick = { insight.actionRoute?.let(onInsightClicked) },
                                onDismiss = { onInsightDismissed(insight.id) }
                            )
                        }
                    }
                }
            }
            is InsightsState.Error -> {
                ErrorState(message = insightsState.message, onRetry = onRetry)
            }
        }
    }
}

@Composable
private fun ShimmerCards() {
    // Uses the shared brush, which flattens to a static tint under reduced motion instead of
    // looping a sweep under the user.
    val brush = rememberShimmerBrush()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(brush)
            )
        }
    }
}

@Composable
private fun InsightCard(
    insight: Insight,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Semantic tokens rather than fixed hexes: a spending alert reads as a warning, a saving
    // opportunity as positive, and a trend as neutral information — and each now follows the
    // user's selected palette instead of staying indigo/amber in all five.
    val leftBorderColor = when (insight.type) {
        InsightType.SPENDING -> MaterialTheme.financialColors.warning
        InsightType.SAVING -> MaterialTheme.financialColors.income
        InsightType.TREND -> MaterialTheme.colorScheme.primary
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = insight.actionRoute != null, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min), // Forces the left-bar indicator to match details height
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type-colored Left Border/Indicator Bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(leftBorderColor)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = insight.type.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = leftBorderColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = insight.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (insight.actionRoute != null) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = stringResource(Res.string.dashboard_insight_open),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(Res.string.dashboard_insight_dismiss),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(Res.string.action_retry))
            }
        }
    }
}

@PreviewLightDark
@Composable
fun AiInsightsWidgetPreview() {
    val sampleInsights = listOf(
        Insight("1", InsightType.SPENDING, "Your spending on Starbucks Coffee has increased by 15% this week. Consider adjusting your dining budget.", "transactions", Clock.System.now()),
        Insight("2", InsightType.SAVING, "Great job! You reached 80% of your holiday savings goal.", null, Clock.System.now())
    )

    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AiInsightsWidget(
                insightsState = InsightsState.Success(sampleInsights),
                onInsightClicked = {},
                onInsightDismissed = {},
                onRetry = {}
            )
        }
    }
}
