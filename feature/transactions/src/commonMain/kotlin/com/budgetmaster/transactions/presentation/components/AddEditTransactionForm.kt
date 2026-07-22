@file:OptIn(ExperimentalTime::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.budgetmaster.transactions.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.action_cancel
import budgetmaster.core.generated.resources.action_ok
import budgetmaster.core.generated.resources.transactions_quick_add_action
import budgetmaster.core.generated.resources.transactions_quick_add_failed
import budgetmaster.core.generated.resources.transactions_quick_add_label
import budgetmaster.core.generated.resources.transactions_quick_add_no_amount
import budgetmaster.core.generated.resources.transactions_quick_add_placeholder
import budgetmaster.core.generated.resources.transactions_category_suggested
import budgetmaster.core.generated.resources.transactions_account_label
import budgetmaster.core.generated.resources.transactions_add_title
import budgetmaster.core.generated.resources.transactions_amount_label
import budgetmaster.core.generated.resources.transactions_cancel
import budgetmaster.core.generated.resources.transactions_category_label
import budgetmaster.core.generated.resources.transactions_date_label
import budgetmaster.core.generated.resources.transactions_description_label
import budgetmaster.core.generated.resources.transactions_description_placeholder
import budgetmaster.core.generated.resources.transactions_edit_title
import budgetmaster.core.generated.resources.transactions_notes_label
import budgetmaster.core.generated.resources.transactions_recurring_label
import budgetmaster.core.generated.resources.transactions_save
import budgetmaster.core.generated.resources.transactions_type_expense
import budgetmaster.core.generated.resources.transactions_type_income
import budgetmaster.core.generated.resources.transactions_receipt_scan
import budgetmaster.core.generated.resources.transactions_receipt_reading
import budgetmaster.core.generated.resources.transactions_receipt_no_text
import budgetmaster.core.generated.resources.transactions_receipt_no_amount
import budgetmaster.core.generated.resources.transactions_receipt_failed
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.designsystem.categoryIconFor
import com.budgetmaster.core.designsystem.pressScale
import com.budgetmaster.core.util.DateUtils
import com.budgetmaster.core.util.rememberHaptics
import com.budgetmaster.core.navigation.TransactionKind
import com.budgetmaster.core.ocr.ReceiptImage
import com.budgetmaster.core.ocr.rememberReceiptPicker
import com.budgetmaster.core.designsystem.categoryNameFor
import com.budgetmaster.transactions.domain.model.TransactionAccount
import com.budgetmaster.transactions.domain.model.TransactionCategory
import com.budgetmaster.transactions.domain.model.TransactionDraft
import com.budgetmaster.transactions.domain.model.TransactionItem
import com.budgetmaster.transactions.domain.usecase.QuickEntryDraft
import com.budgetmaster.transactions.domain.usecase.QuickEntryError
import com.budgetmaster.transactions.domain.usecase.QuickEntryException
import com.budgetmaster.transactions.domain.usecase.ReceiptScanError
import com.budgetmaster.transactions.domain.usecase.ReceiptScanException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    accounts: List<TransactionAccount>,
    activeAccountId: String?,
    onSave: (TransactionDraft) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    // AI quick-add: only shown when a provider exists and the user opted in. Parses a note like
    // "coffee 4.50 yesterday" and fills the fields below; the user still reviews and saves.
    quickAddEnabled: Boolean = false,
    onQuickParse: suspend (String) -> Result<QuickEntryDraft> = { Result.failure(NotImplementedError()) },
    // Suggests a category from a typed description (cached per merchant). Returns null when it
    // can't tell or AI is off — the suggestion chip simply doesn't appear.
    onSuggestCategory: suspend (String) -> String? = { null },
    // Receipt scan: OCR runs on-device, then the extracted text is parsed into the same fields.
    receiptScanEnabled: Boolean = false,
    onScanReceipt: suspend (ReceiptImage) -> Result<QuickEntryDraft> = { Result.failure(NotImplementedError()) },
    // Set when the user arrived from a Dashboard quick action, which already named the kind.
    // Ignored while editing, where the existing entry decides.
    initialKind: TransactionKind? = null,
) {
    var isExpense by remember {
        mutableStateOf(editing?.isExpense ?: (initialKind != TransactionKind.INCOME))
    }
    var amountText by remember {
        mutableStateOf(editing?.let { kotlin.math.abs(it.amount).toString() } ?: "")
    }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    var notes by remember { mutableStateOf(editing?.notes ?: "") }
    var categoryId by remember { mutableStateOf(editing?.category?.id) }
    var timestamp by remember {
        mutableStateOf(editing?.timestamp ?: Clock.System.now().toEpochMilliseconds())
    }
    var isRecurring by remember { mutableStateOf(editing?.isRecurring ?: false) }
    // New entries default to the wallet the app is scoped to; "All accounts" falls back to
    // the first wallet so the picker always shows the account that will actually be used.
    var accountId by remember(accounts, activeAccountId) {
        mutableStateOf(editing?.accountId?.ifBlank { null } ?: activeAccountId ?: accounts.firstOrNull()?.id)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()

    val amount = amountText.replace(',', '.').toDoubleOrNull()
    val canSave = amount != null && amount > 0.0 && description.isNotBlank()

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
        ) {
            DatePicker(state = pickerState)
        }
    }

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

        // Quick-add is for new entries only: prefilling the fields of an edit would fight the
        // values the user is already correcting.
        if (quickAddEnabled && editing == null) {
            // Both AI capture paths land in the same place: they fill these fields for review.
            val fillFromDraft: (QuickEntryDraft) -> Unit = { draft ->
                isExpense = draft.isExpense
                amountText = draft.amountAbs.toString()
                if (draft.description.isNotBlank()) description = draft.description
                draft.categoryId?.let { categoryId = it }
                timestamp = draft.timestamp
            }

            QuickAddField(onParse = onQuickParse, onParsed = fillFromDraft)

            if (receiptScanEnabled) {
                ScanReceiptButton(onScan = onScanReceipt, onParsed = fillFromDraft)
            }
        }

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

        // AI category suggestion: only when the user has typed a description and not yet picked a
        // category. Debounced so it doesn't fire on every keystroke, and it clears the moment the
        // user chooses anything themselves.
        var suggestedCategoryId by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(description, categoryId, quickAddEnabled) {
            suggestedCategoryId = null
            if (!quickAddEnabled || categoryId != null || description.isBlank()) return@LaunchedEffect
            delay(600)
            suggestedCategoryId = onSuggestCategory(description)?.takeIf { it != categoryId }
        }
        suggestedCategoryId?.let { suggestion ->
            val category = categories.firstOrNull { it.id == suggestion }
            if (category != null) {
                AssistChip(
                    onClick = { categoryId = suggestion; suggestedCategoryId = null },
                    label = {
                        Text(
                            stringResource(
                                Res.string.transactions_category_suggested,
                                categoryNameFor(category.id, category.name),
                            ),
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                )
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            categories.forEach { category ->
                FilterChip(
                    selected = categoryId == category.id,
                    onClick = { categoryId = if (categoryId == category.id) null else category.id },
                    label = { Text(categoryNameFor(category.id, category.name)) },
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

        if (accounts.size > 1) {
            Text(
                text = stringResource(Res.string.transactions_account_label),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

        Text(
            text = stringResource(Res.string.transactions_date_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { showDatePicker = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Text(DateUtils.toLocalDate(timestamp).toString())
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.transactions_recurring_label),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
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
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            ) {
                Text(stringResource(Res.string.transactions_cancel))
            }
            val saveInteraction = remember { MutableInteractionSource() }
            Button(
                interactionSource = saveInteraction,
                onClick = {
                    haptics.confirm()
                    onSave(
                        TransactionDraft(
                            id = editing?.id,
                            amountAbs = amount ?: 0.0,
                            isExpense = isExpense,
                            description = description,
                            categoryId = categoryId,
                            timestamp = timestamp,
                            notes = notes.ifBlank { null },
                            accountId = accountId,
                            isRecurring = isRecurring,
                        )
                    )
                },
                enabled = canSave,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).heightIn(min = 52.dp).pressScale(saveInteraction),
            ) {
                Text(stringResource(Res.string.transactions_save), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(Spacing.small))
    }
}

/**
 * "Scan receipt": pick a receipt photo, OCR it on-device, and fill the fields for review.
 *
 * The image is read by ML Kit locally and never uploaded — only the recognised text is summarised
 * to the model. Like quick-add, this drafts an entry; the user still confirms and saves.
 */
@Composable
private fun ScanReceiptButton(
    onScan: suspend (ReceiptImage) -> Result<QuickEntryDraft>,
    onParsed: (QuickEntryDraft) -> Unit,
) {
    var scanning by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Resolved here because a coroutine callback can't call stringResource.
    val noTextMsg = stringResource(Res.string.transactions_receipt_no_text)
    val noAmountMsg = stringResource(Res.string.transactions_receipt_no_amount)
    val failedMsg = stringResource(Res.string.transactions_receipt_failed)

    val picker = rememberReceiptPicker { image ->
        scanning = true
        error = null
        scope.launch {
            onScan(image)
                .onSuccess(onParsed)
                .onFailure {
                    error = when ((it as? ReceiptScanException)?.error) {
                        ReceiptScanError.NoTextFound -> noTextMsg
                        ReceiptScanError.NoAmount -> noAmountMsg
                        else -> failedMsg
                    }
                }
            scanning = false
        }
    }

    if (!picker.isSupported) return

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.micro)) {
        OutlinedButton(
            onClick = { picker.launch() },
            enabled = !scanning,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            if (scanning) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.transactions_receipt_reading))
            } else {
                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.transactions_receipt_scan))
            }
        }
        error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

/**
 * The natural-language quick-add field: type "coffee 4.50 yesterday", tap parse, and the fields
 * below fill in for review. It fills the form rather than saving — AI drafts an entry, it never
 * records money on its own.
 */
@Composable
private fun QuickAddField(
    onParse: suspend (String) -> Result<QuickEntryDraft>,
    onParsed: (QuickEntryDraft) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var parsing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Resolved here because a coroutine callback can't call stringResource.
    val noAmountMsg = stringResource(Res.string.transactions_quick_add_no_amount)
    val failedMsg = stringResource(Res.string.transactions_quick_add_failed)

    val submit: () -> Unit = submit@{
        if (parsing || text.isBlank()) return@submit
        parsing = true
        error = null
        scope.launch {
            onParse(text)
                .onSuccess { draft ->
                    onParsed(draft)
                    text = ""
                }
                .onFailure {
                    error = if ((it as? QuickEntryException)?.error == QuickEntryError.NoAmount) {
                        noAmountMsg
                    } else {
                        failedMsg
                    }
                }
            parsing = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.micro)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; error = null },
            label = { Text(stringResource(Res.string.transactions_quick_add_label)) },
            placeholder = { Text(stringResource(Res.string.transactions_quick_add_placeholder)) },
            singleLine = true,
            enabled = !parsing,
            isError = error != null,
            shape = RoundedCornerShape(14.dp),
            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
            trailingIcon = {
                if (parsing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = submit, enabled = text.isNotBlank()) {
                        Text(stringResource(Res.string.transactions_quick_add_action))
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

