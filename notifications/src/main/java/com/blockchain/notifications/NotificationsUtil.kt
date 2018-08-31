package com.blockchain.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.support.annotation.DrawableRes
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import piuk.blockchain.androidcoreui.utils.AndroidUtils

class NotificationsUtil(
    private val context: Context,
    private val notificationManager: NotificationManager
) {

    fun triggerNotification(
        title: String,
        marquee: String,
        text: String,
        @DrawableRes icon: Int,
        pendingIntent: PendingIntent,
        id: Int
    ) {

        val builder = NotificationCompat.Builder(
            context,
            NOTIFICATION_CHANNEL_ID
        ).setSmallIcon(icon)
            .setColor(ContextCompat.getColor(context, R.color.primary_navy_medium))
            .setContentTitle(title)
            .setContentIntent(pendingIntent)
            .setWhen(System.currentTimeMillis())
            .setSound(Uri.parse("android.resource://${context.packageName}/${R.raw.beep}"))
            .setTicker(marquee)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .predicateBuilder(
                { AndroidUtils.is19orHigher() },
                { setVibrate(longArrayOf(100)) },
                // Vibration requires PERMISSION_VIBRATE on <=4.3 due to AOSP bug, set to empty
                { setVibrate(longArrayOf()) }
            )
            .setDefaults(Notification.DEFAULT_LIGHTS)
            .setContentText(text)

        if (AndroidUtils.is26orHigher()) {
            // TODO: Maybe pass in specific channel names here, such as "payments" and "contacts"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.app_name),
                importance
            ).apply {
                enableLights(true)
                lightColor = ContextCompat.getColor(context, R.color.primary_navy_medium)
                enableVibration(true)
                vibrationPattern = longArrayOf(100)
            }
            notificationManager.createNotificationChannel(notificationChannel)
        }

        notificationManager.notify(id, builder.build())
    }

    private fun NotificationCompat.Builder.predicateBuilder(
        predicate: () -> Boolean,
        trueFunc: NotificationCompat.Builder.() -> NotificationCompat.Builder,
        falseFunc: NotificationCompat.Builder.() -> NotificationCompat.Builder
    ): NotificationCompat.Builder = if (predicate()) this.trueFunc() else this.falseFunc()

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "group_01"
    }
}
