package com.budgetmaster.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.budgetmaster.core.sms.MoneyMessageParser
import org.koin.compose.koinInject

/**
 * Reads the device inbox for mobile-money messages and imports them.
 *
 * Only senders a parser claims are ever looked at, so unrelated messages are never opened.
 */
@Composable
actual fun rememberMessageBackfill(): suspend () -> Int {
    val context = LocalContext.current
    val importer = koinInject<MoneyMessageImporter>()
    val parsers = koinInject<List<MoneyMessageParser>>()

    return remember(context, importer, parsers) {
        {
            backfillInbox(
                context = context,
                importer = importer,
                senderFilter = { sender -> parsers.any { it.handlesSender(sender) } },
            )
        }
    }
}
