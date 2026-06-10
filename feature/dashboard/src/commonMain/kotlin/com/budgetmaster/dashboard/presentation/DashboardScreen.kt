package com.budgetmaster.dashboard.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.dashboard_recent_transactions
import budgetmaster.core.generated.resources.dashboard_title
import budgetmaster.core.generated.resources.dashboard_total_balance
import org.jetbrains.compose.resources.stringResource

/**
 * Composable screen representing the primary fintech Dashboard.
 * Displays accounts, cash flow charts, quick actions, and recent transaction summaries.
 *
 * @param onQuickAction Callback invoked when a quick action (add transaction, scan, transfer) is tapped.
 */
@Composable
fun DashboardScreen(
    onQuickAction: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // App bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "JD",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Hello, John",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "June 2026",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            IconButton(onClick = { onQuickAction("Notifications") }) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Glassmorphic Balance Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.03f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.dashboard_total_balance),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$12,450.80",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Custom canvas upward-arrow indicator (no extended icons needed)
                        Canvas(modifier = Modifier.size(18.dp)) {
                            val w = size.width
                            val h = size.height
                            val path = ComposePath().apply {
                                moveTo(w / 2f, 2.dp.toPx())
                                lineTo(2.dp.toPx(), h - 2.dp.toPx())
                                lineTo(w - 2.dp.toPx(), h - 2.dp.toPx())
                                close()
                            }
                            drawPath(path = path, color = Color(0xFF10B981))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+2.4%",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFeatureSettings = "tnum"
                            ),
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "this month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                label = "Add Expense",
                icon = Icons.Default.Add,
                backgroundColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.weight(1f),
                onClick = { onQuickAction("AddExpense") }
            )

            QuickActionButton(
                label = "Scan Receipt",
                icon = Icons.Default.Add,
                backgroundColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.weight(1f),
                onClick = { onQuickAction("ScanReceipt") }
            )

            QuickActionButton(
                label = "Transfer",
                icon = Icons.Default.Refresh,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                onClick = { onQuickAction("Transfer") }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Cash Flow Sparkline
        Text(
            text = "Cash Flow",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .height(160.dp)
                .padding(16.dp)
        ) {
            CashFlowChart()
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Recent Transactions Header
        Text(
            text = stringResource(Res.string.dashboard_recent_transactions),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Transaction items list
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TransactionRow(
                description = "Starbucks Coffee",
                date = "Today, 10:45 AM",
                amount = "$4.50",
                isExpense = true,
                categoryColor = Color(0xFFF59E0B)
            )

            TransactionRow(
                description = "Salary Inflow",
                date = "Yesterday, 8:00 AM",
                amount = "$2,500.00",
                isExpense = false,
                categoryColor = Color(0xFF10B981)
            )

            TransactionRow(
                description = "Chevron Gasoline",
                date = "June 4, 3:15 PM",
                amount = "$45.00",
                isExpense = true,
                categoryColor = Color(0xFF3B82F6)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(54.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = contentColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun CashFlowChart() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Draw grid
        for (i in 1..3) {
            val y = h * (i * 0.25f)
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
        }

        // Draw trend line
        val path = ComposePath().apply {
            moveTo(0f, h * 0.8f)
            lineTo(w * 0.2f, h * 0.7f)
            lineTo(w * 0.4f, h * 0.45f)
            lineTo(w * 0.6f, h * 0.5f)
            lineTo(w * 0.8f, h * 0.25f)
            lineTo(w, h * 0.15f)
        }

        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF6366F1), Color(0xFF10B981))
            ),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun TransactionRow(
    description: String,
    date: String,
    amount: String,
    isExpense: Boolean,
    categoryColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.15f))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }

            Text(
                text = (if (isExpense) "-" else "+") + amount,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFeatureSettings = "tnum"
                ),
                fontWeight = FontWeight.Bold,
                color = if (isExpense) MaterialTheme.colorScheme.error else Color(0xFF10B981)
            )
        }
    }
}
