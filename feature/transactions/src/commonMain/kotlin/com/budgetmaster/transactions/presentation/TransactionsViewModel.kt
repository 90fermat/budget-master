@file:OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)

package com.budgetmaster.transactions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmaster.core.prefs.AppSettingsRepository
import com.budgetmaster.core.session.ActiveAccountStore
import com.budgetmaster.core.util.DateUtils
import com.budgetmaster.transactions.domain.model.TransactionFilter
import com.budgetmaster.transactions.domain.model.TransactionItem
import com.budgetmaster.transactions.domain.usecase.DeleteTransactionUseCase
import com.budgetmaster.transactions.domain.usecase.DetectRecurringChargesUseCase
import com.budgetmaster.transactions.domain.usecase.ObserveCategoriesUseCase
import com.budgetmaster.transactions.domain.usecase.ObserveTransactionAccountsUseCase
import com.budgetmaster.transactions.domain.usecase.ObserveTransactionsUseCase
import com.budgetmaster.transactions.domain.usecase.ParseQuickEntryUseCase
import com.budgetmaster.transactions.domain.usecase.QuickEntryDraft
import com.budgetmaster.transactions.domain.usecase.RestoreTransactionUseCase
import com.budgetmaster.transactions.domain.usecase.ImportMoneyMessageUseCase
import com.budgetmaster.transactions.domain.usecase.ImportOutcome
import com.budgetmaster.transactions.domain.usecase.ObservePendingImportsUseCase
import com.budgetmaster.transactions.domain.usecase.ParseReceiptUseCase
import com.budgetmaster.transactions.domain.usecase.ResolvePendingImportUseCase
import com.budgetmaster.transactions.domain.usecase.SuggestCategoryUseCase
import com.budgetmaster.core.ocr.ReceiptImage
import com.budgetmaster.transactions.domain.usecase.SaveTransactionUseCase
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * MVI ViewModel for the Transactions screen.
 *
 * The active [TransactionFilter] drives a `flatMapLatest` re-subscription so search
 * and filter changes re-query reactively. Results are grouped by day for the UI.
 */
