package com.budgetmaster.accounts.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.accounts_add
import budgetmaster.core.generated.resources.accounts_currency_label
import budgetmaster.core.generated.resources.accounts_edit
import budgetmaster.core.generated.resources.accounts_name_label
import budgetmaster.core.generated.resources.accounts_opening_balance_label
import budgetmaster.core.generated.resources.accounts_type_label
import budgetmaster.core.generated.resources.action_cancel
import budgetmaster.core.generated.resources.action_save
import com.budgetmaster.accounts.domain.model.Account
import com.budgetmaster.accounts.domain.model.AccountDraft
import com.budgetmaster.accounts.domain.model.AccountType
import org.jetbrains.compose.resources.stringResource

private val CURRENCIES = listOf("USD", "EUR", "GBP", "XAF", "CAD", "NGN")

/** Create/edit dialog for a wallet. [existing] non-null means editing. */
@Composable
fun AddEditAccountForm(
    existing: Account?,
    onSubmit: (AccountDraft) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: AccountType.CASH) }
    var balanceText by remember {
        mutableStateOf(existing?.openingBalance?.let { if (it == 0.0) "" else it.toString() } ?: "")
    }
    var currency by remember { mutableStateOf(existing?.currency ?: "USD") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (existing == null) Res.string.accounts_add else Res.string.accounts_edit))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.accounts_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(stringResource(Res.string.accounts_type_label), style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                ) {
                    AccountType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(option.label()) },
                        )
                    }
                }

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                    label = { Text(stringResource(Res.string.accounts_opening_balance_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(stringResource(Res.string.accounts_currency_label), style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                ) {
                    CURRENCIES.forEach { code ->
                        FilterChip(
                            selected = currency == code,
                            onClick = { currency = code },
                            label = { Text(code) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSubmit(
                        AccountDraft(
                            id = existing?.id,
                            name = name.trim(),
                            type = type,
                            openingBalance = balanceText.toDoubleOrNull() ?: 0.0,
                            currency = currency,
                        ),
                    )
                },
            ) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}
