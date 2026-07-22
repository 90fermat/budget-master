package com.budgetmaster.shared

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Posts a system notification for a mobile-money import.
 *
 * Exists because live capture happens while the app is closed: an in-app inbox row alone is
 * invisible until the user next opens the app, which leaves "did my SMS import?" unanswerable in
 * the moment — the exact doubt this work removes. The notification taps through to the app.
 *
 * Silently does nothing when POST_NOTIFICATIONS is not granted (Android 13+). The in-app inbox
 * row is still written by the caller, so the information is never lost, only less immediate.
 */
class ImportSystemNotifier(private val context: Context) {

    fun notify(title: String, message: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager, channelName)

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP) }
        val contentIntent = launch?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(message)
            // The amount is money; keep it off the lock screen for the same reason FLAG_SECURE
            // keeps it out of the recents switcher.
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .apply { contentIntent?.let(::setContentIntent) }
            .build()

        // One id per import would pile up a stack of notifications after a burst of messages;
        // a single id keeps the latest and the inbox keeps the history.
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(manager: NotificationManager, channelName: String) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            // The content is financial; the channel-level lockscreen setting mirrors the
            // per-notification visibility so the user's Settings app shows the truth.
            lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
        }
        manager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "money_import"
        const val NOTIFICATION_ID = 4001
    }
}
