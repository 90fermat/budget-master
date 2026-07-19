@file:OptIn(ExperimentalMaterial3Api::class)

package com.budgetmaster.transactions.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import budgetmaster.core.generated.resources.empty_transactions_cta
import budgetmaster.core.generated.resources.recurring_manage
import budgetmaster.core.generated.resources.transactions_deleted
import budgetmaster.core.generated.resources.transactions_empty_filtered
import budgetmaster.core.generated.resources.transactions_empty_subtitle
import budgetmaster.core.generated.resources.transactions_empty_title
import budgetmaster.core.generated.resources.transactions_filter_all
import budgetmaster.core.generated.resources.transactions_filter_expense
import budgetmaster.core.generated.resources.transactions_filter_income
import budgetmaster.core.generated.resources.transactions_search_placeholder
import budgetmaster.core.generated.resources.transactions_title
import budgetmaster.core.generated.resources.transactions_today
import budgetmaster.core.generated.resources.transactions_uncategorized
import budgetmaster.core.generated.resources.transactions_recurring_detected
import budgetmaster.core.generated.resources.transactions_undo
import budgetmaster.core.generated.resources.transactions_yesterday
import budgetmaster.core.generated.resources.transactions_paste_label
import budgetmaster.core.generated.resources.transactions_paste_placeholder
import budgetmaster.core.generated.resources.transactions_paste_action
import budgetmaster.core.generated.resources.transactions_paste_imported
import budgetmaster.core.generated.resources.transactions_paste_duplicate
import budgetmaster.core.generated.resources.transactions_paste_needs_review
import budgetmaster.core.generated.resources.transactions_paste_unreadable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.TextButton
import kotlinx.coroutines.launch
import com.budgetmaster.transactions.domain.usecase.ImportOutcome
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.designsystem.SurfaceLevel
import com.budgetmaster.core.designsystem.components.AppCard
import com.budgetmaster.core.designsystem.components.EmptyState as SharedEmptyState
import com.budgetmaster.core.designsystem.components.GuidanceHost
import com.budgetmaster.core.designsystem.components.HelpIconButton
import com.budgetmaster.core.designsystem.components.ShimmerListPlaceholder
import com.budgetmaster.core.designsystem.components.rememberGuidance
import com.budgetmaster.core.guidance.GuidanceKey
import com.budgetmaster.core.navigation.TransactionKind
import com.budgetmaster.core.designsystem.categoryIconFor
import com.budgetmaster.core.util.MoneyFormatter
import com.budgetmaster.transactions.domain.usecase.RecurringCharge
import com.budgetmaster.core.util.RelativeDay
import com.budgetmaster.core.util.formatSigned
import com.budgetmaster.core.designsystem.categoryNameFor
import com.budgetmaster.transactions.domain.model.TypeFilter
import com.budgetmaster.transactions.presentation.components.AddEditTransactionForm
import com.budgetmaster.transactions.presentation.components.PendingImportsCard
import com.budgetmaster.transactions.presentation.components.TransactionRowItem
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Transactions screen: searchable, filterable, day-grouped list backed by live SQLDelight
 * data, with swipe-to-delete + undo.
 *
 * Adaptive: a **list-detail split** from 600dp, where the editor docks beside the list; a
 * bottom sheet below that.
 */
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel = koinViewModel(),
    onManageRecurring: () -> Unit = {},
    openEditorFor: TransactionKind? = null,
) {
    val state by viewModel.state.collectAsState()

    // Arriving from a Dashboard quick action: open the editor straight away, pre-set to the kind
    // the button named. Keyed on the argument so returning to this screen normally does not
    // re-open it, and a configuration change does not open it twice.
    LaunchedEffect(openEditorFor) {
        if (openEditorFor != null) {
            viewModel.onIntent(TransactionsIntent.AddClicked(openEditorFor))
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val guidance = rememberGuidance(GuidanceKey.TRANSACTIONS)

    val undoLabel = stringResource(Res.string.transactions_undo)
    LaunchedEffectEffects(viewModel, snackbarHostState, undoLabel)

    GuidanceHost(guidance)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onIntent(TransactionsIntent.AddClicked()) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.transactions_title))
            }
        },
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            // List-detail from ≥600dp: the editor becomes a docked pane beside the list rather
            // than a sheet over it, so the list stays readable while you type. Uses the app's
            // existing BoxWithConstraints breakpoint — the same idiom as the shell and every
            // other editor — rather than material3-adaptive's pane scaffold, which is declared
            // in the version catalog but wired into no module and still beta.
            val isWide = maxWidth >= 600.dp
            val showDetailPane = isWide && state.editor.visible

            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = Spacing.medium),
                ) {
                    Spacer(Modifier.height(Spacing.medium))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(Res.string.transactions_title),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onManageRecurring) {
                                Icon(
                                    Icons.Default.Autorenew,
                                    contentDescription = stringResource(Res.string.recurring_manage),
                                )
                            }
                            HelpIconButton(onClick = guidance::show)
                        }
                    }
                    Spacer(Modifier.height(Spacing.medium))

                    OutlinedTextField(
                        value = state.query,
                        onValueChange = { viewModel.onIntent(TransactionsIntent.SearchChanged(it)) },
                        placeholder = { Text(stringResource(Res.string.transactions_search_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(Spacing.medium))
                    TypeAndCategoryFilters(state, viewModel)
                    Spacer(Modifier.height(Spacing.medium))

                    when {
                        // Placeholder rows shaped like the real list, so nothing jumps on arrival.
                        state.isLoading -> ShimmerListPlaceholder()
                        state.isEmpty -> EmptyState(
                            filtered = !state.query.isBlank() || state.categoryFilterId != null ||
                                state.typeFilter != TypeFilter.ALL,
                            onAdd = { viewModel.onIntent(TransactionsIntent.AddClicked()) },
                        )
                        else -> TransactionList(state, viewModel)
                    }
                }

                if (showDetailPane) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.width(420.dp).fillMaxHeight(),
                    ) {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            AddEditTransactionForm(
                                editing = state.editor.editing,
                                categories = state.categories,
                                accounts = state.accounts,
                                activeAccountId = state.activeAccountId,
                                onSave = { viewModel.onIntent(TransactionsIntent.SaveTransaction(it)) },
                                onCancel = { viewModel.onIntent(TransactionsIntent.EditorDismissed) },
                                quickAddEnabled = state.quickAddEnabled,
                                onQuickParse = viewModel::parseQuickEntry,
                                onSuggestCategory = viewModel::suggestCategory,
                                receiptScanEnabled = state.receiptScanEnabled,
                                onScanReceipt = viewModel::scanReceipt,
                                initialKind = state.editor.initialKind,
                            )
                        }
                    }
                }
            }

            // Narrow layouts keep the bottom sheet.
            if (state.editor.visible && !isWide) {
                TransactionEditor(state, viewModel)
            }
        }
    }
}

