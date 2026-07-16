@file:OptIn(ExperimentalLayoutApi::class)

package com.budgetmaster.budgets.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import budgetmaster.core.generated.resources.action_save
import budgetmaster.core.generated.resources.budgets_category_label
import budgetmaster.core.generated.resources.budgets_edit
import budgetmaster.core.generated.resources.budgets_limit_label
import budgetmaster.core.generated.resources.budgets_new
import com.budgetmaster.budgets.domain.model.BudgetCategory
import com.budgetmaster.budgets.domain.model.BudgetItem
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.designsystem.categoryIconFor
import org.jetbrains.compose.resources.stringResource

/** Create/edit budget form used inside a bottom sheet (phone) or dialog (wide). */
@Composable
internal fun AddEditBudgetForm(
    editing: BudgetItem?,
    categories: List<BudgetCategory>,
    onSave: (categoryId: String, limit: Double) -> Unit,
    onDelete: (id: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var categoryId by remember { mutableStateOf(editing?.category?.id ?: categories.firstOrNull()?.id) }
    var limitText by remember { mutableStateOf(editing?.limit?.toString() ?: "") }

    val limit = limitText.replace(',', '.').toDoubleOrNull()
    val canSave = categoryId != null && limit != null && limit > 0.0

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
                text = stringResource(if (editing == null) Res.string.budgets_new else Res.string.budgets_edit),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (editing != null) {
                IconButton(onClick = { onDelete(editing.id) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(Res.string.action_delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Text(
            text = stringResource(Res.string.budgets_category_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            categories.forEach { category ->
                FilterChip(
                    selected = categoryId == category.id,
                    onClick = { categoryId = category.id },
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

        OutlinedTextField(
            value = limitText,
            onValueChange = { limitText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
            label = { Text(stringResource(Res.string.budgets_limit_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
            ) { Text(stringResource(Res.string.action_cancel)) }
            Button(
                onClick = { onSave(categoryId!!, limit ?: 0.0) },
                enabled = canSave,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(52.dp),
            ) { Text(stringResource(Res.string.action_save), fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(Spacing.small))
    }
}
