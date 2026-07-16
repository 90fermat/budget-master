package com.budgetmaster.budgets.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgetmaster.budgets.domain.model.BudgetItem
import com.budgetmaster.budgets.domain.model.BudgetStatus
import com.budgetmaster.core.designsystem.FinancialTextStyles
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.designsystem.financialColors
import com.budgetmaster.core.util.MoneyFormatter
import org.jetbrains.compose.resources.stringResource
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.budgets_left
import budgetmaster.core.generated.resources.budgets_over
import budgetmaster.core.generated.resources.budgets_spent_of

/**
 * A budget gauge card: category avatar, spent-of-limit, and a status-colored progress
 * bar (green → amber → red). Tapping opens the editor.
 */
@Composable
internal fun BudgetCard(
    item: BudgetItem,
    currencyCode: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColor = when (item.status) {
        BudgetStatus.OK -> MaterialTheme.financialColors.income
        BudgetStatus.WARNING -> MaterialTheme.financialColors.warning
        BudgetStatus.EXCEEDED -> MaterialTheme.financialColors.expense
    }
    val barColor by animateColorAsState(statusColor)
    val progress by animateFloatAsState(item.progress)
    val accent = parseHexColor(item.category.colorHex, MaterialTheme.colorScheme.primary)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(Spacing.medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Text(item.category.icon) }
                Spacer(Modifier.width(Spacing.compact))
                Text(
                    text = item.category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = stringResource(
                    Res.string.budgets_spent_of,
                    MoneyFormatter.format(item.spent, currencyCode),
                    MoneyFormatter.format(item.limit, currencyCode),
                ),
                style = FinancialTextStyles.amountList,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Spacing.compact))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

        Spacer(Modifier.height(Spacing.small))

        val remaining = item.remaining
        val remainingText = if (remaining >= 0) {
            stringResource(Res.string.budgets_left, MoneyFormatter.format(remaining, currencyCode))
        } else {
            stringResource(Res.string.budgets_over, MoneyFormatter.format(-remaining, currencyCode))
        }
        Text(
            text = remainingText,
            style = MaterialTheme.typography.bodySmall,
            color = if (remaining >= 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.financialColors.expense,
        )
    }
}
