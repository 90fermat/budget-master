package com.budgetmaster.transactions.domain.model

/**
 * A wallet a transaction can be booked against, as far as this feature needs to know.
 *
 * Deliberately minimal: `:feature:transactions` must not depend on `:feature:accounts`, so it
 * reads `AccountEntity` through `:core`'s database and maps it to this local model — the same
 * pattern both features already use for categories.
 */
data class TransactionAccount(
    val id: String,
    val name: String,
    val currency: String,
)
