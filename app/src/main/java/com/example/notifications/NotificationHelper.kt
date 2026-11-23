package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.i18n.AppLang
import com.example.i18n.StringsBnEn

object NotificationHelper {
    const val CHANNEL_ID = "revisekeep_reminders"
    const val NOTIFICATION_ID = 1001
    const val EXTRA_OPEN_CHECK_REVISION = "open_check_revision"

    const val ACTION_SNOOZE = "com.aistudio.revisekeep.kzxqpm.ACTION_SNOOZE"
    const val ACTION_FOCUS_MODE = "com.aistudio.revisekeep.kzxqpm.ACTION_FOCUS_MODE"
    const val ACTION_DISMISSED = "com.aistudio.revisekeep.kzxqpm.ACTION_DISMISSED"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val desc = context.getString(R.string.notification_channel_desc)
            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = desc
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showMergedRevisionNotification(
        context: Context,
        title: String,
        body: String,
        lang: AppLang
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannel(context)

        val strings = StringsBnEn.forLang(lang)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_CHECK_REVISION, true)
        }
        val openPending = PendingIntent.getActivity(
            context,
            10,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
        }
        val snoozePending = PendingIntent.getBroadcast(
            context,
            11,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val focusIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_FOCUS_MODE
        }
        val focusPending = PendingIntent.getBroadcast(
            context,
            12,
            focusIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DISMISSED
        }
        val dismissPending = PendingIntent.getBroadcast(
            context,
            13,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openPending)
            .setDeleteIntent(dismissPending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .addAction(0, strings.snoozeAction, snoozePending)
            .addAction(0, strings.studyingNow, focusPending)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    fun cancelRevisionNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
