package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.prefs.AccentPreset
import com.example.data.prefs.AppSettings
import com.example.data.prefs.ThemeMode
import com.example.i18n.AppStrings
import com.example.ui.components.FrostedBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.ReviseTopBar
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.LocalGlassPalette
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LookAndFeelScreen(
    settings: AppSettings,
    strings: AppStrings,
    onBack: () -> Unit,
    onToggleLang: () -> Unit,
    onToggleFocusMode: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetAccentPreset: (AccentPreset) -> Unit,
    onSetFontScale: (Float) -> Unit,
    onSetSoundFeedback: (Boolean) -> Unit,
    onSetHapticFeedback: (Boolean) -> Unit,
    onUnlockDeveloperMenu: () -> Unit,
    onSetDeveloperOffset: (Long) -> Unit
) {
    val glass = LocalGlassPalette.current
    var versionTapCount by remember { mutableIntStateOf(0) }

    FrostedBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ReviseTopBar(
                    title = strings.lookAndFeel,
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
                    // Theme Mode
                    GlassCard {
                        Text(
                            text = strings.themeModeLabel,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = glass.inkPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(
                                ThemeMode.SYSTEM to strings.themeSystem,
                                ThemeMode.LIGHT to strings.themeLight,
                                ThemeMode.DARK to strings.themeDark
                            ).forEach { (mode, label) ->
                                FilterChip(
                                    selected = settings.themeMode == mode,
                                    onClick = { onSetThemeMode(mode) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("theme_chip_${mode.name}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = strings.accentColorLabel,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = glass.inkPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            val accentColors = listOf(
                                AccentPreset.MIDNIGHT_TEAL to GlassTokens.IndigoPrimary,
                                AccentPreset.OCEAN_SAPPHIRE to GlassTokens.SapphirePrimary,
                                AccentPreset.WARM_DAWN to GlassTokens.DawnPrimary,
                                AccentPreset.SOFT_EMERALD to GlassTokens.EmeraldPrimary
                            )
                            accentColors.forEach { (preset, swatch) ->
                                val selected = settings.accentPreset == preset
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(swatch)
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) Color.White else glass.glassBorder,
                                            shape = CircleShape
                                        )
                                        .testTag("accent_${preset.key}")
                                        .clickable { onSetAccentPreset(preset) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "${strings.fontSizeLabel} (${(settings.fontScale * 100).toInt()}%)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = glass.inkPrimary
                        )
                        Slider(
                            value = settings.fontScale,
                            onValueChange = onSetFontScale,
                            valueRange = 0.85f..1.35f
                        )
                    }

                    // Sensory feedback
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(strings.soundFeedbackLabel, style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = settings.soundFeedbackEnabled,
                                onCheckedChange = onSetSoundFeedback
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(strings.hapticFeedbackLabel, style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = settings.hapticFeedbackEnabled,
                                onCheckedChange = onSetHapticFeedback
                            )
                        }
                    }

                    // Hidden Developer Time-Travel Switch (5 taps on version footer unlocks)
                    if (settings.developerMenuUnlocked) {
                        GlassCard(tintColor = glass.dueContainer) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FastForward,
                                    contentDescription = strings.devTimeTravelTitle,
                                    tint = glass.dueColor
                                )
                                Text(
                                    text = strings.devTimeTravelTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = glass.inkPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strings.devTimeTravelSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = glass.inkSecondary
                            )
                            if (settings.developerTimeOffsetMillis > 0L) {
                                Spacer(modifier = Modifier.height(6.dp))
                                val hours = TimeUnit.MILLISECONDS.toHours(settings.developerTimeOffsetMillis)
                                val label = if (hours >= 24) "${hours / 24}d" else "${hours}h"
                                Text(
                                    text = strings.devOffsetActive(label),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = glass.dueColor
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            val jumps = listOf(
                                "+2h" to TimeUnit.HOURS.toMillis(2),
                                "+24h" to TimeUnit.HOURS.toMillis(24),
                                "+3d" to TimeUnit.DAYS.toMillis(3),
                                "+7d" to TimeUnit.DAYS.toMillis(7),
                                "+1m" to TimeUnit.DAYS.toMillis(31),
                                "+2m" to TimeUnit.DAYS.toMillis(62),
                                "+6m" to TimeUnit.DAYS.toMillis(184),
                                "+12m" to TimeUnit.DAYS.toMillis(366)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                jumps.forEach { (label, delta) ->
                                    OutlinedButton(
                                        onClick = {
                                            onSetDeveloperOffset(settings.developerTimeOffsetMillis + delta)
                                        },
                                        border = BorderStroke(1.dp, glass.dueColor.copy(alpha = 0.5f)),
                                        modifier = Modifier.testTag("dev_jump_$label")
                                    ) {
                                        Text(label)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { onSetDeveloperOffset(0L) },
                                modifier = Modifier.testTag("dev_reset_time")
                            ) {
                                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(strings.devResetTime)
                            }
                        }
                    }

                    // Version tap footer to reveal Developer Time Travel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .testTag("version_dev_unlock_footer")
                            .clickable {
                                versionTapCount++
                                if (versionTapCount >= 5) {
                                    onUnlockDeveloperMenu()
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ReviseKeep v1.0",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.inkSecondary
                        )
                    }
                }
            }
        }
    }
}
