package com.budgetmaster.dashboard.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgetmaster.core.designsystem.financialColors
import com.budgetmaster.core.util.MoneyFormatter
import com.budgetmaster.dashboard.domain.model.BalanceSummary
import com.budgetmaster.dashboard.domain.model.BalanceTrend

/**
 * Animated counter state helper that interpolates a Double value over 800ms using EaseOutCubic easing.
 *
 * @param targetValue The end target numeric value to count up/down to.
 * @return A [State] containing the animated numeric value.
 */
@Composable
fun animateCounterAsState(targetValue: Double): State<Double> {
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(targetValue) {
        animatable.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = tween(
                durationMillis = 800,
                easing = EaseOutCubic
            )
        )
    }
    return remember { derivedStateOf { animatable.value.toDouble() } }
}

/**
 * Formats a dashboard amount in the user's selected currency.
 *
 * Delegates to the shared [MoneyFormatter] so the Dashboard matches Transactions, Budgets,
 * Goals, and Accounts — this replaced a local formatter that hardcoded a `$` prefix.
 *
 * @param amount The value to format.
 * @param currencyCode ISO currency code from the user's settings.
 */
fun formatCurrency(amount: Double, currencyCode: String): String =
    MoneyFormatter.format(amount, currencyCode)

/**
 * Premium dashboard balance summary card.
 * Uses Material 3 ElevatedCard styled with primaryContainer background.
 * Animates the total balance counter and displays monthly flow details.
 *
 * @param balanceSummary The balance data containing total, income, expense, and trend.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
fun BalanceCard(
    balanceSummary: BalanceSummary,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val animatedBalance by animateCounterAsState(balanceSummary.totalBalance)
    val isPositive = balanceSummary.balanceTrend == BalanceTrend.POSITIVE

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "TOTAL BALANCE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatCurrency(animatedBalance, currencyCode),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = if (isPositive) "Positive Trend" else "Negative Trend",
                    tint = if (isPositive) MaterialTheme.financialColors.income else MaterialTheme.financialColors.expense,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${if (balanceSummary.trendPercentage >= 0) "+" else ""}${balanceSummary.trendPercentage}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPositive) MaterialTheme.financialColors.income else MaterialTheme.financialColors.expense
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "this month",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCurrency(balanceSummary.monthlyIncome, currencyCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.financialColors.income
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Expenses",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCurrency(balanceSummary.monthlyExpenses, currencyCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.financialColors.expense
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun BalanceCardPreview() {
    MaterialTheme {
        BalanceCard(
            balanceSummary = BalanceSummary(
                totalBalance = 12450.80,
                monthlyIncome = 5320.00,
                monthlyExpenses = 2869.20,
                balanceTrend = BalanceTrend.POSITIVE,
                trendPercentage = 2.4
            ),
            currencyCode = "USD"
        )
    }
}
