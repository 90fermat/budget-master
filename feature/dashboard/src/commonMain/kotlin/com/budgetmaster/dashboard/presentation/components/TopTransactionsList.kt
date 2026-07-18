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
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.dashboard_recent_transactions
import budgetmaster.core.generated.resources.dashboard_no_recent_transactions
import budgetmaster.core.generated.resources.dashboard_view_all
import budgetmaster.core.generated.resources.action_delete
import budgetmaster.core.generated.resources.dashboard_transaction_subtitle
import org.jetbrains.compose.resources.stringResource
import com.budgetmaster.core.designsystem.SurfaceLevel
import com.budgetmaster.core.designsystem.components.AppCard
import com.budgetmaster.core.designsystem.categoryAccentFor
import com.budgetmaster.core.designsystem.financialColors
import com.budgetmaster.core.designsystem.categoryIconFor
import com.budgetmaster.core.designsystem.categoryNameFor
import com.budgetmaster.core.model.Transaction
import com.budgetmaster.core.util.dateTimeLabel
import com.budgetmaster.core.util.rememberHaptics

/**
 * Icon and accent for a transaction's category, from the shared design system.
 *
 * Replaces a local palette that matched on category *names* ("food", "starbucks") while
 * [Transaction.category] actually carries the category **id** ("cat_food") — so nothing ever
 * matched and every row silently drew the generic fallback.
 */
@Composable
private fun categoryVisualsFor(categoryId: String): Pair<ImageVector, Color> =
    categoryIconFor(categoryId) to categoryAccentFor(categoryId)

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
    AppCard(modifier = modifier, level = SurfaceLevel.Raised) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.dashboard_recent_transactions),
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
                        text = stringResource(Res.string.dashboard_no_recent_transactions),
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
                    text = stringResource(Res.string.dashboard_view_all),
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
    val haptics = rememberHaptics()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                // Confirm the destructive gesture in the hand before the row disappears.
                haptics.longPress()
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
                    contentDescription = stringResource(Res.string.action_delete),
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        content = {
            val (icon, categoryColor) = categoryVisualsFor(transaction.category)
            val isExpense = transaction.amount < 0
            val absAmount = if (isExpense) -transaction.amount else transaction.amount

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
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
                                text = stringResource(
                                    Res.string.dashboard_transaction_subtitle,
                                    categoryNameFor(transaction.category, transaction.category),
                                    dateTimeLabel(transaction.timestamp),
                                ),
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
                        color = if (isExpense) {
                            MaterialTheme.financialColors.expense
                        } else {
                            MaterialTheme.financialColors.income
                        }
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
