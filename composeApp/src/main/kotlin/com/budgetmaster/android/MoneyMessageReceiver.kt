package com.budgetmaster.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.budgetmaster.core.sms.MoneyMessageParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.budgetmaster.shared.MoneyMessageImporter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Captures mobile-money messages as they arrive.
 *
 * Multi-part messages are reassembled before parsing — a long transfer notification arrives split,
 * and half a message parses into a half-wrong transaction, which is worse than none.
 *
 * The sender allowlist is applied first, so a message from anyone other than a known money
 * provider is discarded without its body being examined at all. Import itself is idempotent and
 * a no-op until the user opts in, so a stray broadcast can never write anything.
 */
class MoneyMessageReceiver : BroadcastReceiver(), KoinComponent {

    private val importer: MoneyMessageImporter by inject()
    private val parsers: List<MoneyMessageParser> by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val sender = messages.first().originatingAddress.orEmpty()
        if (parsers.none { it.handlesSender(sender) }) return

        // Concatenated SMS arrives as several parts of one logical message.
        val body = messages.joinToString("") { it.displayMessageBody.orEmpty() }
        val receivedAt = messages.first().timestampMillis

        // goAsync keeps the receiver alive past onReceive; the work is a couple of local queries
        // and an insert, well inside the allowance.
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                importer.import(sender, body, receivedAt)
            } catch (e: Exception) {
                // A failed import must never crash the user's messaging pipeline; the message stays
                // in the inbox and the next backfill will pick it up.
            } finally {
                pending.finish()
            }
        }
    }
}
