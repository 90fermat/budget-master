@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class, ExperimentalLayoutApi::class)

package com.budgetmaster.accounts.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.accounts_transfer
import budgetmaster.core.generated.resources.accounts_transfer_amount
import budgetmaster.core.generated.resources.accounts_transfer_from
import budgetmaster.core.generated.resources.accounts_transfer_note
import budgetmaster.core.generated.resources.accounts_transfer_same_account
import budgetmaster.core.generated.resources.accounts_transfer_to
import budgetmaster.core.generated.resources.action_cancel
import budgetmaster.core.generated.resources.action_ok
import budgetmaster.core.generated.resources.transactions_date_label
import com.budgetmaster.accounts.domain.model.Account
import com.budgetmaster.core.util.DateUtils
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Moves money between two wallets. Requires two distinct accounts and a positive amount;
 * the resulting pair of entries is excluded from income/expense totals.
 */
@Composable
fun TransferForm(
    accounts: List<Account>,
    initialFromId: String?,
    onSubmit: (fromId: String, toId: String, amount: Double, timestamp: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var fromId by remember { mutableStateOf(initialFromId ?: accounts.firstOrNull()?.id) }
    var toId by remember { mutableStateOf(accounts.firstOrNull { it.id != fromId }?.id) }
    var amountText by remember { mutableStateOf("") }
    var timestamp by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val amount = amountText.replace(',', '.').toDoubleOrNull()
    val sameAccount = fromId != null && fromId == toId
    val canSubmit = fromId != null && toId != null && !sameAccount && amount != null && amount > 0.0

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = timestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { timestamp = it }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.accounts_transfer)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(Res.string.accounts_transfer_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(stringResource(Res.string.accounts_transfer_from), style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.forEach { account ->
                        FilterChip(
                            selected = fromId == account.id,
                            onClick = {
                                fromId = account.id
                                if (toId == account.id) toId = accounts.firstOrNull { it.id != account.id }?.id
                            },
                            label = { Text(account.name) },
                        )
                    }
                }

                Text(stringResource(Res.string.accounts_transfer_to), style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.filter { it.id != fromId }.forEach { account ->
                        FilterChip(
                            selected = toId == account.id,
                            onClick = { toId = account.id },
                            label = { Text(account.name) },
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text(stringResource(Res.string.accounts_transfer_amount)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(stringResource(Res.string.transactions_date_label), style = MaterialTheme.typography.labelMedium)
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(DateUtils.toLocalDate(timestamp).toString())
                }

                if (sameAccount) {
                    Text(
                        stringResource(Res.string.accounts_transfer_same_account),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSubmit,
                onClick = { onSubmit(fromId!!, toId!!, amount ?: 0.0, timestamp) },
            ) { Text(stringResource(Res.string.accounts_transfer)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}