@Composable
private fun TypeAndCategoryFilters(state: TransactionsState, viewModel: TransactionsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            FilterChip(
                selected = state.typeFilter == TypeFilter.ALL,
                onClick = { viewModel.onIntent(TransactionsIntent.TypeFilterChanged(TypeFilter.ALL)) },
                label = { Text(stringResource(Res.string.transactions_filter_all)) },
            )
            FilterChip(
                selected = state.typeFilter == TypeFilter.INCOME,
                onClick = { viewModel.onIntent(TransactionsIntent.TypeFilterChanged(TypeFilter.INCOME)) },
                label = { Text(stringResource(Res.string.transactions_filter_income)) },
            )
            FilterChip(
                selected = state.typeFilter == TypeFilter.EXPENSE,
                onClick = { viewModel.onIntent(TransactionsIntent.TypeFilterChanged(TypeFilter.EXPENSE)) },
                label = { Text(stringResource(Res.string.transactions_filter_expense)) },
            )
        }
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            items(state.categories) { category ->
                FilterChip(
                    selected = state.categoryFilterId == category.id,
                    onClick = {
                        val next = if (state.categoryFilterId == category.id) null else category.id
                        viewModel.onIntent(TransactionsIntent.CategoryFilterChanged(next))
                    },
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
    }
}

