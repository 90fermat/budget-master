package com.budgetmaster.transactions.domain.model

/** Direction filter applied to the transaction list. */
enum class TypeFilter { ALL, INCOME, EXPENSE }

/**
 * The active filter/search criteria for the transaction list.
 *
 * @property query Case-insensitive substring matched against description and notes.
 * @property categoryId Restrict to a single category, or `null` for all.
 * @property type Restrict to income, expense, or all.
 */
data class TransactionFilter(
    val query: String = "",
    val categoryId: String? = null,
    val type: TypeFilter = TypeFilter.ALL,
    /**
     * How many of the most recent rows to load while browsing unfiltered. Grows as the user
     * scrolls, keeping very large histories cheap. Ignored while a filter is active — search
     * must see every row, not just the loaded window.
     */
    val limit: Long = DEFAULT_PAGE_SIZE,
) {
    /** True when no narrowing is applied. */
    val isEmpty: Boolean
        get() = query.isBlank() && categoryId == null && type == TypeFilter.ALL

    /** The row limit to send to the database: the window when browsing, unlimited when filtering. */
    val effectiveLimit: Long get() = if (isEmpty) limit else NO_LIMIT

    companion object {
        /** Rows fetched initially, and added on each "load more". */
        const val DEFAULT_PAGE_SIZE = 100L

        /** SQLite treats a negative LIMIT as unbounded. */
        const val NO_LIMIT = -1L
    }
}
