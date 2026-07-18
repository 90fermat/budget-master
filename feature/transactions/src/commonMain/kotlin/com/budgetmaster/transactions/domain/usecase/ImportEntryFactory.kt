package com.budgetmaster.transactions.domain.usecase

import com.budgetmaster.transactions.domain.repository.ImportedEntry

/**
 * Builds the ledger rows for one captured money message.
 *
 * Shared because entries get built in two places — at first import, and again when the user
 * resolves a pending review in the importer's favour. Two copies of this logic would drift, and
 * the failure mode is silent: a reviewed import missing its fee row would leave the balance
 * quietly wrong.
 */
internal object ImportEntryFactory {
    const val FEES_CATEGORY = "cat_fees"
    const val FEE_LABEL = "Fee"

    /** Suffixed so the fee row is unique, and so both rows stay traceable to one message. */
    const val FEE_ID_SUFFIX = "#fee"

    /**
     * @param amount signed principal — negative for money out.
     * @param fee always charged as money out, even on an incoming transfer. 0.0 for none.
     */
    fun build(
        accountId: String,
        amount: Double,
        fee: Double,
        description: String,
        occurredAt: Long,
        externalId: String,
    ): List<ImportedEntry> {
        val principal = ImportedEntry(
            accountId = accountId,
            // Uncategorised on purpose. A transfer or merchant payment could be anything, and a
            // wrong category is worse than none: it silently skews budgets and reports, and the
            // user has no reason to go looking for it.
            categoryId = null,
            amount = amount,
            description = description,
            timestamp = occurredAt,
            externalId = externalId,
        )
        if (fee <= 0.0) return listOf(principal)

        return listOf(
            principal,
            ImportedEntry(
                accountId = accountId,
                categoryId = FEES_CATEGORY,
                amount = -fee,
                description = "$FEE_LABEL — $description",
                timestamp = occurredAt,
                externalId = "$externalId$FEE_ID_SUFFIX",
            ),
        )
    }
}
