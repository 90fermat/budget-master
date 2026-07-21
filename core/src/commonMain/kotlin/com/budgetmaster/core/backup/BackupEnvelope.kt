package com.budgetmaster.core.backup

import kotlinx.serialization.Serializable

/**
 * A complete, restorable snapshot of the user's data.
 *
 * Versioned so a file written today can still be read after the schema moves: [formatVersion] is
 * checked on import, and an unknown one is refused with an explanation rather than parsed into
 * something wrong. Fields are added, never repurposed.
 *
 * **What is in here and what is not.** Everything the user would miss is included, including the
 * imported-message ledger — unlike sync, which deliberately leaves that on the device. The
 * reasoning differs because the artefacts differ: a backup is encrypted under the user's own
 * passphrase and goes where they choose, and restoring onto a device genuinely benefits from the
 * dedup ledger, so a re-sent SMS is not imported twice after a restore.
 *
 * Excluded: exchange rates (public market data, refetched in a day) and AI insights (a derived
 * cache of the user's spending that would only go stale).
 */
@Serializable
data class BackupEnvelope(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    /** Schema version the snapshot was taken at, for diagnosing a future migration mismatch. */
    val schemaVersion: Long,
    val createdAt: Long,
    val users: List<BackupUser> = emptyList(),
    val accounts: List<BackupAccount> = emptyList(),
    val categories: List<BackupCategory> = emptyList(),
    val transactions: List<BackupTransaction> = emptyList(),
    val budgets: List<BackupBudget> = emptyList(),
    val goals: List<BackupGoal> = emptyList(),
    val recurring: List<BackupRecurring> = emptyList(),
    val notifications: List<BackupNotification> = emptyList(),
    val importedMessages: List<BackupImportedMessage> = emptyList(),
) {
    companion object {
        /** Bumped only when the shape changes incompatibly. */
        const val CURRENT_FORMAT_VERSION = 1
    }
}

@Serializable
data class BackupUser(
    val id: String,
    val name: String,
    val email: String,
    val currency: String,
    val createdAt: Long,
)

@Serializable
data class BackupAccount(
    val id: String,
    val userId: String,
    val name: String,
    val type: String,
    val balance: Double,
    val currency: String,
    val createdAt: Long,
    val isArchived: Long,
    /**
     * Defaulted, so a backup written before this column existed still restores — an added field
     * with a default is not a breaking change and needs no format-version bump.
     */
    val includeInTotals: Long = 1,
)

@Serializable
data class BackupCategory(
    val id: String,
    val userId: String,
    val name: String,
    val icon: String,
    val color: String,
    val isDefault: Long,
)

@Serializable
data class BackupTransaction(
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
    /** Carried so a restored ledger still deduplicates against re-sent provider messages. */
    val externalId: String?,
    val source: String,
)

@Serializable
data class BackupBudget(
    val id: String,
    val userId: String,
    val categoryId: String,
    val amount: Double,
    val spent: Double,
    val startDate: Long,
    val endDate: Long,
)

@Serializable
data class BackupGoal(
    val id: String,
    val userId: String,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: Long,
    val createdAt: Long,
)

@Serializable
data class BackupRecurring(
    val id: String,
    val accountId: String,
    val categoryId: String?,
    val amount: Double,
    val description: String,
    val frequency: String,
    val startDate: Long,
    val nextRunDate: Long,
    val isActive: Long,
)

@Serializable
data class BackupNotification(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Long,
)

@Serializable
data class BackupImportedMessage(
    val hash: String,
    val userId: String,
    val provider: String,
    val sender: String,
    val receivedAt: Long,
    val status: String,
    val transactionId: String?,
    val externalId: String?,
    val pendingAccountId: String?,
    val pendingAmount: Double?,
    val pendingFee: Double?,
    val pendingDescription: String?,
    val pendingOccurredAt: Long?,
)
