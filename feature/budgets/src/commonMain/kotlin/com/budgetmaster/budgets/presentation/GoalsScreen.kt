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
import budgetmaster.core.generated.resources.goals_title
import org.jetbrains.compose.resources.stringResource

/**
 * Composable screen representing Savings Goals.
 */
@Composable
fun GoalsScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(Res.string.goals_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tesla savings goal
        GoalProgressCard(
            name = "New Tesla Model 3",
            targetAmount = 45000f,
            currentAmount = 13500f,
            targetDate = "Dec 2027"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Emergency Fund savings goal
        GoalProgressCard(
            name = "Emergency Fund",
            targetAmount = 10000f,
            currentAmount = 9000f,
            targetDate = "Dec 2026"
        )
    }
}

@Composable
private fun GoalProgressCard(
    name: String,
    targetAmount: Float,
    currentAmount: Float,
    targetDate: String
) {
    val progress = (currentAmount / targetAmount).coerceIn(0f, 1f)

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
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Target: $$targetAmount ($targetDate)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFeatureSettings = "tnum"
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Saved $${currentAmount.toInt()} of $${targetAmount.toInt()}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}
