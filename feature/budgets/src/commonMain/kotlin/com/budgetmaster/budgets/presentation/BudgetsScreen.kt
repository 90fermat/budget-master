@file:OptIn(ExperimentalMaterial3Api::class)

package com.budgetmaster.budgets.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.budgets_empty_subtitle
import budgetmaster.core.generated.resources.budgets_empty_title
import budgetmaster.core.generated.resources.budgets_new
import budgetmaster.core.generated.resources.budgets_spent_of
import budgetmaster.core.generated.resources.budgets_this_month
import budgetmaster.core.generated.resources.budgets_title
import com.budgetmaster.budgets.presentation.components.AddEditBudgetForm
import com.budgetmaster.budgets.presentation.components.BudgetCard
import com.budgetmaster.core.designsystem.FinancialTextStyles
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.util.MoneyFormatter
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Budgets screen: per-category monthly budgets on live data, with a summary header,
 * create/edit editor (bottom sheet on phone, dialog on wide), and delete.
 */
@Composable
fun BudgetsScreen(viewModel: BudgetsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BudgetsEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onIntent(BudgetsIntent.AddClicked) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.budgets_new)) }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.medium),
        ) {
            Spacer(Modifier.height(Spacing.medium))
            Text(
                text = stringResource(Res.string.budgets_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.medium))

            when {
                state.isEmpty -> EmptyState()
                else -> {
                    if (state.budgets.isNotEmpty()) SummaryHeader(state)
                    Spacer(Modifier.height(Spacing.medium))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                        items(state.budgets, key = { it.id }) { item ->
                            BudgetCard(
                                item = item,
                                currencyCode = state.currencyCode,
                                onClick = { viewModel.onIntent(BudgetsIntent.EditClicked(item)) },
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }

        if (state.editor.visible) {
            BudgetEditor(state, viewModel)
        }
    }
}

@Composable
private fun SummaryHeader(state: BudgetsState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(Spacing.large),
    ) {
        Text(
            text = stringResource(Res.string.budgets_this_month),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
        )
        Spacer(Modifier.height(Spacing.micro))
        Text(
            text = stringResource(
                Res.string.budgets_spent_of,
                MoneyFormatter.format(state.totalSpent, state.currencyCode),
                MoneyFormatter.format(state.totalLimit, state.currencyCode),
            ),
            style = FinancialTextStyles.amountList.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎯", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(Spacing.medium))
            Text(
                text = stringResource(Res.string.budgets_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.small))
            Text(
                text = stringResource(Res.string.budgets_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BudgetEditor(state: BudgetsState, viewModel: BudgetsViewModel) {
    BoxWithConstraints {
        val isCompact = maxWidth < 600.dp
        val form: @Composable () -> Unit = {
            AddEditBudgetForm(
                editing = state.editor.editing,
                categories = state.categories,
                onSave = { categoryId, limit ->
                    viewModel.onIntent(BudgetsIntent.SaveBudget(categoryId, limit, state.editor.editing?.id))
                },
                onDelete = { id -> viewModel.onIntent(BudgetsIntent.DeleteRequested(id)) },
                onCancel = { viewModel.onIntent(BudgetsIntent.EditorDismissed) },
            )
        }
        if (isCompact) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.onIntent(BudgetsIntent.EditorDismissed) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
            ) { form() }
        } else {
            Dialog(onDismissRequest = { viewModel.onIntent(BudgetsIntent.EditorDismissed) }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.width(480.dp),
                ) { form() }
            }
        }
    }
}
