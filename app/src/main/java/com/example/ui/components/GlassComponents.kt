package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.MilestoneStatus
import com.example.domain.RevisionStage
import com.example.i18n.AppLang
import com.example.i18n.AppStrings
import com.example.ui.theme.LocalGlassPalette

@Composable
fun FrostedBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val glass = LocalGlassPalette.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(glass.canvasTop, glass.canvasMid, glass.canvasBottom)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glass.primaryAccent.copy(alpha = if (glass.isDark) 0.28f else 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.15f, h * 0.12f),
                    radius = w * 0.72f
                ),
                radius = w * 0.72f,
                center = Offset(w * 0.15f, h * 0.12f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glass.secondaryAccent.copy(alpha = if (glass.isDark) 0.24f else 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.88f, h * 0.42f),
                    radius = w * 0.68f
                ),
                radius = w * 0.68f,
                center = Offset(w * 0.88f, h * 0.42f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glass.primaryAccent.copy(alpha = if (glass.isDark) 0.18f else 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.35f, h * 0.88f),
                    radius = w * 0.7f
                ),
                radius = w * 0.7f,
                center = Offset(w * 0.35f, h * 0.88f)
            )
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    tintColor: Color? = null,
    cornerRadius: Dp = 26.dp,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LocalGlassPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "glassCardScale"
    )
    val baseFill = tintColor ?: glass.glassSurface
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            glass.glassBorder,
            glass.glassBorder.copy(alpha = 0.18f)
        )
    )
    val shape = RoundedCornerShape(cornerRadius)

    Surface(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = baseFill,
        border = BorderStroke(1.dp, borderBrush),
        tonalElevation = if (glass.isDark) 2.dp else 4.dp,
        shadowElevation = if (glass.isDark) 6.dp else 4.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (glass.isDark) 0.08f else 0.32f),
                            Color.Transparent
                        )
                    )
                )
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun GiantActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    containerColor: Color? = null,
    contentColor: Color = Color.White,
    hapticEnabled: Boolean = true,
    minHeight: Dp = 88.dp,
    testTag: String = "giant_action_button"
) {
    val glass = LocalGlassPalette.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "giantButtonScale"
    )
    val bg = containerColor ?: glass.primaryAccent
    val shape = RoundedCornerShape(28.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .scale(scale)
            .testTag(testTag)
            .clip(shape)
            .clickable(interactionSource = interactionSource, indication = null) {
                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = shape,
        color = bg,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            bg,
                            bg.copy(alpha = 0.82f),
                            glass.secondaryAccent.copy(alpha = 0.55f)
                        )
                    )
                )
                .padding(horizontal = 22.dp, vertical = 18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = text,
                        tint = contentColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = contentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.9f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReviseTopBar(
    title: String,
    strings: AppStrings,
    onMenuClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    onToggleLang: () -> Unit,
    isStudyingFocusMode: Boolean,
    onToggleFocusMode: () -> Unit,
) {
    val glass = LocalGlassPalette.current
    val focusBg by animateColorAsState(
        targetValue = if (isStudyingFocusMode) glass.doneColor else glass.glassSurface,
        label = "focusPillBg"
    )
    val focusInk = if (isStudyingFocusMode) Color.White else glass.inkPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (onBackClick != null) {
            Surface(
                modifier = Modifier
                    .size(52.dp)
                    .testTag("back_button"),
                shape = CircleShape,
                color = glass.glassSurface,
                border = BorderStroke(1.dp, glass.glassBorder)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = glass.inkPrimary
                    )
                }
            }
        } else if (onMenuClick != null) {
            Surface(
                modifier = Modifier
                    .size(52.dp)
                    .testTag("hamburger_menu_button"),
                shape = CircleShape,
                color = glass.glassSurface,
                border = BorderStroke(1.dp, glass.glassBorder)
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = "Menu",
                        tint = glass.inkPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = glass.inkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Focus Mode Pill ("I am studying now")
        Surface(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .heightIn(min = 48.dp)
                .testTag("focus_mode_toggle")
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onToggleFocusMode),
            shape = RoundedCornerShape(24.dp),
            color = focusBg,
            border = BorderStroke(1.dp, glass.glassBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SelfImprovement,
                    contentDescription = strings.studyingNow,
                    tint = focusInk,
                    modifier = Modifier.size(20.dp)
                )
                if (isStudyingFocusMode) {
                    Text(
                        text = "ON",
                        style = MaterialTheme.typography.labelLarge,
                        color = focusInk
                    )
                }
            }
        }

        // Instant Language Toggle (বাংলা / EN)
        Surface(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .heightIn(min = 48.dp)
                .testTag("language_toggle_button")
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onToggleLang),
            shape = RoundedCornerShape(24.dp),
            color = glass.glassElevated,
            border = BorderStroke(1.dp, glass.glassBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Translate,
                    contentDescription = strings.languageLabel,
                    tint = glass.primaryAccent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = strings.langToggleChip,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = glass.inkPrimary
                )
            }
        }
    }
}

