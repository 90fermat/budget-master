package com.budgetmaster.core.notifications

/**
 * Posts a notification the operating system shows, outside the app.
 *
 * The in-app inbox is a record; this is the part the user actually sees in the moment. For anything
 * time-sensitive the difference is the whole value: a spending warning read three days later, when
 * the app next happens to be opened, has already stopped being a warning.
 *
 * Lives in `:core` so both the SMS importer and the budget alerts can reach it — they sit in
 * different modules and the architecture rules forbid one feature importing another.
 *
 * Callers still write the inbox row themselves. This can fail silently — permission refused,
 * platform without notifications — and the information must survive that.
 */
interface SystemNotifier {

    /**
     * @param channelId groups related notifications in the OS settings, and is stable forever:
     *   changing it strands whatever the user configured against the old one.
     * @param channelName the group's name as the user sees it in those settings, so translated.
     * @param tag identifies this notification. Posting the same tag twice replaces rather than
     *   stacks, which is what makes a repeated budget alert one warning instead of a pile.
     */
    fun post(channelId: String, channelName: String, tag: String, title: String, message: String)
}

/** Channel ids, fixed for the life of the app. */
object NotificationChannels {
    const val MONEY_IMPORT = "money_import"
    const val BUDGET_ALERTS = "budget_alerts"
}

/** The platform's notifier: the real thing on Android, a no-op where there is nothing to post to. */
expect fun createSystemNotifier(): SystemNotifier
