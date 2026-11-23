package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.prefs.AppSettings
import com.example.i18n.AppStrings
import com.example.notifications.NotificationHelper
import com.example.ui.components.FrostedBackground
import com.example.ui.components.GiantActionButton
import com.example.ui.components.GlassCard
import com.example.ui.components.ReviseTopBar
import com.example.ui.theme.LocalGlassPalette
import kotlin.math.roundToInt

@Composable
fun NotificationSettingsScreen(
    settings: AppSettings,
    strings: AppStrings,
    onBack: () -> Unit,
    onToggleLang: () -> Unit,
    onToggleFocusMode: () -> Unit,
    onSetDailyCap: (Int) -> Unit,
    onSetQuietHours: (Int, Int) -> Unit,
    onClearSnooze: () -> Unit,
    onResetAdaptive: () -> Unit,
    onTestCheckNow: () -> Unit
) {
    val context = LocalContext.current
    val glass = LocalGlassPalette.current
    var hasNotifPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
    }

    FrostedBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ReviseTopBar(
                    title = strings.notificationSettings,
                    strings = strings,
                    onBackClick = onBack,
                    onToggleLang = onToggleLang,
                    isStudyingFocusMode = settings.isStudyingFocusMode,
                    onToggleFocusMode = onToggleFocusMode
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        GlassCard(tintColor = glass.dueContainer) {
                            Text(
                                text = strings.notificationPermissionTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = glass.inkPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            GiantActionButton(
                                text = strings.notificationPermissionBtn,
                                icon = Icons.Rounded.NotificationsActive,
                                containerColor = glass.dueColor,
                                minHeight = 76.dp,
                                testTag = "request_notification_permission",
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            )
                        }
                    }

                    // Adaptive Throttling Notice (after 3 dismissals in a row)
                    if (settings.adaptiveThrottled) {
                        GlassCard(tintColor = glass.dueContainer) {
                            Text(
                                text = strings.adaptiveThrottledNotice,
                                style = MaterialTheme.typography.bodyLarge,
                                color = glass.inkPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(onClick = onResetAdaptive) {
                                Text(strings.resetAdaptiveBtn)
                            }
                        }
                    }

                    // Active Snooze indicator
                    if (settings.snoozedUntilMillis > System.currentTimeMillis()) {
                        GlassCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.snoozedNotice, modifier = Modifier.weight(1f))
                                Button(onClick = onClearSnooze) {
                                    Text(strings.clearSnooze)
                                }
                            }
                        }
                    }

                    // Focus Mode ("I am studying now")
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SelfImprovement,
                                    contentDescription = strings.studyingNow,
                                    tint = glass.doneColor
                                )
                                Text(
                                    text = strings.studyingNow,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = glass.inkPrimary
                                )
                            }
                            Switch(
                                checked = settings.isStudyingFocusMode,
                                onCheckedChange = { onToggleFocusMode() }
                            )
                        }
                    }

                    // Daily Cap & Quiet Hours
                    GlassCard {
                        Text(
                            text = strings.dailyCapLabel(settings.dailyNotificationCap),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = glass.inkPrimary
                        )
                        Slider(
                            value = settings.dailyNotificationCap.toFloat(),
                            onValueChange = { onSetDailyCap(it.roundToInt()) },
                            valueRange = 1f..15f,
                            steps = 13,
                            modifier = Modifier.testTag("daily_cap_slider")
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = strings.quietHoursLabel,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = glass.inkPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.quietStartLabel(settings.quietStartHour),
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.inkSecondary
                        )
                        Slider(
                            value = settings.quietStartHour.toFloat(),
                            onValueChange = { onSetQuietHours(it.roundToInt(), settings.quietEndHour) },
                            valueRange = 0f..23f,
                            steps = 22
                        )
                        Text(
                            text = strings.quietEndLabel(settings.quietEndHour),
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.inkSecondary
                        )
                        Slider(
                            value = settings.quietEndHour.toFloat(),
                            onValueChange = { onSetQuietHours(settings.quietStartHour, it.roundToInt()) },
                            valueRange = 0f..23f,
                            steps = 22
                        )
                    }

                    // Honest Android background notification note + manual check button
                    GlassCard {
                        Text(
                            text = strings.notificationHonestNote,
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.inkSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.Transparent) {
                            GiantActionButton(
                                text = strings.checkRevision,
                                icon = Icons.Rounded.NotificationsActive,
                                containerColor = glass.primaryAccent,
                                minHeight = 72.dp,
                                onClick = onTestCheckNow
                            )
                        }
                    }
                }
            }
        }
    }
}
