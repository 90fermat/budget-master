package com.budgetmaster.accounts.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import budgetmaster.core.generated.resources.accounts_reconcile
import budgetmaster.core.generated.resources.accounts_reconcile_actual
import budgetmaster.core.generated.resources.accounts_reconcile_hint
import budgetmaster.core.generated.resources.accounts_reconcile_title
import budgetmaster.core.generated.resources.action_cancel
import com.budgetmaster.accounts.domain.model.Account
import com.budgetmaster.core.util.MoneyFormatter
import org.jetbrains.compose.resources.stringResource

/**
 * Reconciles a wallet against its real-world balance.
 *
 * The user enters what the account actually holds; the difference is posted as a single
 * adjustment entry rather than overwriting the balance, so the running total stays derived
 * from transactions and remains auditable.
 */
@Composable
fun ReconcileForm(
    account: Account,
    onSubmit: (actualBalance: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var balanceText by remember { mutableStateOf(account.currentBalance.toString()) }
    val actual = balanceText.replace(',', '.').toDoubleOrNull()
    val delta = actual?.let { it - account.currentBalance }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.accounts_reconcile_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${account.name} · ${MoneyFormatter.format(account.currentBalance, account.currency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it.filter { c -> c.isDigit() || c == '.' || c == ',' || c == '-' } },
                    label = { Text(stringResource(Res.string.accounts_reconcile_actual)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = if (delta != null && delta != 0.0) {
                        MoneyFormatter.format(delta, account.currency)
                    } else {
                        stringResource(Res.string.accounts_reconcile_hint)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = actual != null && delta != 0.0,
                onClick = { onSubmit(actual ?: account.currentBalance) },
            ) { Text(stringResource(Res.string.accounts_reconcile)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}