@Composable
private fun TransactionList(state: TransactionsState, viewModel: TransactionsViewModel) {
    val uncategorized = stringResource(Res.string.transactions_uncategorized)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        item(key = "paste_message") {
            PasteMessageCard(onImport = viewModel::importPastedMessage)
        }
        if (state.pendingImports.isNotEmpty()) {
            // Above the list on purpose: it is a question addressed to the user, and a question
            // parked below the fold is one that never gets answered.
            item(key = "pending_imports") {
                PendingImportsCard(
                    items = state.pendingImports,
                    currencyCode = state.currencyCode,
                    onResolve = { hash, keep ->
                        viewModel.onIntent(TransactionsIntent.ResolvePendingImport(hash, keep))
                    },
                )
            }
        }
        if (state.recurringCharges.isNotEmpty()) {
            item(key = "recurring_charges") {
                RecurringChargesCard(state.recurringCharges, state.currencyCode)
            }
        }
        state.groups.forEach { group ->
            item(key = "header_${group.date}") {
                DayHeader(group, state.currencyCode)
            }
            items(group.items, key = { it.id }) { item ->
                TransactionRowItem(
                    item = item,
                    currencyCode = state.currencyCode,
                    categoryLabel = item.category?.name ?: uncategorized,
                    onClick = { viewModel.onIntent(TransactionsIntent.EditClicked(item)) },
                    onDelete = { viewModel.onIntent(TransactionsIntent.DeleteRequested(item.id)) },
                )
            }
        }
        // Reaching the tail widens the page window (no-op once everything is loaded).
        item {
            LaunchedEffect(state.groups.size) { viewModel.onIntent(TransactionsIntent.LoadMore) }
            Spacer(Modifier.height(80.dp))
        }
    }
}

/**
 * A compact "we noticed these repeat" card for locally-detected subscriptions. Detection is
 * entirely on device, so this shows whether or not AI is enabled — it's a fact about the ledger,
 * not a model output.
 */
