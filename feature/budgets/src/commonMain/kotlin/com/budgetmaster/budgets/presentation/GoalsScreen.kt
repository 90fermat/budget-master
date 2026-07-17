@file:OptIn(ExperimentalMaterial3Api::class)

package com.budgetmaster.budgets.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.goals_empty_subtitle
import budgetmaster.core.generated.resources.empty_goals_cta
import budgetmaster.core.generated.resources.goals_empty_title
import budgetmaster.core.generated.resources.goals_new
import budgetmaster.core.generated.resources.goals_title
import com.budgetmaster.budgets.presentation.components.AddEditGoalForm
import com.budgetmaster.budgets.presentation.components.ContributeForm
import com.budgetmaster.budgets.presentation.components.GoalCard
import com.budgetmaster.budgets.presentation.components.WithdrawForm
import com.budgetmaster.core.designsystem.components.GuidanceHost
import com.budgetmaster.core.designsystem.components.HelpIconButton
import com.budgetmaster.core.designsystem.components.rememberGuidance
import com.budgetmaster.core.guidance.GuidanceKey
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.designsystem.components.EmptyState as SharedEmptyState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Savings Goals screen on live data: progress cards, create/edit editor, delete, and
 * an "Add funds" contribution dialog.
 */
@Composable
fun GoalsScreen(viewModel: GoalsViewModel = koinViewModel()) {
    val guidance = rememberGuidance(GuidanceKey.GOALS)
    GuidanceHost(guidance)

    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is GoalsEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onIntent(GoalsIntent.AddClicked) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.goals_new)) }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.medium),
        ) {
            Spacer(Modifier.height(Spacing.medium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(Res.string.goals_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                HelpIconButton(onClick = guidance::show)
            }
            Spacer(Modifier.height(Spacing.medium))

            when {
                state.isEmpty -> EmptyState(onAdd = { viewModel.onIntent(GoalsIntent.AddClicked) })
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    items(state.goals, key = { it.id }) { item ->
                        GoalCard(
                            item = item,
                            currencyCode = state.currencyCode,
                            onClick = { viewModel.onIntent(GoalsIntent.EditClicked(item)) },
                            onContribute = { viewModel.onIntent(GoalsIntent.ContributeClicked(item)) },
                            onWithdraw = { viewModel.onIntent(GoalsIntent.WithdrawClicked(item)) },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        if (state.editor.visible) GoalEditor(state, viewModel)
        if (state.contribute.visible) ContributeDialog(state, viewModel)
        if (state.withdraw.visible) WithdrawDialog(state, viewModel)
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        SharedEmptyState(
            icon = Icons.Default.Flag,
            title = stringResource(Res.string.goals_empty_title),
            subtitle = stringResource(Res.string.goals_empty_subtitle),
            actionLabel = stringResource(Res.string.empty_goals_cta),
            onAction = onAdd,
        )
    }
}

@Composable
private fun GoalEditor(state: GoalsState, viewModel: GoalsViewModel) {
    AdaptiveContainer(onDismiss = { viewModel.onIntent(GoalsIntent.EditorDismissed) }) {
        AddEditGoalForm(
            editing = state.editor.editing,
            onSave = { name, target, targetDate ->
                viewModel.onIntent(GoalsIntent.SaveGoal(name, target, targetDate, state.editor.editing?.id))
            },
            onDelete = { id -> viewModel.onIntent(GoalsIntent.DeleteRequested(id)) },
            onCancel = { viewModel.onIntent(GoalsIntent.EditorDismissed) },
        )
    }
}

@Composable
private fun ContributeDialog(state: GoalsState, viewModel: GoalsViewModel) {
    val goal = state.contribute.goal ?: return
    AdaptiveContainer(onDismiss = { viewModel.onIntent(GoalsIntent.ContributeDismissed) }) {
        ContributeForm(
            goalName = goal.name,
            onSubmit = { amount -> viewModel.onIntent(GoalsIntent.SubmitContribution(goal.id, amount)) },
            onCancel = { viewModel.onIntent(GoalsIntent.ContributeDismissed) },
        )
    }
}

@Composable
private fun WithdrawDialog(state: GoalsState, viewModel: GoalsViewModel) {
    val goal = state.withdraw.goal ?: return
    AdaptiveContainer(onDismiss = { viewModel.onIntent(GoalsIntent.WithdrawDismissed) }) {
        WithdrawForm(
            goal = goal,
            currencyCode = state.currencyCode,
            onSubmit = { amount -> viewModel.onIntent(GoalsIntent.SubmitWithdrawal(goal.id, amount)) },
            onCancel = { viewModel.onIntent(GoalsIntent.WithdrawDismissed) },
        )
    }
}

/** Bottom sheet on phones, centered dialog on wide layouts. */
@Composable
private fun AdaptiveContainer(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    BoxWithConstraints {
        if (maxWidth < 600.dp) {
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
            ) { content() }
        } else {
            Dialog(onDismissRequest = onDismiss) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.width(480.dp),
                ) { content() }
            }
        }
    }
}
