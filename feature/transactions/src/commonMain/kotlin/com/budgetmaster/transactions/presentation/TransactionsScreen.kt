package com.budgetmaster.transactions.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.transactions_search_placeholder
import budgetmaster.core.generated.resources.transactions_title
import org.jetbrains.compose.resources.stringResource

/**
 * Composable screen representing the Transactions history and search log.
 */
@Composable
fun TransactionsScreen() {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.transactions_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text(stringResource(Res.string.transactions_search_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text("All") }
                )
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text("Food") }
                )
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text("Bills") }
                )
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text("Travel") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Chronological List Group
            Text(
                text = "Today",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TransactionItemRow(
                    description = "Starbucks Coffee",
                    time = "10:45 AM",
                    amount = "$4.50",
                    isExpense = true,
                    categoryColor = Color(0xFFF59E0B)
                )

                TransactionItemRow(
                    description = "Chevron Gas",
                    time = "9:15 AM",
                    amount = "$45.00",
                    isExpense = true,
                    categoryColor = Color(0xFF3B82F6)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Yesterday",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TransactionItemRow(
                    description = "Salary Deposit",
                    time = "8:00 AM",
                    amount = "$2,500.00",
                    isExpense = false,
                    categoryColor = Color(0xFF10B981)
                )

                TransactionItemRow(
                    description = "Supermarket Grocery",
                    time = "Yesterday",
                    amount = "$128.40",
                    isExpense = true,
                    categoryColor = Color(0xFFEC4899)
                )
            }
        }

        // Receipt scan FAB
        FloatingActionButton(
            onClick = {},
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Scan Receipt")
        }
    }
}

@Composable
private fun TransactionItemRow(
    description: String,
    time: String,
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
                        text = time,
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
