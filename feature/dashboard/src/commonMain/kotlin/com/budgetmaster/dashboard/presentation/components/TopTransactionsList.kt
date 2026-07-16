@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.dashboard.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgetmaster.core.model.Transaction
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Maps a category name to a corresponding Material Icon and theme color.
 *
 * @param category The string category name.
 * @return A [Pair] containing the [ImageVector] icon and [Color] style.
 */
fun getCategoryIconAndColor(category: String): Pair<ImageVector, Color> {
    return when (category.lowercase()) {
        "food", "food & dining", "starbucks", "dining" -> Icons.Default.Fastfood to Color(0xFFF59E0B) // Amber
        "bills", "housing", "rent", "utilities" -> Icons.Default.ReceiptLong to Color(0xFF3B82F6) // Blue
        "entertainment", "leisure", "movies" -> Icons.Default.LocalActivity to Color(0xFF8B5CF6) // Purple
        "shopping", "clothing" -> Icons.Default.ShoppingBag to Color(0xFFEC4899) // Pink
        "travel", "transport", "gas", "chevron" -> Icons.Default.DirectionsCar to Color(0xFF14B8A6) // Teal
        "salary", "income", "inflow" -> Icons.Default.Savings to Color(0xFF10B981) // Emerald Green
        else -> Icons.Default.Payment to Color(0xFF94A3B8) // Slate Gray fallback
    }
}

/**
 * Formats an epoch millisecond timestamp into a clean user-facing format.
 *
 * @param timestamp The timestamp in epoch milliseconds.
 * @return Formatted string (e.g. "Jun 20, 10:45 AM").
 */
fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val month = localDateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    val day = localDateTime.dayOfMonth
    val hour = localDateTime.hour
    val minute = localDateTime.minute.toString().padStart(2, '0')
    val amPm = if (hour >= 12) "PM" else "AM"
    val hourFormatted = if (hour % 12 == 0) 12 else hour % 12
    return "$month $day, $hourFormatted:$minute $amPm"
}

/**
 * Renders a list of the user's most recent transactions within an ElevatedCard.
 * Supports Swipe-to-Dismiss action on each row.
 *
 * @param transactions The list of transactions.
 * @param onTransactionSwiped Callback invoked when a transaction is swiped-to-dismiss.
 * @param onViewAllClicked Callback invoked when "Voir tout" button is tapped.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
fun TopTransactionsList(
    transactions: List<Transaction>,
    currencyCode: String,
    onTransactionSwiped: (String) -> Unit,
    onViewAllClicked: () -> Unit,
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
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent transactions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = transactions,
                        key = { it.id }
                    ) { transaction ->
                        DismissibleTransactionItem(
                            transaction = transaction,
                            currencyCode = currencyCode,
                            onSwiped = onTransactionSwiped
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onViewAllClicked,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Voir tout",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Wraps a transaction row item with SwipeToDismissBox behavior to support swipe-to-delete.
 *
 * @param transaction The transaction data.
 * @param onSwiped Callback containing transaction ID to trigger delete action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DismissibleTransactionItem(
    transaction: Transaction,
    currencyCode: String,
    onSwiped: (String) -> Unit
) {
    val currentOnSwiped by rememberUpdatedState(onSwiped)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                currentOnSwiped(transaction.id)
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                    else -> Color.Transparent
                }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Transaction",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        content = {
            val (icon, categoryColor) = getCategoryIconAndColor(transaction.category)
            val isExpense = transaction.amount < 0
            val absAmount = if (isExpense) -transaction.amount else transaction.amount

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(categoryColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = categoryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = transaction.description,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${transaction.category} • ${formatTimestamp(transaction.timestamp)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Text(
                        text = (if (isExpense) "-" else "+") + formatCurrency(absAmount, currencyCode),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = FontWeight.Bold,
                        color = if (isExpense) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                    )
                }
            }
        }
    )
}

@PreviewLightDark
@Composable
fun TopTransactionsListPreview() {
    val sampleTransactions = listOf(
        Transaction("tx_1", -4.50, "Food", "Starbucks Coffee", 1781958000000L),
        Transaction("tx_2", 2500.00, "Salary", "Monthly Inflow", 1781950000000L),
        Transaction("tx_3", -45.00, "Travel", "Chevron Gas", 1781940000000L),
        Transaction("tx_4", -128.40, "Shopping", "Supermarket Grocery", 1781930000000L)
    )

    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TopTransactionsList(
                transactions = sampleTransactions,
                currencyCode = "USD",
                onTransactionSwiped = {},
                onViewAllClicked = {}
            )
        }
    }
}