fun stageIconFor(stageIndex: Int): ImageVector = when (stageIndex) {
    0 -> Icons.Rounded.Bolt
    1 -> Icons.Rounded.WbSunny
    2 -> Icons.Rounded.Today
    3 -> Icons.Rounded.DateRange
    in 4..6 -> Icons.Rounded.CalendarMonth
    in 7..11 -> Icons.Rounded.EventAvailable
    else -> Icons.Rounded.AutoAwesome
}

@Composable
fun StageBadge(
    stageIndex: Int,
    status: MilestoneStatus,
    lang: AppLang,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassPalette.current
    val stage = RevisionStage.fromIndex(stageIndex)
    val (bg, fg, statusIcon) = when (status) {
        MilestoneStatus.COMPLETED -> Triple(glass.doneContainer, glass.doneColor, Icons.Rounded.CheckCircle)
        MilestoneStatus.ACTIVE_DUE -> Triple(glass.dueContainer, glass.dueColor, Icons.Rounded.Schedule)
        MilestoneStatus.MISSED -> Triple(glass.missedContainer, glass.missedColor, Icons.Rounded.Alarm)
        MilestoneStatus.LOCKED -> Triple(glass.glassSurface, glass.inkSecondary, Icons.Rounded.HourglassTop)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = stageIconFor(stageIndex),
            contentDescription = stage.label(lang),
            tint = fg,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = stage.label(lang),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = fg
        )
        Icon(
            imageVector = statusIcon,
            contentDescription = when (status) {
                MilestoneStatus.COMPLETED -> strings.doneBadge
                MilestoneStatus.ACTIVE_DUE -> strings.dueBadge
                MilestoneStatus.MISSED -> strings.missedBadge
                MilestoneStatus.LOCKED -> strings.lockedBadge
            },
            tint = fg,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun VoiceMemoRecorderBar(
    audioPath: String?,
    isRecording: Boolean,
    isPlaying: Boolean,
    strings: AppStrings,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onTogglePlay: (String) -> Unit,
    onClearAudio: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val glass = LocalGlassPalette.current
    var permissionDeniedNote by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedNote = false
            onStartRecord()
        } else {
            permissionDeniedNote = true
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        if (isRecording) {
                            onStopRecord()
                        } else {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                permissionDeniedNote = false
                                onStartRecord()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                shape = RoundedCornerShape(18.dp),
                color = if (isRecording) glass.missedContainer else glass.glassElevated,
                border = BorderStroke(
                    1.dp,
                    if (isRecording) glass.missedColor else glass.glassBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                        contentDescription = if (isRecording) strings.stopRecording else strings.recordVoiceOptional,
                        tint = if (isRecording) glass.missedColor else glass.primaryAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (isRecording) strings.stopRecording else strings.recordVoiceOptional,
                        style = MaterialTheme.typography.labelLarge,
                        color = glass.inkPrimary
                    )
                }
            }

            if (!audioPath.isNullOrBlank() && !isRecording) {
                Surface(
                    modifier = Modifier
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onTogglePlay(audioPath) },
                    shape = RoundedCornerShape(18.dp),
                    color = glass.doneContainer,
                    border = BorderStroke(1.dp, glass.doneColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) strings.stopVoice else strings.playVoice,
                            tint = glass.doneColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = glass.doneColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isPlaying) strings.stopVoice else strings.playVoice,
                            style = MaterialTheme.typography.labelLarge,
                            color = glass.inkPrimary
                        )
                    }
                }
                if (onClearAudio != null) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = glass.missedContainer,
                        border = BorderStroke(1.dp, glass.missedColor.copy(alpha = 0.45f))
                    ) {
                        IconButton(onClick = onClearAudio) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = strings.deleteNote,
                                tint = glass.missedColor
                            )
                        }
                    }
                }
            }
        }
        if (permissionDeniedNote) {
            Text(
                text = strings.micPermissionNeeded,
                style = MaterialTheme.typography.bodyMedium,
                color = glass.missedColor
            )
        }
    }
}

@Composable
fun FullScreenPhotoDialog(
    photoPath: String,
    strings: AppStrings,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = nextScale
        offset = if (nextScale > 1.01f) {
            offset + panChange
        } else {
            Offset.Zero
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1.2f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        }
                    )
                }
        ) {
            AsyncImage(
                model = File(photoPath),
                contentDescription = strings.addPhotoOptional,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state = transformState)
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(52.dp)
                    .testTag("close_fullscreen_photo"),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = strings.closePhoto,
                        tint = Color.White
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Text(
                    text = strings.zoomPhotoHint,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}
