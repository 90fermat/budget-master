package com.budgetmaster.shared

import android.content.Context
import android.net.Uri
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.import_notif_imported_body
import budgetmaster.core.generated.resources.import_notif_imported_title
import budgetmaster.core.generated.resources.import_notif_no_wallet_body
import budgetmaster.core.generated.resources.import_notif_no_wallet_title
import budgetmaster.core.generated.resources.import_notif_review_body
import budgetmaster.core.generated.resources.import_notif_review_title
import com.budgetmaster.core.db.WalletDirectory
import com.budgetmaster.core.notifications.NotificationRepository
import com.budgetmaster.core.prefs.AppSettingsRepository
import com.budgetmaster.core.sms.moneyProviderLabelRes
import com.budgetmaster.core.util.MoneyFormatter
import com.budgetmaster.core.util.formatSigned
import com.budgetmaster.transactions.domain.usecase.ImportMoneyMessageUseCase
import com.budgetmaster.transactions.domain.usecase.ImportOutcome
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.getString
import com.budgetmaster.core.localization.applyAppLanguageToProcess
import budgetmaster.core.generated.resources.import_notif_channel_name
import com.budgetmaster.core.notifications.SystemNotifier
import com.budgetmaster.core.notifications.NotificationChannels

/**
 * Shared entry point for both capture paths — the live SMS broadcast and the one-off inbox
 * backfill — so they cannot drift apart in how they resolve the wallet, the owner numbers, or the
 * opt-in.
 *
 * Every import is a no-op unless the user has switched it on *and* supplied their own number:
 * without the number the direction of a transfer is unknowable, so importing would be guessing at
 * the sign of someone's money.
 *
 * Two properties were added after the second device test, and both are about accountability:
 *
 * **The destination is the user's explicit choice, never a guess.** Imports used to land in the
 * first wallet ever created, which put money in an account the user was not looking at. Now the
 * per-provider destination from Settings is the only thing consulted; a parsed message with no
 * destination configured becomes a "choose a wallet" notification instead of a misplaced entry.
 *
 * **Every outcome announces itself.** Imported, needs-review, and unconfigured outcomes each
 * write an inbox notification; live captures additionally post a system notification, because
 * the app is closed when an SMS arrives. Silence was previously indistinguishable from failure,
 * which made "is automatic capture working at all?" unanswerable even for its author.
 *
 * Notification text is resolved at write time, in the app language of that moment. Notifications
 * are historical records: one written in French stays French after a language switch, the same
 * way an email does.
 */
class MoneyMessageImporter(
    private val settingsRepository: AppSettingsRepository,
    private val walletDirectory: WalletDirectory,
    private val notifications: NotificationRepository,
    private val systemNotifier: SystemNotifier,
    private val importMessage: ImportMoneyMessageUseCase,
) {
    /**
     * @param live true for a just-arrived SMS (posts a system notification too), false for
     *   backfill (in-app inbox rows only — five hundred system notifications is an attack).
     * @return the outcome, or null when import is switched off or unconfigured.
     */
    suspend fun import(
        sender: String,
        body: String,
        receivedAt: Long,
        live: Boolean = true,
    ): ImportOutcome? {
        val settings = settingsRepository.settings.first()
        if (!settings.smsImportEnabled) return null

        // An SMS can arrive with the app closed, in a process where nothing has ever composed — so
        // the chosen language has never been applied and every string below would resolve in the
        // device's language instead. Someone whose phone is in English and whose app is in French
        // was reading English notifications about their own money.
        applyAppLanguageToProcess(settings.language.tag)

        val owners = settings.smsOwnerMsisdns.parseMsisdns()
        if (owners.isEmpty()) return null

        val outcome = importMessage(
            sender = sender,
            body = body,
            receivedAt = receivedAt,
            accountFor = { provider -> settings.smsImportAccounts[provider] },
            ownerMsisdns = owners,
        )
        announce(outcome, settings.currency, live)
        return outcome
    }

    /** Writes the inbox row (and, for live captures, the system notification) for [outcome]. */
    private suspend fun announce(outcome: ImportOutcome, currencyCode: String, live: Boolean) {
        // The channel name shows in Android's own Settings, so it is translated like everything
        // else the user reads. Resolved here rather than in the notifier because only this side
        // can suspend to read a string resource.
        val channelName = getString(Res.string.import_notif_channel_name)

        when (outcome) {
            is ImportOutcome.Imported -> {
                val wallet = walletDirectory.observeWallets().first()
                    .firstOrNull { it.id == outcome.accountId }?.name ?: outcome.accountId
                val title = getString(Res.string.import_notif_imported_title)
                val bodyText = getString(
                    Res.string.import_notif_imported_body,
                    providerLabel(outcome.provider),
                    MoneyFormatter.formatSigned(outcome.amount, currencyCode),
                    outcome.description,
                    wallet,
                )
                notifications.notify(title, bodyText)
                if (live) systemNotifier.post(
                    channelId = NotificationChannels.MONEY_IMPORT,
                    channelName = channelName,
                    // One tag for every import: a burst of messages leaves the latest on screen
                    // and the full history in the inbox, rather than a stack to dismiss.
                    tag = NotificationChannels.MONEY_IMPORT,
                    title = title,
                    message = bodyText,
                )
            }

            is ImportOutcome.NeedsReview -> {
                val title = getString(Res.string.import_notif_review_title)
                val bodyText = getString(
                    Res.string.import_notif_review_body,
                    providerLabel(outcome.provider),
                    MoneyFormatter.formatSigned(outcome.amount, currencyCode),
                    outcome.description,
                )
                notifications.notify(title, bodyText)
                if (live) systemNotifier.post(
                    channelId = NotificationChannels.MONEY_IMPORT,
                    channelName = channelName,
                    // One tag for every import: a burst of messages leaves the latest on screen
                    // and the full history in the inbox, rather than a stack to dismiss.
                    tag = NotificationChannels.MONEY_IMPORT,
                    title = title,
                    message = bodyText,
                )
            }

            is ImportOutcome.NoDestination -> {
                val title = getString(Res.string.import_notif_no_wallet_title)
                val bodyText = getString(
                    Res.string.import_notif_no_wallet_body,
                    providerLabel(outcome.provider),
                )
                // Idempotent id: a burst of messages with no wallet configured is one problem,
                // not one notification per message.
                notifications.notify(title, bodyText, id = "import_no_wallet_${outcome.provider}")
                if (live) systemNotifier.post(
                    channelId = NotificationChannels.MONEY_IMPORT,
                    channelName = channelName,
                    // One tag for every import: a burst of messages leaves the latest on screen
                    // and the full history in the inbox, rather than a stack to dismiss.
                    tag = NotificationChannels.MONEY_IMPORT,
                    title = title,
                    message = bodyText,
                )
            }

            // Dedup outcomes are working-as-intended and would only be noise; NotRecognised is
            // an advert or OTP from a money sender, which is not the user's business to triage.
            ImportOutcome.AlreadySeen,
            is ImportOutcome.AlreadyRecorded,
            ImportOutcome.NotRecognised,
            -> Unit
        }
    }

    private suspend fun providerLabel(provider: String): String =
        moneyProviderLabelRes(provider)?.let { getString(it) } ?: provider

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
                // Inbox rows only: a 500-message backfill must not post 500 system notifications.
                live = false,
            )
            if (outcome is ImportOutcome.Imported) imported++
        }
    }
    return imported
}
