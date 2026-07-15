package com.budgetmaster.transactions.domain.model

/**
 * A user-facing spending/income category.
 *
 * @property id Stable identifier (matches `CategoryEntity.id`).
 * @property name Display name.
 * @property icon Emoji shown in the category chip/avatar.
 * @property colorHex Accent color as `#RRGGBB`, used to tint the category avatar.
 */
data class TransactionCategory(
    val id: String,
    val name: String,
    val icon: String,
    val colorHex: String,
)
