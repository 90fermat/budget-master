package com.budgetmaster.dashboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.dashboard_greeting
import budgetmaster.core.generated.resources.dashboard_greeting_fallback
import com.budgetmaster.core.util.initialsOf
import com.budgetmaster.core.util.monthYearLabel
import com.budgetmaster.dashboard.presentation.components.PreviewLightDark
import com.budgetmaster.dashboard.domain.model.*
import com.budgetmaster.dashboard.presentation.components.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Dashboard screen entry-point composable.
 * Collects state from [DashboardViewModel] and delegates rendering to specialised
 * sub-composables, keeping business logic out of the UI layer.
 *
 * @param onNavigateToSettings Called when the ViewModel emits [DashboardEffect.NavigateToSettings].
 * @param onViewAllTransactions Callback to navigate to the full Transactions list screen.
 * @param onInsightNavigate Callback for navigating to an insight action route.
 */
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit = {},
    onViewAllTransactions: () -> Unit = {},
    onInsightNavigate: (String) -> Unit = {}
) {
    val viewModel: DashboardViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    // Navigation is driven by typed effects rather than stringly-typed callbacks.
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                DashboardEffect.NavigateToSettings -> onNavigateToSettings()
                DashboardEffect.NavigateToTransactions -> onViewAllTransactions()
                else -> Unit // Other effects are handled locally or not yet routed.
            }
        }
    }

    DashboardContent(
        state = state,
        onIntent = viewModel::onIntent,
        onViewAllTransactions = onViewAllTransactions,
        onInsightNavigate = onInsightNavigate
    )
}

/**
 * Pure stateless content composable for the Dashboard.
 * Receives all data from [state] and dispatches user actions via [onIntent].
 *
 * @param state The current [DashboardState] snapshot.
 * @param onIntent Dispatch function for [DashboardIntent] events.
  * @param onViewAllTransactions Navigation callback for "Voir tout" in the transactions list.
 * @param onInsightNavigate Navigation callback for AI insight action routes.
 */
@Composable
fun DashboardContent(
    state: DashboardState,
    onIntent: (DashboardIntent) -> Unit,
    onViewAllTransactions: () -> Unit = {},
    onInsightNavigate: (String) -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (state.isLoading) {
            DashboardSkeleton()
        } else {
            DashboardScrollableBody(
                state = state,
                onIntent = onIntent,
                onViewAllTransactions = onViewAllTransactions,
                onInsightNavigate = onInsightNavigate
            )
        }

        // Snackbar overlay for non-fatal errors
        if (state.error != null) {
            Snackbar(
                action = {
                    TextButton(onClick = { onIntent(DashboardIntent.RefreshRequested) }) {
                        Text("Retry")
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(state.error)
            }
        }
    }
}

@Composable
private fun DashboardScrollableBody(
    state: DashboardState,
    onIntent: (DashboardIntent) -> Unit,
    onViewAllTransactions: () -> Unit,
    onInsightNavigate: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // ── App Bar Header ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The signed-in user, not the "John Doe / June 2026" mock this replaced.
                val name = state.userName?.takeIf { it.isNotBlank() }
                    ?: stringResource(Res.string.dashboard_greeting_fallback)
                val initials = initialsOf(name)

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (initials.isNotEmpty()) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        // A name we can't initialise would leave an empty circle.
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(Res.string.dashboard_greeting, name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = monthYearLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
            IconButton(onClick = { onIntent(DashboardIntent.NotificationsClicked) }) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // ── Pull-to-refresh indicator ──────────────────────────────────────
        if (state.isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // ── Balance Card ───────────────────────────────────────────────────
        if (state.balance != null) {
            BalanceCard(balanceSummary = state.balance, currencyCode = state.currencyCode)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Quick Actions Row ──────────────────────────────────────────────
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
                onClick = { onIntent(DashboardIntent.QuickActionClicked(TransactionType.EXPENSE)) }
            )
            QuickActionButton(
                label = "Add Income",
                icon = Icons.Default.Add,
                backgroundColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.weight(1f),
                onClick = { onIntent(DashboardIntent.QuickActionClicked(TransactionType.INCOME)) }
            )
            QuickActionButton(
                label = "Transfer",
                icon = Icons.Default.Refresh,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                onClick = { onIntent(DashboardIntent.QuickActionClicked(TransactionType.TRANSFER)) }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Period Filter Chips ────────────────────────────────────────────
        PeriodFilterRow(
            selectedPeriod = state.selectedPeriod,
            onPeriodSelected = { onIntent(DashboardIntent.PeriodChanged(it)) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Spending Chart ─────────────────────────────────────────────────
        SpendingChart(chartData = state.chartData)

        Spacer(modifier = Modifier.height(28.dp))

        // ── Budget Progress List ───────────────────────────────────────────
        BudgetProgressList(budgets = state.budgets, currencyCode = state.currencyCode)

        Spacer(modifier = Modifier.height(28.dp))

        // ── Top Transactions List ──────────────────────────────────────────
        TopTransactionsList(
            transactions = state.topTransactions,
            currencyCode = state.currencyCode,
            onTransactionSwiped = { onIntent(DashboardIntent.TransactionSwiped(it)) },
            onViewAllClicked = onViewAllTransactions
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── AI Insights Widget ─────────────────────────────────────────────
        AiInsightsWidget(
            insightsState = state.insights,
            onInsightClicked = onInsightNavigate,
            onInsightDismissed = { onIntent(DashboardIntent.InsightsDismissed(it)) },
            onRetry = { onIntent(DashboardIntent.RefreshRequested) }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Quick action button with icon and label used in the Dashboard's action row.
 *
 * @param label Button text.
 * @param icon The leading icon vector.
 * @param backgroundColor The button container color.
 * @param contentColor The icon and text color.
 * @param modifier The modifier to be applied.
 * @param onClick Click callback.
 */
@Composable
private fun QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        modifier = modifier.height(54.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

/**
 * Period selector row for filtering balance and chart data.
 *
 * @param selectedPeriod The currently active [Period].
 * @param onPeriodSelected Callback invoked when a period chip is tapped.
 */
@Composable
private fun PeriodFilterRow(
    selectedPeriod: Period,
    onPeriodSelected: (Period) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Period.entries.forEach { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                label = {
                    Text(
                        text = when (period) {
                            Period.WEEK -> "Week"
                            Period.MONTH -> "Month"
                            Period.YEAR -> "Year"
                            Period.ALL -> "All"
                        }
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun DashboardContentPreview() {
    MaterialTheme {
        DashboardContent(
            state = DashboardState(
                isLoading = false,
                balance = BalanceSummary(
                    totalBalance = 12450.80,
                    monthlyIncome = 5320.00,
                    monthlyExpenses = 2869.20,
                    balanceTrend = BalanceTrend.POSITIVE,
                    trendPercentage = 2.4
                ),
                budgets = listOf(
                    BudgetProgress("1", "Food & Dining", "🍔", 450.0, 500.0, 0.9, BudgetStatus.WARNING),
                    BudgetProgress("2", "Entertainment", "🎬", 180.0, 150.0, 1.2, BudgetStatus.EXCEEDED)
                ),
                insights = InsightsState.Loading
            ),
            onIntent = {}
        )
    }
}
