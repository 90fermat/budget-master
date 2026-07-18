@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.budgets.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.time.Clock
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.a11y_goal_complete
import budgetmaster.core.generated.resources.a11y_goal_progress
import budgetmaster.core.generated.resources.goals_add_funds
import budgetmaster.core.generated.resources.goals_completed
import budgetmaster.core.generated.resources.goals_projected_behind
import budgetmaster.core.generated.resources.goals_projected_on_track
import budgetmaster.core.generated.resources.goals_projected_unknown
import budgetmaster.core.generated.resources.goals_saved_of
import budgetmaster.core.generated.resources.goals_withdraw
import com.budgetmaster.core.designsystem.SurfaceLevel
import com.budgetmaster.core.designsystem.components.AppCard
import com.budgetmaster.budgets.domain.model.GoalItem
import com.budgetmaster.core.designsystem.FinancialTextStyles
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.designsystem.financialColors
import com.budgetmaster.core.util.DateUtils
import com.budgetmaster.core.util.MoneyFormatter
import org.jetbrains.compose.resources.stringResource

/** A savings-goal card: progress ring bar, saved-of-target, target date, and Add funds. */
@Composable
internal fun GoalCard(
    item: GoalItem,
    currencyCode: String,
    onClick: () -> Unit,
    onContribute: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(item.progress)
    val now = Clock.System.now().toEpochMilliseconds()
    val projected = item.projectedCompletionAt(now)

    AppCard(modifier = modifier, level = SurfaceLevel.Raised, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                // A vector, not the 🎯 emoji it replaced: emoji draw as tofu boxes on Wasm.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Flag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(Spacing.compact))
                    Text(
                        text = DateUtils.toLocalDate(item.targetDate).toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = if (item.isCompleted) stringResource(Res.string.goals_completed) else "${(item.progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (item.isCompleted) MaterialTheme.financialColors.income else MaterialTheme.colorScheme.tertiary,
            )
        }

        Spacer(Modifier.height(Spacing.compact))
        // The bar alone says nothing to a screen reader, so it carries the numbers in words.
        // Scoped to the indicator rather than merged into the whole card, which would swallow
        // the Add funds / Withdraw buttons as separate targets.
        val progressDescription = if (item.isCompleted) {
            stringResource(
                Res.string.a11y_goal_complete,
                item.name,
                MoneyFormatter.format(item.currentAmount, currencyCode),
            )
        } else {
            stringResource(
                Res.string.a11y_goal_progress,
                item.name,
                MoneyFormatter.format(item.currentAmount, currencyCode),
                MoneyFormatter.format(item.targetAmount, currencyCode),
                (item.progress * 100).roundToInt(),
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .semantics { contentDescription = progressDescription },
            color = if (item.isCompleted) MaterialTheme.financialColors.income else MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        Spacer(Modifier.height(Spacing.small))

        // Projected completion, extrapolated from the saving rate so far.
        if (!item.isCompleted) {
            Text(
                text = when (projected) {
                    null -> stringResource(Res.string.goals_projected_unknown)
                    else -> stringResource(
                        if (item.isOnTrack(now)) {
                            Res.string.goals_projected_on_track
                        } else {
                            Res.string.goals_projected_behind
                        },
                        DateUtils.toLocalDate(projected).toString(),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (projected != null && !item.isOnTrack(now)) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(Modifier.height(Spacing.compact))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    Res.string.goals_saved_of,
                    MoneyFormatter.format(item.currentAmount, currencyCode),
                    MoneyFormatter.format(item.targetAmount, currencyCode),
                ),
                style = FinancialTextStyles.amountList,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.compact)) {
                if (item.currentAmount > 0.0) {
                    TextButton(onClick = onWithdraw, shape = RoundedCornerShape(12.dp)) {
                        Text(stringResource(Res.string.goals_withdraw))
                    }
                }
                if (!item.isCompleted) {
                    FilledTonalButton(onClick = onContribute, shape = RoundedCornerShape(12.dp)) {
                        Text(stringResource(Res.string.goals_add_funds))
                    }
                }
            }
        }
    }
}
