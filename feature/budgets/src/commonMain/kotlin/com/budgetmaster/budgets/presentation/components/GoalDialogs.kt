@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)

package com.budgetmaster.budgets.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.action_cancel
import budgetmaster.core.generated.resources.action_delete
import budgetmaster.core.generated.resources.action_ok
import budgetmaster.core.generated.resources.action_save
import budgetmaster.core.generated.resources.goals_add_funds
import budgetmaster.core.generated.resources.goals_amount_label
import budgetmaster.core.generated.resources.goals_contribute
import budgetmaster.core.generated.resources.goals_edit
import budgetmaster.core.generated.resources.goals_name_label
import budgetmaster.core.generated.resources.goals_new
import budgetmaster.core.generated.resources.goals_target_date_label
import budgetmaster.core.generated.resources.goals_target_label
import budgetmaster.core.generated.resources.goals_withdraw
import budgetmaster.core.generated.resources.goals_withdraw_title
import com.budgetmaster.budgets.domain.model.GoalItem
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.util.DateUtils
import com.budgetmaster.core.util.MoneyFormatter
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** One year in milliseconds — the default target horizon for a new goal. */
private const val DEFAULT_GOAL_HORIZON_MS = 365L * 24 * 60 * 60 * 1000

/** Create/edit goal form. */
@Composable
internal fun AddEditGoalForm(
    editing: GoalItem?,
    onSave: (name: String, target: Double, targetDate: Long) -> Unit,
    onDelete: (id: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var targetText by remember { mutableStateOf(editing?.targetAmount?.toString() ?: "") }
    var targetDate by remember {
        mutableStateOf(
            editing?.targetDate ?: (Clock.System.now().toEpochMilliseconds() + DEFAULT_GOAL_HORIZON_MS),
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val target = targetText.replace(',', '.').toDoubleOrNull()
    val canSave = name.isNotBlank() && target != null && target > 0.0

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = targetDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { targetDate = it }
                    showDatePicker = false
                }) { Text(stringResource(Res.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(if (editing == null) Res.string.goals_new else Res.string.goals_edit),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (editing != null) {
                IconButton(onClick = { onDelete(editing.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.action_delete), tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(Res.string.goals_name_label)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = targetText,
            onValueChange = { targetText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
            label = { Text(stringResource(Res.string.goals_target_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = stringResource(Res.string.goals_target_date_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { showDatePicker = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(DateUtils.toLocalDate(targetDate).toString())
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).height(52.dp)) {
                Text(stringResource(Res.string.action_cancel))
            }
            Button(onClick = { onSave(name, target ?: 0.0, targetDate) }, enabled = canSave, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).height(52.dp)) {
                Text(stringResource(Res.string.action_save), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(Spacing.small))
    }
}

/** Contribution amount form. */
@Composable
internal fun ContributeForm(
    goalName: String,
    onSubmit: (amount: Double) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var amountText by remember { mutableStateOf("") }
    val amount = amountText.replace(',', '.').toDoubleOrNull()
    val canSubmit = amount != null && amount > 0.0

    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Text(
            text = "${stringResource(Res.string.goals_contribute)} · $goalName",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
            label = { Text(stringResource(Res.string.goals_amount_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).height(52.dp)) {
                Text(stringResource(Res.string.action_cancel))
            }
            Button(onClick = { onSubmit(amount ?: 0.0) }, enabled = canSubmit, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).height(52.dp)) {
                Text(stringResource(Res.string.goals_add_funds), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(Spacing.small))
    }
}

/**
 * Withdraw-funds form. The amount is capped at the goal's saved balance so a withdrawal can
 * never take it negative (the repository clamps at zero as well).
 */
@Composable
internal fun WithdrawForm(
    goal: GoalItem,
    currencyCode: String,
    onSubmit: (amount: Double) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var amountText by remember { mutableStateOf("") }
    val amount = amountText.replace(',', '.').toDoubleOrNull()
    val canSubmit = amount != null && amount > 0.0 && amount <= goal.currentAmount

    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Text(
            text = "${stringResource(Res.string.goals_withdraw_title)} · ${goal.name}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = MoneyFormatter.format(goal.currentAmount, currencyCode),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
            label = { Text(stringResource(Res.string.goals_amount_label)) },
            singleLine = true,
            isError = amount != null && amount > goal.currentAmount,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).height(52.dp)) {
                Text(stringResource(Res.string.action_cancel))
            }
            Button(onClick = { onSubmit(amount ?: 0.0) }, enabled = canSubmit, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).height(52.dp)) {
                Text(stringResource(Res.string.goals_withdraw), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(Spacing.small))
    }
}
