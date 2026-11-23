package com.example.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.MilestoneStatus
import com.example.data.repository.ReviseKeepRepository
import com.example.domain.RevisionEngine
import java.util.concurrent.TimeUnit

class RevisionCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = ReviseKeepRepository(applicationContext)
        repo.sweepOverdueMilestones()

        val settings = repo.prefs.getSettingsSync()
        val realNow = System.currentTimeMillis()
        val effectiveNow = repo.effectiveNowMillis()

        // Respect "I am studying now" focus mode
        if (settings.isStudyingFocusMode) return Result.success()

        // Respect Snooze
        if (settings.snoozedUntilMillis > realNow) return Result.success()

        // Respect Quiet Hours (default 10 PM to 7 AM)
        if (RevisionEngine.isWithinQuietHours(realNow, settings.quietStartHour, settings.quietEndHour)) {
            return Result.success()
        }

        // Respect Daily Cap (default 5)
        val todayKey = RevisionEngine.dayKey(realNow)
        val sentToday = if (settings.notificationsSentDateKey == todayKey) {
            settings.notificationsSentToday
        } else {
            0
        }
        if (sentToday >= settings.dailyNotificationCap) return Result.success()

        val dueItems = repo.getAllDueItemsSync()
        if (dueItems.isEmpty()) return Result.success()

        // Filter missed items that already reached max 2 reminders
        val eligibleItems = dueItems.filter { item ->
            val isMissed = item.milestone.status == MilestoneStatus.MISSED || effectiveNow > item.milestone.dueAt
            if (isMissed) item.milestone.missedReminderCount < 2 else true
        }
        if (eligibleItems.isEmpty()) return Result.success()

        val (title, body) = repo.generateSmartNotificationText(eligibleItems)
        NotificationHelper.showMergedRevisionNotification(
            context = applicationContext,
            title = title,
            body = body,
            lang = settings.language
        )

        repo.prefs.recordNotificationSent(todayKey)
        val missedToIncrement = eligibleItems
            .map { it.milestone }
            .filter { it.status == MilestoneStatus.MISSED || effectiveNow > it.dueAt }
        repo.markMissedItemsNotified(missedToIncrement)

        return Result.success()
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "revisekeep_periodic_check"

        fun schedulePeriodic(context: Context, throttled: Boolean) {
            val intervalMinutes = if (throttled) 120L else 30L
            val request = PeriodicWorkRequestBuilder<RevisionCheckWorker>(
                intervalMinutes,
                TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun triggerImmediateCheck(context: Context) {
            val request = OneTimeWorkRequestBuilder<RevisionCheckWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
