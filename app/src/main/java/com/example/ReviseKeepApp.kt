package com.example

import android.app.Application
import com.example.data.repository.ReviseKeepRepository
import com.example.notifications.NotificationHelper
import com.example.notifications.RevisionCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReviseKeepApp : Application() {
    lateinit var repository: ReviseKeepRepository
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        repository = ReviseKeepRepository(this)
        NotificationHelper.ensureChannel(this)
        appScope.launch {
            repository.sweepOverdueMilestones()
            val settings = repository.prefs.getSettingsSync()
            RevisionCheckWorker.schedulePeriodic(
                context = this@ReviseKeepApp,
                throttled = settings.adaptiveThrottled
            )
        }
    }
}
