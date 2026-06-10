package com.budgetmaster.budgets.presentation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.budgets_title
import org.jetbrains.compose.resources.stringResource

/**
 * Composable screen representing Budgets and category caps.
 */
@Composable
fun BudgetsScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(Res.string.budgets_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Food budget
        BudgetProgressCard(
            categoryName = "Food & Dining",
            spent = 450f,
            limit = 1000f
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Rent budget
        BudgetProgressCard(
            categoryName = "Housing & Rent",
            spent = 1500f,
            limit = 1500f
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Entertainment budget
        BudgetProgressCard(
            categoryName = "Entertainment",
            spent = 320f,
            limit = 200f
        )
    }
}

@Composable
private fun BudgetProgressCard(
    categoryName: String,
    spent: Float,
    limit: Float
) {
    val progress = (spent / limit).coerceIn(0f, 1f)
    val progressColor = when {
        spent >= limit -> MaterialTheme.colorScheme.error
        spent / limit >= 0.85f -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFF10B981) // Emerald
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$${spent.toInt()} of $${limit.toInt()}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFeatureSettings = "tnum"
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
    }
}
