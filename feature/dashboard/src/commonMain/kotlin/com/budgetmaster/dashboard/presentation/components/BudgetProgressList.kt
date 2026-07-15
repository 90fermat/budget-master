package com.budgetmaster.dashboard.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetmaster.dashboard.domain.model.BudgetProgress
import com.budgetmaster.dashboard.domain.model.BudgetStatus

/**
 * Renders a list of active category budgets and their spending progress.
 *
 * @param budgets The list of category budget progress to display.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
fun BudgetProgressList(
    budgets: List<BudgetProgress>,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Budgets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (budgets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active budgets set",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                // Fixed height or wrapping container for list items inside a card
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    budgets.forEach { budget ->
                        BudgetProgressItem(budget = budget)
                    }
                }
            }
        }
    }
}

/**
 * A single category budget row displaying progress.
 * Uses animateFloatAsState to animate the LinearProgressIndicator.
 * Displays category emoji, name, limits, and progress values.
 *
 * @param budget The budget progress data for a single category.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
fun BudgetProgressItem(
    budget: BudgetProgress,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = budget.percentage.toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600)
    )

    val isOverBudget = budget.percentage > 1.0
    val progressColor = if (isOverBudget) {
        MaterialTheme.colorScheme.error
    } else if (budget.percentage >= 0.8) {
        Color(0xFFF59E0B) // Warning color (Amber)
    } else {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = budget.categoryEmoji ?: "💰",
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = budget.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${formatCurrency(budget.spent)} of ${formatCurrency(budget.limit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
    }
}

@PreviewLightDark
@Composable
fun BudgetProgressListPreview() {
    val sampleBudgets = listOf(
        BudgetProgress("1", "Food & Dining", "🍔", 450.0, 500.0, 0.9, BudgetStatus.WARNING),
        BudgetProgress("2", "Rent & Bills", "🏠", 1200.0, 1200.0, 1.0, BudgetStatus.OK),
        BudgetProgress("3", "Entertainment", "🎬", 180.0, 150.0, 1.2, BudgetStatus.EXCEEDED),
        BudgetProgress("4", "Shopping", "🛍️", 50.0, 300.0, 0.16, BudgetStatus.OK)
    )

    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            BudgetProgressList(budgets = sampleBudgets)
        }
    }
}