class TransactionsViewModel(
    private val observeTransactions: ObserveTransactionsUseCase,
    observeCategories: ObserveCategoriesUseCase,
    observeAccounts: ObserveTransactionAccountsUseCase,
    private val settingsRepository: AppSettingsRepository,
    activeAccountStore: ActiveAccountStore,
    private val saveTransaction: SaveTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val restoreTransaction: RestoreTransactionUseCase,
    private val parseQuickEntry: ParseQuickEntryUseCase,
    private val detectRecurringCharges: DetectRecurringChargesUseCase,
    private val suggestCategory: SuggestCategoryUseCase,
    private val parseReceipt: ParseReceiptUseCase,
    private val importMoneyMessage: ImportMoneyMessageUseCase,
    observePendingImports: ObservePendingImportsUseCase,
    private val resolvePendingImport: ResolvePendingImportUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionsState())
    val state: StateFlow<TransactionsState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<TransactionsEffect>()
    val effects: SharedFlow<TransactionsEffect> = _effects.asSharedFlow()

    private val filter = MutableStateFlow(TransactionFilter())

    /** Holds the last-deleted transaction so it can be restored via undo. */
    private var lastDeleted: TransactionItem? = null

    init {
        observeCategories()
            .onEach { categories -> _state.update { it.copy(categories = categories) } }
            .launchIn(viewModelScope)

        observeAccounts()
            .onEach { accounts -> _state.update { it.copy(accounts = accounts) } }
            .launchIn(viewModelScope)

        observePendingImports()
            .catch { /* The queue is auxiliary: a failure here must not take the list down. */ }
            .onEach { pending ->
                _state.update { state ->
                    state.copy(
                        pendingImports = pending.map {
                            PendingImportItem(
                                hash = it.hash,
                                provider = it.provider,
                                description = it.details.description,
                                amount = it.details.amount,
                                fee = it.details.fee,
                                occurredAt = it.details.occurredAt,
                            )
                        },
                    )
                }
            }
            .launchIn(viewModelScope)

        settingsRepository.settings
            .onEach { settings ->
                _state.update {
                    it.copy(
                        currencyCode = settings.currency,
                        // Quick-add appears only when a provider exists *and* the user opted in —
                        // it sends the typed text to the model, so it obeys the same consent gate
                        // as every other AI surface.
                        quickAddEnabled = parseQuickEntry.isAvailable && settings.aiEnabled,
                        receiptScanEnabled = parseReceipt.isAvailable && settings.aiEnabled,
                    )
                }
            }
            .launchIn(viewModelScope)

        activeAccountStore.activeAccountId
            .onEach { id -> _state.update { it.copy(activeAccountId = id) } }
            .launchIn(viewModelScope)

        filter
            .flatMapLatest { f -> observeTransactions(f) }
            .catch { e -> emitEffect(TransactionsEffect.ShowError(e.message ?: "Failed to load transactions.")) }
            .onEach { items ->
                _state.update { it.copy(isLoading = false, groups = groupByDay(items)) }
            }
            .launchIn(viewModelScope)

        // Recurring-charge detection runs over the *unfiltered* history (search/category filters
        // would hide the very repeats we're looking for) and is entirely local — no AI, no
        // network. Kept in its own flow so filtering the list doesn't recompute it.
        observeTransactions(TransactionFilter())
            .onEach { items ->
                _state.update { it.copy(recurringCharges = detectRecurringCharges(items)) }
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: TransactionsIntent) {
        when (intent) {
            is TransactionsIntent.SearchChanged -> {
                _state.update { it.copy(query = intent.query) }
                filter.update { it.copy(query = intent.query) }
            }
            is TransactionsIntent.CategoryFilterChanged -> {
                _state.update { it.copy(categoryFilterId = intent.categoryId) }
                filter.update { it.copy(categoryId = intent.categoryId) }
            }
            is TransactionsIntent.TypeFilterChanged -> {
                _state.update { it.copy(typeFilter = intent.type) }
                filter.update { it.copy(type = intent.type) }
            }
            is TransactionsIntent.LoadMore -> {
                // Only meaningful while browsing: a filtered query is already unbounded.
                if (filter.value.isEmpty) {
                    filter.update { it.copy(limit = it.limit + TransactionFilter.DEFAULT_PAGE_SIZE) }
                }
            }
            is TransactionsIntent.DeleteRequested -> delete(intent.id)
            is TransactionsIntent.UndoDelete -> undoDelete()
            is TransactionsIntent.SaveTransaction -> save(intent)
            is TransactionsIntent.AddClicked ->
                _state.update {
                    it.copy(
                        editor = EditorState(
                            visible = true,
                            editing = null,
                            initialKind = intent.kind,
                        ),
                    )
                }
            is TransactionsIntent.EditClicked ->
                _state.update { it.copy(editor = EditorState(visible = true, editing = intent.item)) }
            is TransactionsIntent.EditorDismissed ->
                _state.update { it.copy(editor = EditorState(visible = false)) }
            is TransactionsIntent.ResolvePendingImport -> viewModelScope.launch {
                // The list is observed, so the answered item leaves the queue on its own; there is
                // nothing to remove from state by hand.
                resolvePendingImport(intent.hash, intent.keep)
            }
        }
    }

    /**
     * Parses a natural-language note ("coffee 4.50 yesterday") into draft fields the editor
     * prefills. Suspends for the form to await; nothing is saved — the user still confirms.
     */
    suspend fun parseQuickEntry(text: String): Result<QuickEntryDraft> =
        parseQuickEntry.invoke(text, _state.value.categories)

    /** Suggests a category for a typed description, or null. Cached per merchant; safe to call
     *  as the user types. */
    suspend fun suggestCategory(description: String): String? =
        suggestCategory.invoke(description, _state.value.categories)

    /**
     * Imports a mobile-money message the user pasted or shared in.
     *
     * The fallback path for the three cases automatic capture cannot serve: the SMS permission
     * refused or unavailable, **iOS where SMS access does not exist at all**, and messages the
     * receiver missed. It runs through the same importer, so a pasted message and a later
     * automatic capture of the same transaction collapse on the provider id rather than
     * double-counting.
     */
    suspend fun importPastedMessage(text: String): ImportOutcome {
        val settings = settingsRepository.settings.first()
        val owners = settings.smsOwnerMsisdns
            .split(',', ';')
            .map { it.filter(Char::isDigit) }
            .filter { it.isNotBlank() }
            .toSet()
        return importMoneyMessage(
            // No sender: the importer falls back to matching on the body.
            sender = "",
            body = text,
            receivedAt = Clock.System.now().toEpochMilliseconds(),
            // The configured destination for the provider first. Falling back is right *here*,
            // unlike in automatic capture: the user is present, pasting into this screen, and the
            // resulting row appears in front of them - so the active wallet is their context, not
            // a guess. The old code used the first wallet ever created, which is how a paste
            // landed in an account the user wasn't looking at.
            accountFor = { provider ->
                settings.smsImportAccounts[provider]
                    ?: _state.value.activeAccountId
                    ?: _state.value.accounts.firstOrNull()?.id
            },
            ownerMsisdns = owners,
        )
    }

    /**
     * OCRs a photographed receipt on-device, then parses the extracted text into draft fields.
     * The image never leaves the phone; only the recognised text is summarised to the model.
     */
    suspend fun scanReceipt(image: ReceiptImage): Result<QuickEntryDraft> =
        parseReceipt.invoke(image, _state.value.categories)

    private fun delete(id: String) {
        val target = _state.value.groups.flatMap { it.items }.firstOrNull { it.id == id } ?: return
        lastDeleted = target
        viewModelScope.launch {
            try {
                deleteTransaction(id)
                emitEffect(TransactionsEffect.ShowUndoDelete(target.description))
            } catch (e: Exception) {
                emitEffect(TransactionsEffect.ShowError(e.message ?: "Failed to delete."))
            }
        }
    }

    private fun undoDelete() {
        val deleted = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch {
            try {
                restoreTransaction(deleted)
            } catch (e: Exception) {
                emitEffect(TransactionsEffect.ShowError(e.message ?: "Failed to undo."))
            }
        }
    }

    private fun save(intent: TransactionsIntent.SaveTransaction) {
        viewModelScope.launch {
            try {
                saveTransaction(intent.draft)
                _state.update { it.copy(editor = EditorState(visible = false)) }
            } catch (e: IllegalArgumentException) {
                emitEffect(TransactionsEffect.ShowError(e.message ?: "Invalid transaction."))
            } catch (e: Exception) {
                emitEffect(TransactionsEffect.ShowError(e.message ?: "Failed to save."))
            }
        }
    }

    private fun groupByDay(items: List<TransactionItem>): List<TransactionDayGroup> =
        items.groupBy { DateUtils.toLocalDate(it.timestamp) }
            .toList()
            .sortedByDescending { (date, _) -> date }
            .map { (date, dayItems) ->
                TransactionDayGroup(
                    date = date,
                    relative = DateUtils.relativeDay(date),
                    items = dayItems.sortedByDescending { it.timestamp },
                    net = dayItems.sumOf { it.amount },
                )
            }

    private fun emitEffect(effect: TransactionsEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }
}
