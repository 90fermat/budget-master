@file:OptIn(ExperimentalTime::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.budgetmaster.transactions.presentation.recurring

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.action_cancel
import budgetmaster.core.generated.resources.action_ok
import budgetmaster.core.generated.resources.action_save
import budgetmaster.core.generated.resources.recurring_add
import budgetmaster.core.generated.resources.recurring_daily
import budgetmaster.core.generated.resources.recurring_monthly
import budgetmaster.core.generated.resources.recurring_weekly
import budgetmaster.core.generated.resources.recurring_yearly
import budgetmaster.core.generated.resources.recurring_edit
import budgetmaster.core.generated.resources.recurring_frequency_label
import budgetmaster.core.generated.resources.recurring_start_label
import budgetmaster.core.generated.resources.transactions_account_label
import budgetmaster.core.generated.resources.transactions_amount_label
import budgetmaster.core.generated.resources.transactions_category_label
import budgetmaster.core.generated.resources.transactions_description_label
import budgetmaster.core.generated.resources.transactions_type_expense
import budgetmaster.core.generated.resources.transactions_type_income
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.designsystem.pressScale
import com.budgetmaster.core.util.DateUtils
import com.budgetmaster.core.util.rememberHaptics
import com.budgetmaster.core.designsystem.categoryNameFor
import com.budgetmaster.transactions.domain.model.Frequency
import com.budgetmaster.transactions.domain.model.RecurringDraft
import com.budgetmaster.transactions.domain.model.RecurringTransaction
import com.budgetmaster.transactions.domain.model.TransactionAccount
import com.budgetmaster.transactions.domain.model.TransactionCategory
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Create/edit form for a recurring schedule. */
@Composable
internal fun AddEditRecurringForm(
    editing: RecurringTransaction?,
    categories: List<TransactionCategory>,
    accounts: List<TransactionAccount>,
    onSave: (RecurringDraft) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpense by remember { mutableStateOf(editing?.isExpense ?: true) }
    var amountText by remember { mutableStateOf(editing?.let { abs(it.amount).toString() } ?: "") }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    var categoryId by remember { mutableStateOf(editing?.categoryId) }
    var accountId by remember(accounts) { mutableStateOf(editing?.accountId ?: accounts.firstOrNull()?.id) }
    var frequency by remember { mutableStateOf(editing?.frequency ?: Frequency.MONTHLY) }
    var startDate by remember {
        mutableStateOf(editing?.startDate ?: Clock.System.now().toEpochMilliseconds())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()
    val saveInteraction = remember { MutableInteractionSource() }

    val amount = amountText.replace(',', '.').toDoubleOrNull()
    val canSave = amount != null && amount > 0.0 && description.isNotBlank()

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { startDate = it }
                    showDatePicker = false
                }) { Text(stringResource(Res.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        ) { DatePicker(state = pickerState) }
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Text(
            text = stringResource(if (editing == null) Res.string.recurring_add else Res.string.recurring_edit),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            FilterChip(
                selected = isExpense,
                onClick = { isExpense = true },
                label = { Text(stringResource(Res.string.transactions_type_expense)) },
            )
            FilterChip(
                selected = !isExpense,
                onClick = { isExpense = false },
                label = { Text(stringResource(Res.string.transactions_type_income)) },
            )
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
            label = { Text(stringResource(Res.string.transactions_amount_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(Res.string.transactions_description_label)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(stringResource(Res.string.recurring_frequency_label), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            Frequency.entries.forEach { option ->
                FilterChip(
                    selected = frequency == option,
                    onClick = { frequency = option },
                    label = { Text(option.label()) },
                )
            }
        }

        Text(stringResource(Res.string.transactions_category_label), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            categories.forEach { category ->
                FilterChip(
                    selected = categoryId == category.id,
                    onClick = { categoryId = if (categoryId == category.id) null else category.id },
                    label = { Text(categoryNameFor(category.id, category.name)) },
                )
            }
        }

        if (accounts.size > 1) {
            Text(stringResource(Res.string.transactions_account_label), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                accounts.forEach { account ->
                    FilterChip(
                        selected = accountId == account.id,
                        onClick = { accountId = account.id },
                        label = { Text(account.name) },
                    )
                }
            }
        }

        Text(stringResource(Res.string.recurring_start_label), style = MaterialTheme.typography.titleSmall)
        OutlinedButton(
            onClick = { showDatePicker = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Text(DateUtils.toLocalDate(startDate).toString())
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            ) { Text(stringResource(Res.string.action_cancel)) }

            Button(
                onClick = {
                    haptics.confirm()
                    onSave(
                        RecurringDraft(
                            id = editing?.id,
                            accountId = accountId,
                            categoryId = categoryId,
                            amountAbs = amount ?: 0.0,
                            isExpense = isExpense,
                            description = description,
                            frequency = frequency,
                            startDate = startDate,
                        ),
                    )
                },
                enabled = canSave,
                interactionSource = saveInteraction,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).heightIn(min = 52.dp).pressScale(saveInteraction),
            ) { Text(stringResource(Res.string.action_save), fontWeight = FontWeight.Bold) }
        }
    }
}

/** Localized display name for a [Frequency]. */
@Composable
internal fun Frequency.label(): String = stringResource(
    when (this) {
        Frequency.DAILY -> Res.string.recurring_daily
        Frequency.WEEKLY -> Res.string.recurring_weekly
        Frequency.MONTHLY -> Res.string.recurring_monthly
        Frequency.YEARLY -> Res.string.recurring_yearly
    },
)
