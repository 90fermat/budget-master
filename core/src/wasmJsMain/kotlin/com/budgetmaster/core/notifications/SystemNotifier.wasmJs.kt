package com.budgetmaster.core.notifications

/**
 * No system notifications here yet.
 *
 * The in-app inbox still records everything, so nothing is lost — only the immediacy, which is
 * exactly what this interface exists to add and what these platforms do not yet wire up.
 */
private object NoSystemNotifier : SystemNotifier {
    override fun post(
        channelId: String,
        channelName: String,
        tag: String,
        title: String,
        message: String,
    ) = Unit
}

actual fun createSystemNotifier(): SystemNotifier = NoSystemNotifier