@Composable
private fun RecurringChargesCard(charges: List<RecurringCharge>, currencyCode: String) {
    // Flat: locally-detected suggestions about the list, not entries in it.
    AppCard(
        level = SurfaceLevel.Flat,
        contentPadding = Spacing.medium,
        verticalArrangement = Arrangement.spacedBy(Spacing.micro),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Autorenew,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(Res.string.transactions_recurring_detected),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        // Cap the list so a long history doesn't push the actual transactions off-screen.
        charges.take(5).forEach { charge ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = charge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${MoneyFormatter.format(charge.typicalAmount, currencyCode)} · ${charge.occurrences}×",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DayHeader(group: TransactionDayGroup, currencyCode: String) {
    val label = when (group.relative) {
        RelativeDay.TODAY -> stringResource(Res.string.transactions_today)
        RelativeDay.YESTERDAY -> stringResource(Res.string.transactions_yesterday)
        RelativeDay.OLDER -> group.date.toString()
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Text(
            text = MoneyFormatter.formatSigned(group.net, currencyCode),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyState(filtered: Boolean, onAdd: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        SharedEmptyState(
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            title = stringResource(Res.string.transactions_empty_title),
            subtitle = stringResource(
                if (filtered) Res.string.transactions_empty_filtered else Res.string.transactions_empty_subtitle,
            ),
            // A CTA only helps when the list is genuinely empty; adding an entry won't fix a
            // filter that matched nothing.
            actionLabel = if (filtered) null else stringResource(Res.string.empty_transactions_cta),
            onAction = if (filtered) null else onAdd,
        )
    }
}

@Composable
private fun TransactionEditor(state: TransactionsState, viewModel: TransactionsViewModel) {
    BoxWithConstraints {
        val isCompact = maxWidth < 600.dp
        val form: @Composable () -> Unit = {
            AddEditTransactionForm(
                editing = state.editor.editing,
                categories = state.categories,
                accounts = state.accounts,
                activeAccountId = state.activeAccountId,
                onSave = { viewModel.onIntent(TransactionsIntent.SaveTransaction(it)) },
                onCancel = { viewModel.onIntent(TransactionsIntent.EditorDismissed) },
                quickAddEnabled = state.quickAddEnabled,
                onQuickParse = viewModel::parseQuickEntry,
                                onSuggestCategory = viewModel::suggestCategory,
                                receiptScanEnabled = state.receiptScanEnabled,
                                onScanReceipt = viewModel::scanReceipt,
                                initialKind = state.editor.initialKind,
            )
        }
        if (isCompact) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.onIntent(TransactionsIntent.EditorDismissed) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
            ) { form() }
        } else {
            Dialog(onDismissRequest = { viewModel.onIntent(TransactionsIntent.EditorDismissed) }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.width(480.dp),
                ) { form() }
            }
        }
    }
}

/** Collects one-shot effects into the snackbar host (with an Undo action). */
@Composable
private fun LaunchedEffectEffects(
    viewModel: TransactionsViewModel,
    snackbarHostState: SnackbarHostState,
    undoLabel: String,
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TransactionsEffect.ShowUndoDelete -> {
                    val message = getString(Res.string.transactions_deleted, effect.description)
                    val result = snackbarHostState.showSnackbar(message = message, actionLabel = undoLabel)
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(TransactionsIntent.UndoDelete)
                    }
                }
                is TransactionsEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }
}

/**
 * Paste or share in a mobile-money message.
 *
 * The fallback capture path, and on platforms without SMS access the *only* one. It runs through
 * the same importer as automatic capture, so a message pasted here and the same transaction
 * captured later collapse on the provider id instead of double-counting.
 */
@Composable
private fun PasteMessageCard(onImport: suspend (String) -> ImportOutcome) {
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Resolved up front: a coroutine callback cannot call stringResource.
    val imported = stringResource(Res.string.transactions_paste_imported)
    val duplicate = stringResource(Res.string.transactions_paste_duplicate)
    val needsReview = stringResource(Res.string.transactions_paste_needs_review)
    val unreadable = stringResource(Res.string.transactions_paste_unreadable)

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.micro)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; result = null },
            label = { Text(stringResource(Res.string.transactions_paste_label)) },
            placeholder = { Text(stringResource(Res.string.transactions_paste_placeholder)) },
            enabled = !busy,
            minLines = 2,
            shape = RoundedCornerShape(14.dp),
            trailingIcon = {
                TextButton(
                    enabled = !busy && text.isNotBlank(),
                    onClick = {
                        busy = true
                        result = null
                        scope.launch {
                            result = when (onImport(text)) {
                                is ImportOutcome.Imported -> { text = ""; imported }
                                // Already-seen and already-recorded are both "we have this", which
                                // is a success from the user's point of view, not an error.
                                ImportOutcome.AlreadySeen,
                                is ImportOutcome.AlreadyRecorded -> duplicate
                                // Distinct from a duplicate: this one is a question, and the
                                // answer is now waiting a few rows down the same screen.
                                is ImportOutcome.NeedsReview -> { text = ""; needsReview }
                                ImportOutcome.NotRecognised -> unreadable
                            }
                            busy = false
                        }
                    },
                ) { Text(stringResource(Res.string.transactions_paste_action)) }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        result?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
