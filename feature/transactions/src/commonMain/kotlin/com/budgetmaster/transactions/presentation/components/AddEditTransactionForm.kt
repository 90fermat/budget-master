@file:OptIn(ExperimentalTime::class, ExperimentalLayoutApi::class)

package com.budgetmaster.transactions.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import budgetmaster.core.generated.resources.transactions_add_title
import budgetmaster.core.generated.resources.transactions_amount_label
import budgetmaster.core.generated.resources.transactions_cancel
import budgetmaster.core.generated.resources.transactions_category_label
import budgetmaster.core.generated.resources.transactions_description_label
import budgetmaster.core.generated.resources.transactions_description_placeholder
import budgetmaster.core.generated.resources.transactions_edit_title
import budgetmaster.core.generated.resources.transactions_notes_label
import budgetmaster.core.generated.resources.transactions_save
import budgetmaster.core.generated.resources.transactions_type_expense
import budgetmaster.core.generated.resources.transactions_type_income
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.transactions.domain.model.TransactionCategory
import com.budgetmaster.transactions.domain.model.TransactionDraft
import com.budgetmaster.transactions.domain.model.TransactionItem
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The create/edit transaction form used inside a bottom sheet (phone) or dialog (wide).
 *
 * Manages its own local field state and emits a [TransactionDraft] via [onSave]. The
 * Save button is disabled until an amount and description are present.
 */
@Composable
internal fun AddEditTransactionForm(
    editing: TransactionItem?,
    categories: List<TransactionCategory>,
    onSave: (TransactionDraft) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpense by remember { mutableStateOf(editing?.isExpense ?: true) }
    var amountText by remember {
        mutableStateOf(editing?.let { kotlin.math.abs(it.amount).toString() } ?: "")
    }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    var notes by remember { mutableStateOf(editing?.notes ?: "") }
    var categoryId by remember { mutableStateOf(editing?.category?.id) }

    val amount = amountText.replace(',', '.').toDoubleOrNull()
    val canSave = amount != null && amount > 0.0 && description.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Text(
            text = stringResource(if (editing == null) Res.string.transactions_add_title else Res.string.transactions_edit_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // Expense / Income toggle
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
            placeholder = { Text(stringResource(Res.string.transactions_description_placeholder)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = stringResource(Res.string.transactions_category_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            categories.forEach { category ->
                FilterChip(
                    selected = categoryId == category.id,
                    onClick = { categoryId = if (categoryId == category.id) null else category.id },
                    label = { Text("${category.icon} ${category.name}") },
                )
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(Res.string.transactions_notes_label)) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Text(stringResource(Res.string.transactions_cancel))
            }
            Button(
                onClick = {
                    onSave(
                        TransactionDraft(
                            id = editing?.id,
                            amountAbs = amount ?: 0.0,
                            isExpense = isExpense,
                            description = description,
                            categoryId = categoryId,
                            timestamp = editing?.timestamp ?: Clock.System.now().toEpochMilliseconds(),
                            notes = notes.ifBlank { null },
                        )
                    )
                },
                enabled = canSave,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Text(stringResource(Res.string.transactions_save), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(Spacing.small))
    }
}
