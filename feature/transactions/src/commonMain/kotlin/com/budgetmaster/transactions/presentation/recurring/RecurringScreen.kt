@file:OptIn(ExperimentalMaterial3Api::class)

package com.budgetmaster.transactions.presentation.recurring

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.action_delete
import budgetmaster.core.generated.resources.recurring_add
import budgetmaster.core.generated.resources.recurring_empty_subtitle
import budgetmaster.core.generated.resources.recurring_empty_title
import budgetmaster.core.generated.resources.recurring_next_run
import budgetmaster.core.generated.resources.recurring_paused
import budgetmaster.core.generated.resources.recurring_title
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.designsystem.categoryIconFor
import com.budgetmaster.core.designsystem.components.EmptyState
import com.budgetmaster.core.designsystem.components.ShimmerListPlaceholder
import com.budgetmaster.core.designsystem.financialColors
import com.budgetmaster.core.util.DateUtils
import com.budgetmaster.core.util.MoneyFormatter
import com.budgetmaster.core.util.formatSigned
import com.budgetmaster.core.util.rememberHaptics
import com.budgetmaster.transactions.domain.model.RecurringTransaction
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Manage recurring schedules: create, edit, pause/resume, delete.
 *
 * The engine materializes entries on app open; this screen is where the schedules that drive
 * it are actually managed.
 */
@Composable
fun RecurringScreen(viewModel: RecurringViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RecurringEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onIntent(RecurringIntent.AddClicked) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.recurring_add))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.medium),
        ) {
            Spacer(Modifier.height(Spacing.medium))
            Text(
                text = stringResource(Res.string.recurring_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.medium))

            when {
                state.isLoading -> ShimmerListPlaceholder()
                state.isEmpty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Default.Autorenew,
                        title = stringResource(Res.string.recurring_empty_title),
                        subtitle = stringResource(Res.string.recurring_empty_subtitle),
                        actionLabel = stringResource(Res.string.recurring_add),
                        onAction = { viewModel.onIntent(RecurringIntent.AddClicked) },
                    )
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    items(state.schedules, key = { it.id }) { schedule ->
                        ScheduleRow(
                            schedule = schedule,
                            currencyCode = state.currencyCode,
                            onClick = { viewModel.onIntent(RecurringIntent.EditClicked(schedule)) },
                            onToggle = { active ->
                                viewModel.onIntent(RecurringIntent.SetActive(schedule.id, active))
                            },
                            onDelete = { viewModel.onIntent(RecurringIntent.Delete(schedule.id)) },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        if (state.editor.visible) {
            RecurringEditor(state, viewModel)
        }
    }
}

@Composable
private fun ScheduleRow(
    schedule: RecurringTransaction,
    currencyCode: String,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val haptics = rememberHaptics()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
                // Paused schedules stay legible but visibly inactive.
                .alpha(if (schedule.isActive) 1f else 0.55f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = categoryIconFor(schedule.categoryId),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(Spacing.compact))
            Column(Modifier.weight(1f)) {
                Text(
                    schedule.description,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (schedule.isActive) {
                        stringResource(
                            Res.string.recurring_next_run,
                            DateUtils.toLocalDate(schedule.nextRunDate).toString(),
                        )
                    } else {
                        stringResource(Res.string.recurring_paused)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = MoneyFormatter.formatSigned(schedule.amount, currencyCode),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (schedule.isExpense) {
                    MaterialTheme.financialColors.expense
                } else {
                    MaterialTheme.financialColors.income
                },
            )
            Switch(
                checked = schedule.isActive,
                onCheckedChange = {
                    haptics.toggle(it)
                    onToggle(it)
                },
                modifier = Modifier.padding(horizontal = Spacing.compact),
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.action_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** Bottom sheet on phones, centered dialog on wide layouts — matching the other editors. */
@Composable
private fun RecurringEditor(state: RecurringState, viewModel: RecurringViewModel) {
    val form: @Composable () -> Unit = {
        AddEditRecurringForm(
            editing = state.editor.editing,
            categories = state.categories,
            accounts = state.accounts,
            onSave = { viewModel.onIntent(RecurringIntent.Save(it)) },
            onCancel = { viewModel.onIntent(RecurringIntent.EditorDismissed) },
        )
    }

    BoxWithConstraints {
        if (maxWidth < 600.dp) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.onIntent(RecurringIntent.EditorDismissed) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) { form() }
        } else {
            Dialog(onDismissRequest = { viewModel.onIntent(RecurringIntent.EditorDismissed) }) {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.width(480.dp).verticalScroll(rememberScrollState())) { form() }
                }
            }
        }
    }
}
