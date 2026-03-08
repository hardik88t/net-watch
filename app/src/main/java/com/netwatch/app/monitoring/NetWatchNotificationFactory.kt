package com.netwatch.app.monitoring

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.netwatch.app.R

class NetWatchNotificationFactory(
    private val context: Context,
) {
    private val manager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            MONITOR_NOTIFICATION_CHANNEL_ID,
            "NetWatch Monitoring",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Persistent notification while NetWatch collects network diagnostics"
        }

        manager.createNotificationChannel(channel)
    }

    fun build(contentText: String? = null): Notification {
        return NotificationCompat.Builder(context, MONITOR_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(context.getString(R.string.monitoring_notification_title))
            .setContentText(contentText ?: context.getString(R.string.monitoring_notification_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }
}
