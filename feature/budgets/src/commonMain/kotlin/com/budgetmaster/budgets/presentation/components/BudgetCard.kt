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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgetmaster.budgets.domain.model.BudgetItem
import com.budgetmaster.budgets.domain.model.BudgetStatus
import com.budgetmaster.core.designsystem.FinancialTextStyles
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.designsystem.categoryIconFor
import com.budgetmaster.core.designsystem.categoryNameFor
import com.budgetmaster.core.designsystem.parseHexColor
import com.budgetmaster.core.designsystem.financialColors
import com.budgetmaster.core.util.MoneyFormatter
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.a11y_budget_gauge
import budgetmaster.core.generated.resources.budgets_left
import budgetmaster.core.generated.resources.budgets_status_exceeded
import budgetmaster.core.generated.resources.budgets_status_ok
import budgetmaster.core.generated.resources.budgets_status_warning
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

    // A gauge is meaningless to a screen reader: the bar is a shape and the colour carries the
    // status. Merge the card into one description that states the numbers and the status in
    // words, so it reads the same as it looks.
    val statusText = stringResource(
        when (item.status) {
            BudgetStatus.OK -> Res.string.budgets_status_ok
            BudgetStatus.WARNING -> Res.string.budgets_status_warning
            BudgetStatus.EXCEEDED -> Res.string.budgets_status_exceeded
        },
    )
    val categoryName = categoryNameFor(item.category.id, item.category.name)
    val gaugeDescription = stringResource(
        Res.string.a11y_budget_gauge,
        categoryName,
        MoneyFormatter.format(item.spent, currencyCode),
        MoneyFormatter.format(item.limit, currencyCode),
        (item.ratio * 100).roundToInt(),
        statusText,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = gaugeDescription }
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
                ) {
                    Icon(
                        imageVector = categoryIconFor(item.category.id),
                        // Decorative: the merged description already names the category.
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(Spacing.compact))
                Text(
                    text = categoryName,
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
