@file:OptIn(ExperimentalMaterial3Api::class)

package com.budgetmaster.transactions.presentation

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.transactions_deleted
import budgetmaster.core.generated.resources.transactions_empty_filtered
import budgetmaster.core.generated.resources.transactions_empty_subtitle
import budgetmaster.core.generated.resources.transactions_empty_title
import budgetmaster.core.generated.resources.transactions_filter_all
import budgetmaster.core.generated.resources.transactions_filter_expense
import budgetmaster.core.generated.resources.transactions_filter_income
import budgetmaster.core.generated.resources.transactions_search_placeholder
import budgetmaster.core.generated.resources.transactions_title
import budgetmaster.core.generated.resources.transactions_today
import budgetmaster.core.generated.resources.transactions_uncategorized
import budgetmaster.core.generated.resources.transactions_undo
import budgetmaster.core.generated.resources.transactions_yesterday
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.designsystem.categoryIconFor
import com.budgetmaster.core.util.MoneyFormatter
import com.budgetmaster.core.util.RelativeDay
import com.budgetmaster.core.util.formatSigned
import com.budgetmaster.transactions.domain.model.TypeFilter
import com.budgetmaster.transactions.presentation.components.AddEditTransactionForm
import com.budgetmaster.transactions.presentation.components.TransactionRowItem
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Transactions screen: searchable, filterable, day-grouped list backed by live
 * SQLDelight data, with swipe-to-delete + undo and an adaptive add/edit editor
 * (bottom sheet on phones, centered dialog on wide layouts).
 */
@Composable
fun TransactionsScreen(viewModel: TransactionsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val undoLabel = stringResource(Res.string.transactions_undo)
    LaunchedEffectEffects(viewModel, snackbarHostState, undoLabel)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onIntent(TransactionsIntent.AddClicked) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.transactions_title))
            }
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
                text = stringResource(Res.string.transactions_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.medium))

            OutlinedTextField(
                value = state.query,
                onValueChange = { viewModel.onIntent(TransactionsIntent.SearchChanged(it)) },
                placeholder = { Text(stringResource(Res.string.transactions_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Spacing.medium))
            TypeAndCategoryFilters(state, viewModel)
            Spacer(Modifier.height(Spacing.medium))

            when {
                state.isEmpty -> EmptyState(filtered = !state.query.isBlank() || state.categoryFilterId != null || state.typeFilter != TypeFilter.ALL)
                else -> TransactionList(state, viewModel)
            }
        }

        if (state.editor.visible) {
            TransactionEditor(state, viewModel)
        }
    }
}

@Composable
private fun TypeAndCategoryFilters(state: TransactionsState, viewModel: TransactionsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            FilterChip(
                selected = state.typeFilter == TypeFilter.ALL,
                onClick = { viewModel.onIntent(TransactionsIntent.TypeFilterChanged(TypeFilter.ALL)) },
                label = { Text(stringResource(Res.string.transactions_filter_all)) },
            )
            FilterChip(
                selected = state.typeFilter == TypeFilter.INCOME,
                onClick = { viewModel.onIntent(TransactionsIntent.TypeFilterChanged(TypeFilter.INCOME)) },
                label = { Text(stringResource(Res.string.transactions_filter_income)) },
            )
            FilterChip(
                selected = state.typeFilter == TypeFilter.EXPENSE,
                onClick = { viewModel.onIntent(TransactionsIntent.TypeFilterChanged(TypeFilter.EXPENSE)) },
                label = { Text(stringResource(Res.string.transactions_filter_expense)) },
            )
        }
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            items(state.categories) { category ->
                FilterChip(
                    selected = state.categoryFilterId == category.id,
                    onClick = {
                        val next = if (state.categoryFilterId == category.id) null else category.id
                        viewModel.onIntent(TransactionsIntent.CategoryFilterChanged(next))
                    },
                    label = { Text(category.name) },
                    leadingIcon = {
                        Icon(
                            imageVector = categoryIconFor(category.id),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun TransactionList(state: TransactionsState, viewModel: TransactionsViewModel) {
    val uncategorized = stringResource(Res.string.transactions_uncategorized)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        state.groups.forEach { group ->
            item(key = "header_${group.date}") {
                DayHeader(group, state.currencyCode)
            }
            items(group.items, key = { it.id }) { item ->
                TransactionRowItem(
                    item = item,
                    currencyCode = state.currencyCode,
                    categoryLabel = item.category?.name ?: uncategorized,
                    onClick = { viewModel.onIntent(TransactionsIntent.EditClicked(item)) },
                    onDelete = { viewModel.onIntent(TransactionsIntent.DeleteRequested(item.id)) },
                )
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun DayHeader(group: TransactionDayGroup, currencyCode: String) {
    val label = when (group.relative) {
        RelativeDay.TODAY -> stringResource(Res.string.transactions_today)
        RelativeDay.YESTERDAY -> stringResource(Res.string.transactions_yesterday)
        RelativeDay.OLDER -> group.date.toString()
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Text(
            text = MoneyFormatter.formatSigned(group.net, currencyCode),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyState(filtered: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("💸", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(Spacing.medium))
            Text(
                text = stringResource(Res.string.transactions_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.small))
            Text(
                text = stringResource(
                    if (filtered) Res.string.transactions_empty_filtered else Res.string.transactions_empty_subtitle
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TransactionEditor(state: TransactionsState, viewModel: TransactionsViewModel) {
    BoxWithConstraints {
        val isCompact = maxWidth < 600.dp
        val form: @Composable () -> Unit = {
            AddEditTransactionForm(
                editing = state.editor.editing,
                categories = state.categories,
                accounts = state.accounts,
                activeAccountId = state.activeAccountId,
                onSave = { viewModel.onIntent(TransactionsIntent.SaveTransaction(it)) },
                onCancel = { viewModel.onIntent(TransactionsIntent.EditorDismissed) },
            )
        }
        if (isCompact) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.onIntent(TransactionsIntent.EditorDismissed) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
            ) { form() }
        } else {
            Dialog(onDismissRequest = { viewModel.onIntent(TransactionsIntent.EditorDismissed) }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.width(480.dp),
                ) { form() }
            }
        }
    }
}

/** Collects one-shot effects into the snackbar host (with an Undo action). */
@Composable
private fun LaunchedEffectEffects(
    viewModel: TransactionsViewModel,
    snackbarHostState: SnackbarHostState,
    undoLabel: String,
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TransactionsEffect.ShowUndoDelete -> {
                    val message = getString(Res.string.transactions_deleted, effect.description)
                    val result = snackbarHostState.showSnackbar(message = message, actionLabel = undoLabel)
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(TransactionsIntent.UndoDelete)
                    }
                }
                is TransactionsEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }
}
