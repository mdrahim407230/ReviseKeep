package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.prefs.AppPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val prefs = AppPreferencesRepository(appContext)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    NotificationHelper.ACTION_SNOOZE -> {
                        val snoozeUntil = System.currentTimeMillis() + 30L * 60L * 1000L
                        prefs.setSnoozedUntil(snoozeUntil)
                        NotificationHelper.cancelRevisionNotification(appContext)
                    }
                    NotificationHelper.ACTION_FOCUS_MODE -> {
                        prefs.setStudyingFocusMode(true)
                        NotificationHelper.cancelRevisionNotification(appContext)
                    }
                    NotificationHelper.ACTION_DISMISSED -> {
                        prefs.incrementConsecutiveDismissals()
                        val updated = prefs.getSettingsSync()
                        if (updated.adaptiveThrottled) {
                            RevisionCheckWorker.schedulePeriodic(appContext, throttled = true)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
