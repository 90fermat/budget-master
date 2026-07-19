package com.budgetmaster.dashboard.domain.model

/**
 * A full-fidelity snapshot of a deleted row, kept so the delete can be undone exactly.
 *
 * The dashboard's [com.budgetmaster.core.model.Transaction] is a *display* model — it carries an
 * amount, a category label, a description and a timestamp, and nothing else. Restoring from it
 * would silently invent an `accountId` and drop `externalId` and `source`, which would put the
 * money in the wrong wallet and break mobile-money deduplication for that entry: the provider's
 * next re-send would import as a second transaction.
 *
 * So the repository captures the whole row before deleting it and hands back this snapshot. The
 * ViewModel holds it opaquely; only the repository knows how to put it back.
 */
data class DeletedTransaction(
    val id: String,
    val accountId: String,
    val categoryId: String?,
    val amount: Double,
    val description: String,
    val timestamp: Long,
    val notes: String?,
    val tags: String?,
    val isRecurring: Long,
    val transferGroupId: String?,
    val externalId: String?,
    val source: String,
)
