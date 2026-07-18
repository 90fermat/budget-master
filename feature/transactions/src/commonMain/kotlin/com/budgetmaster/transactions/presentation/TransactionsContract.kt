package com.budgetmaster.transactions.presentation

import com.budgetmaster.core.util.RelativeDay
import com.budgetmaster.transactions.domain.usecase.RecurringCharge
import com.budgetmaster.transactions.domain.model.TransactionAccount
import com.budgetmaster.transactions.domain.model.TransactionCategory
import com.budgetmaster.transactions.domain.model.TransactionDraft
import com.budgetmaster.transactions.domain.model.TransactionItem
import com.budgetmaster.transactions.domain.model.TypeFilter
import kotlinx.datetime.LocalDate

/** User actions on the Transactions screen. */
sealed interface TransactionsIntent {
    data class SearchChanged(val query: String) : TransactionsIntent
    data class CategoryFilterChanged(val categoryId: String?) : TransactionsIntent
    data class TypeFilterChanged(val type: TypeFilter) : TransactionsIntent
    data class DeleteRequested(val id: String) : TransactionsIntent
    data object UndoDelete : TransactionsIntent
    data class SaveTransaction(val draft: TransactionDraft) : TransactionsIntent
    data object AddClicked : TransactionsIntent

    /** The list reached its end; widen the page window. */
    data object LoadMore : TransactionsIntent
    data class EditClicked(val item: TransactionItem) : TransactionsIntent
    data object EditorDismissed : TransactionsIntent

    /**
     * Answers one review-queue item.
     *
     * @param keep true to record it as a separate transaction, false to accept it as the one the
     *   user already entered.
     */
    data class ResolvePendingImport(val hash: String, val keep: Boolean) : TransactionsIntent
}

/**
 * One review-queue item, ready to render.
 *
 * Carries what the message said rather than a reference to it, because message bodies are never
 * stored — the parsed figures are the whole of what the user has to judge by, alongside their own
 * ledger below.
 */
data class PendingImportItem(
    val hash: String,
    /** Display name of the provider, e.g. "Orange Money". */
    val provider: String,
    val description: String,
    /** Signed, principal only; the fee is mentioned separately when there is one. */
    val amount: Double,
    val fee: Double,
    val occurredAt: Long,
)

/** One-shot side effects. */
sealed interface TransactionsEffect {
    data class ShowUndoDelete(val description: String) : TransactionsEffect
    data class ShowError(val message: String) : TransactionsEffect
}

/**
 * A day-grouped bucket of transactions for the list.
 *
 * @property date The calendar day of the group.
 * @property relative Today / Yesterday / Older, for a localized header.
 * @property items Transactions on this day (newest first).
 * @property net Signed sum of the day's transactions.
 */
data class TransactionDayGroup(
    val date: LocalDate,
    val relative: RelativeDay,
    val items: List<TransactionItem>,
    val net: Double,
)

/** State of the editor sheet/dialog. */
data class EditorState(
    val visible: Boolean = false,
    val editing: TransactionItem? = null,
)

/**
 * Immutable UI state of the Transactions screen.
 *
 * @property isLoading True until the first data emission.
 * @property groups Day-grouped, filtered transactions.
 * @property categories Available categories for chips and the picker.
 * @property query Current search text.
 * @property categoryFilterId Active category filter, or null for all.
 * @property typeFilter Active income/expense/all filter.
 * @property currencyCode ISO currency for formatting amounts.
 * @property accounts The user's active wallets, for the editor's account picker.
 * @property activeAccountId The wallet the app is scoped to (`null` = All accounts); used to
 *   preselect the account for a new entry.
 * @property editor Editor sheet state.
 * @property isEmpty True when there are no transactions matching the filter.
 */
data class TransactionsState(
    val isLoading: Boolean = true,
    val groups: List<TransactionDayGroup> = emptyList(),
    val categories: List<TransactionCategory> = emptyList(),
    val query: String = "",
    val categoryFilterId: String? = null,
    val typeFilter: TypeFilter = TypeFilter.ALL,
    val currencyCode: String = "USD",
    val accounts: List<TransactionAccount> = emptyList(),
    val activeAccountId: String? = null,
    val editor: EditorState = EditorState(),
    /** True when AI quick-add is available and the user has opted in; shows the parse field. */
    val quickAddEnabled: Boolean = false,
    /** True when on-device OCR and an AI provider are both available and the user opted in. */
    val receiptScanEnabled: Boolean = false,
    /** Locally-detected likely subscriptions; empty until enough history exists. No AI involved. */
    val recurringCharges: List<RecurringCharge> = emptyList(),
    /**
     * Captured messages the importer could not place. Empty for almost everyone, almost always —
     * the whole review section disappears with it.
     */
    val pendingImports: List<PendingImportItem> = emptyList(),
) {
    val isEmpty: Boolean get() = !isLoading && groups.isEmpty()
}
