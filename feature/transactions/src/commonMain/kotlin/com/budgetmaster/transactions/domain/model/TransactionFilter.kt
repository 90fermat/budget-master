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
) {
    /** True when no narrowing is applied. */
    val isEmpty: Boolean
        get() = query.isBlank() && categoryId == null && type == TypeFilter.ALL
}
