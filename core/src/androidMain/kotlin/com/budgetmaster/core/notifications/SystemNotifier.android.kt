package com.budgetmaster.core.notifications

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
import com.budgetmaster.core.db.AppContextHolder

/**
 * Posts through Android's notification manager.
 *
 * Silently does nothing when POST_NOTIFICATIONS has not been granted (Android 13+). That is not
 * negligence: the caller has already written the in-app inbox row, so refusing the permission costs
 * immediacy and never the information itself.
 */
internal class AndroidSystemNotifier(private val context: Context) : SystemNotifier {

    override fun post(
        channelId: String,
        channelName: String,
        tag: String,
        title: String,
        message: String,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager, channelId, channelName)

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

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(message)
            // These are amounts; keep them off the lock screen for the same reason FLAG_SECURE
            // keeps them out of the recents switcher.
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .apply { contentIntent?.let(::setContentIntent) }
            .build()

        // Derived from the tag so that re-posting the same thing replaces it. A burst of imports,
        // or a budget that keeps crossing its limit, is one notification and a full inbox history
        // rather than a stack the user has to clear.
        manager.notify(tag.hashCode(), notification)
    }

    private fun ensureChannel(manager: NotificationManager, channelId: String, channelName: String) {
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            // The content is financial; the channel-level lockscreen setting mirrors the
            // per-notification visibility so the user's Settings app shows the truth.
            lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
        }
        manager.createNotificationChannel(channel)
    }
}

actual fun createSystemNotifier(): SystemNotifier = AndroidSystemNotifier(AppContextHolder.context)
