package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notifications.NotificationHelper
import com.example.ui.navigation.ReviseKeepRoot
import com.example.ui.theme.ReviseKeepTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val openCheckRevision = intent?.getBooleanExtra(NotificationHelper.EXTRA_OPEN_CHECK_REVISION, false) == true

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val effectiveSettings = settings ?: com.example.data.prefs.AppSettings()
            ReviseKeepTheme(
                themeMode = effectiveSettings.themeMode,
                accentPreset = effectiveSettings.accentPreset,
                fontScale = effectiveSettings.fontScale
            ) {
                ReviseKeepRoot(
                    viewModel = viewModel,
                    openCheckRevisionOnLaunch = openCheckRevision
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.sweepOverdueOnResume()
    }

    override fun onStop() {
        super.onStop()
        viewModel.syncInBackgroundOnStop()
    }
}
