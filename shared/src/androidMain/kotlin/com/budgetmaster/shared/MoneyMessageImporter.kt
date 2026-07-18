package com.budgetmaster.shared

import android.content.Context
import android.net.Uri
import com.budgetmaster.core.prefs.AppSettingsRepository
import com.budgetmaster.transactions.domain.usecase.ImportMoneyMessageUseCase
import com.budgetmaster.transactions.domain.usecase.ImportOutcome
import com.budgetmaster.transactions.domain.usecase.ObserveTransactionAccountsUseCase
import kotlinx.coroutines.flow.first

/**
 * Shared entry point for both capture paths — the live SMS broadcast and the one-off inbox
 * backfill — so they cannot drift apart in how they resolve the wallet, the owner numbers, or the
 * opt-in.
 *
 * Every import is a no-op unless the user has switched it on *and* supplied their own number:
 * without the number the direction of a transfer is unknowable, so importing would be guessing at
 * the sign of someone's money.
 */
class MoneyMessageImporter(
    private val settingsRepository: AppSettingsRepository,
    private val observeAccounts: ObserveTransactionAccountsUseCase,
    private val importMessage: ImportMoneyMessageUseCase,
) {
    /** @return the outcome, or null when import is switched off or unconfigured. */
    suspend fun import(sender: String, body: String, receivedAt: Long): ImportOutcome? {
        val settings = settingsRepository.settings.first()
        if (!settings.smsImportEnabled) return null

        val owners = settings.smsOwnerMsisdns.parseMsisdns()
        if (owners.isEmpty()) return null

        // Imports land in the user's first wallet. A per-provider wallet mapping is the obvious
        // refinement, but guessing wrong here is recoverable — the entry can be moved — whereas
        // blocking on configuration would mean capturing nothing.
        val accountId = observeAccounts().first().firstOrNull()?.id ?: return null

        return importMessage(sender, body, receivedAt, accountId, owners)
    }

    /** Tolerates "659228030, 690440480" and stray spaces; digits are all that matter. */
    private fun String.parseMsisdns(): Set<String> =
        split(',', ';')
            .map { it.filter(Char::isDigit) }
            .filter { it.isNotBlank() }
            .toSet()
}

/**
 * Reads mobile-money messages already sitting in the inbox.
 *
 * Run once when the user switches import on, so the app starts with their history rather than
 * only whatever arrives next — the difference between a ledger that is useful today and one that
 * is useful in a month. Import is idempotent, so re-running is harmless.
 */
suspend fun backfillInbox(
    context: Context,
    importer: MoneyMessageImporter,
    senderFilter: (String) -> Boolean,
    limit: Int = 500,
): Int {
    var imported = 0
    val projection = arrayOf("address", "body", "date")

    context.contentResolver.query(
        Uri.parse("content://sms/inbox"),
        projection,
        null,
        null,
        "date DESC LIMIT $limit",
    )?.use { cursor ->
        val addressIndex = cursor.getColumnIndex("address")
        val bodyIndex = cursor.getColumnIndex("body")
        val dateIndex = cursor.getColumnIndex("date")
        if (addressIndex < 0 || bodyIndex < 0 || dateIndex < 0) return 0

        while (cursor.moveToNext()) {
            val sender = cursor.getString(addressIndex).orEmpty()
            // Cheapest possible rejection: anything not from a money provider is never read.
            if (!senderFilter(sender)) continue

            val outcome = importer.import(
                sender = sender,
                body = cursor.getString(bodyIndex).orEmpty(),
                receivedAt = cursor.getLong(dateIndex),
            )
            if (outcome is ImportOutcome.Imported) imported++
        }
    }
    return imported
}
