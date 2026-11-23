package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.DueRevisionItem
import com.example.data.local.MilestoneStatus
import com.example.data.repository.CompletionOutcome
import com.example.domain.RevisionStage
import com.example.i18n.AppLang
import com.example.i18n.AppStrings
import com.example.ui.components.FrostedBackground
import com.example.ui.components.FullScreenPhotoDialog
import com.example.ui.components.GiantActionButton
import com.example.ui.components.GlassCard
import com.example.ui.components.ReviseTopBar
import com.example.ui.components.stageIconFor
import com.example.ui.theme.LocalGlassPalette
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CheckRevisionScreen(
    dueQueue: List<DueRevisionItem>,
    effectiveNowMillis: Long,
    strings: AppStrings,
    lang: AppLang,
    isStudyingFocusMode: Boolean,
    hapticEnabled: Boolean,
    playingAudioPath: String?,
    completionOutcome: CompletionOutcome?,
    scopedNoteTitle: String? = null,
    onBack: () -> Unit,
    onToggleLang: () -> Unit,
    onToggleFocusMode: () -> Unit,
    onCompleteMilestone: (Long) -> Unit,
    onDismissCompletion: () -> Unit,
    onTogglePlayAudio: (String) -> Unit
) {
    val glass = LocalGlassPalette.current
    val dateFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    var zoomedPhotoPath by remember { mutableStateOf<String?>(null) }

    // Group strictly in serial order (dueQueue is already sorted by stageIndex 0..15, then oldest dueAt)
    val groupedByStage = remember(dueQueue) {
        dueQueue.groupBy { it.milestone.stageIndex }
    }
    val hasMissed = remember(dueQueue, effectiveNowMillis) {
        dueQueue.any { it.milestone.status == MilestoneStatus.MISSED || effectiveNowMillis > it.milestone.dueAt }
    }

    val topTitle = if (!scopedNoteTitle.isNullOrBlank()) {
        "${strings.dueQueueTitle} • $scopedNoteTitle"
    } else {
        strings.dueQueueTitle
    }

    FrostedBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ReviseTopBar(
                    title = topTitle,
                    strings = strings,
                    onBackClick = onBack,
                    onToggleLang = onToggleLang,
                    isStudyingFocusMode = isStudyingFocusMode,
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
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Serial order hint & compassionate missed banner
                    item {
                        GlassCard(contentPadding = PaddingValues(14.dp)) {
                            Text(
                                text = strings.serialOrderHint,
                                style = MaterialTheme.typography.labelLarge,
                                color = glass.primaryAccent
                            )
                            Text(
                                text = strings.oldestFirstHint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = glass.inkSecondary
                            )
                            if (hasMissed) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = glass.missedContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Favorite,
                                            contentDescription = null,
                                            tint = glass.missedColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = strings.missedCompassionateBanner,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = glass.inkPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (dueQueue.isEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 26.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(glass.doneContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = strings.allCaughtUp,
                                            tint = glass.doneColor,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                    Text(
                                        text = strings.dueQueueEmptyTitle,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = glass.inkPrimary
                                    )
                                    Text(
                                        text = strings.dueQueueEmptySubtitle,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = glass.inkSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        groupedByStage.forEach { (stageIdx, stageItems) ->
                            val stage = RevisionStage.fromIndex(stageIdx)
                            item(key = "stage_header_$stageIdx") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(glass.primaryAccent.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = stageIconFor(stageIdx),
                                            contentDescription = stage.label(lang),
                                            tint = glass.primaryAccent,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = stage.label(lang),
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = glass.inkPrimary
                                        )
                                        Text(
                                            text = "${stageItems.size}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = glass.inkSecondary
                                        )
                                    }
                                }
                            }

                            items(stageItems, key = { it.milestone.id }) { item ->
                                val isMissed = item.milestone.status == MilestoneStatus.MISSED ||
                                    effectiveNowMillis > item.milestone.dueAt
                                val statusColor = if (isMissed) glass.missedColor else glass.dueColor
                                val statusBg = if (isMissed) glass.missedContainer else glass.dueContainer

                                GlassCard(
                                    tintColor = glass.noteTints.getOrElse(item.note.colorIndex) { glass.glassSurface },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("due_item_card_${item.milestone.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.note.title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = glass.primaryAccent
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = statusBg
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Alarm,
                                                    contentDescription = null,
                                                    tint = statusColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = if (isMissed) strings.missedBadge else strings.dueBadge,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = statusColor
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = strings.dueAtLabel(dateFormatter.format(Date(item.milestone.dueAt))),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = glass.inkSecondary
                                    )

                                    if (item.entry.contentText.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = item.entry.contentText,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = glass.inkPrimary
                                        )
                                    }

                                    val photoPath = item.entry.photoPath
                                    if (!photoPath.isNullOrBlank() && File(photoPath).exists()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable { zoomedPhotoPath = photoPath }
                                                .testTag("revision_item_photo_${item.entry.id}")
                                        ) {
                                            AsyncImage(
                                                model = File(photoPath),
                                                contentDescription = strings.addPhotoOptional,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            Surface(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(8.dp),
                                                shape = CircleShape,
                                                color = Color.Black.copy(alpha = 0.55f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.ZoomIn,
                                                    contentDescription = strings.zoomPhotoHint,
                                                    tint = Color.White,
                                                    modifier = Modifier.padding(6.dp).size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    val audioPath = item.entry.audioPath
                                    if (!audioPath.isNullOrBlank() && File(audioPath).exists()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        val isPlaying = playingAudioPath == audioPath
                                        Surface(
                                            modifier = Modifier
                                                .heightIn(min = 50.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable { onTogglePlayAudio(item.entry.audioPath) },
                                            shape = RoundedCornerShape(16.dp),
                                            color = glass.doneContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                                    contentDescription = strings.playVoice,
                                                    tint = glass.doneColor
                                                )
                                                Icon(
                                                    imageVector = Icons.Rounded.GraphicEq,
                                                    contentDescription = null,
                                                    tint = glass.doneColor
                                                )
                                                Text(
                                                    text = if (isPlaying) strings.stopVoice else strings.playVoice,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = glass.inkPrimary
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    GiantActionButton(
                                        text = strings.revisionComplete,
                                        icon = Icons.Rounded.CheckCircle,
                                        containerColor = statusColor,
                                        hapticEnabled = hapticEnabled,
                                        minHeight = 88.dp,
                                        testTag = "revision_complete_button_${item.milestone.id}",
                                        onClick = { onCompleteMilestone(item.milestone.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Gentle Completion Celebration Overlay
                AnimatedVisibility(
                    visible = completionOutcome != null,
                    enter = fadeIn() + scaleIn(
                        initialScale = 0.88f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    ),
                    exit = fadeOut()
                ) {
                    if (completionOutcome != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable(onClick = onDismissCompletion)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassCard(
                                cornerRadius = 32.dp,
                                modifier = Modifier
                                    .widthIn(max = 440.dp)
                                    .testTag("completion_celebration_card")
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(glass.doneContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = glass.doneColor,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                    Text(
                                        text = strings.completedCelebrationTitle,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = glass.inkPrimary
                                    )
                                    Text(
                                        text = completionOutcome.message,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = glass.inkSecondary
                                    )
                                    if (completionOutcome.unlockedLetterTitles.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = glass.dueContainer
                                        ) {
                                            Text(
                                                text = "✉ ${completionOutcome.unlockedLetterTitles.joinToString(", ")}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = glass.dueColor,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                    }
                                    GiantActionButton(
                                        text = strings.doneBadge,
                                        icon = Icons.Rounded.CheckCircle,
                                        containerColor = glass.doneColor,
                                        minHeight = 68.dp,
                                        onClick = onDismissCompletion
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (zoomedPhotoPath != null) {
        FullScreenPhotoDialog(
            photoPath = zoomedPhotoPath!!,
            strings = strings,
            onDismiss = { zoomedPhotoPath = null }
        )
    }
}
