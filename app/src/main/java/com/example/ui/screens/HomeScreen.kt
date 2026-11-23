package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.NoteCardSummary
import com.example.data.local.NoteEntity
import com.example.data.local.WhyProfileEntity
import com.example.i18n.AppLang
import com.example.i18n.AppStrings
import com.example.ui.components.FrostedBackground
import com.example.ui.components.FullScreenPhotoDialog
import com.example.ui.components.GiantActionButton
import com.example.ui.components.GlassCard
import com.example.ui.components.ReviseTopBar
import com.example.ui.theme.LocalGlassPalette
import java.io.File

@Composable
fun HomeScreen(
    strings: AppStrings,
    lang: AppLang,
    whyProfile: WhyProfileEntity?,
    noteCards: List<NoteCardSummary>,
    totalDueCount: Int,
    totalMissedCount: Int,
    streakDays: Int,
    isStudyingFocusMode: Boolean,
    hapticEnabled: Boolean,
    playingAudioPath: String?,
    onMenuClick: () -> Unit,
    onToggleLang: () -> Unit,
    onToggleFocusMode: () -> Unit,
    onOpenCheckRevision: () -> Unit,
    onOpenMyWhy: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onCreateNote: (title: String, subjectTag: String, colorIndex: Int) -> Unit,
    onUpdateNote: (NoteEntity) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onTogglePlayAudio: (String) -> Unit
) {
    val glass = LocalGlassPalette.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var zoomedPhotoPath by remember { mutableStateOf<String?>(null) }

    FrostedBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ReviseTopBar(
                    title = strings.appName,
                    strings = strings,
                    onMenuClick = onMenuClick,
                    onToggleLang = onToggleLang,
                    isStudyingFocusMode = isStudyingFocusMode,
                    onToggleFocusMode = onToggleFocusMode
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .size(72.dp)
                        .testTag("add_note_fab"),
                    shape = CircleShape,
                    containerColor = glass.primaryAccent,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = strings.newNote,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 165.dp),
                    modifier = Modifier
                        .widthIn(max = 900.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 112.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Pinned "My Why" Hero Card
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        PinnedMyWhyCard(
                            strings = strings,
                            whyProfile = whyProfile,
                            streakDays = streakDays,
                            playingAudioPath = playingAudioPath,
                            onClick = onOpenMyWhy,
                            onPhotoClick = { path -> zoomedPhotoPath = path },
                            onTogglePlayAudio = onTogglePlayAudio
                        )
                    }

                    // 2. Giant "Check Revision" Button
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        val buttonColor = when {
                            totalMissedCount > 0 -> glass.missedColor
                            totalDueCount > 0 -> glass.dueColor
                            else -> glass.doneColor
                        }
                        val subtitle = when {
                            totalMissedCount > 0 -> "${strings.dueNowCount(totalDueCount)} • ${strings.missedCount(totalMissedCount)}"
                            totalDueCount > 0 -> strings.dueNowCount(totalDueCount)
                            else -> strings.allCaughtUp
                        }
                        val icon = when {
                            totalDueCount > 0 -> Icons.Rounded.Schedule
                            else -> Icons.Rounded.CheckCircle
                        }
                        GiantActionButton(
                            text = strings.checkRevision,
                            subtitle = subtitle,
                            icon = icon,
                            containerColor = buttonColor,
                            hapticEnabled = hapticEnabled,
                            minHeight = 92.dp,
                            testTag = "check_revision_button",
                            onClick = onOpenCheckRevision
                        )
                    }

                    // Section header
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = strings.allNotes,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = glass.inkPrimary,
                            modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                        )
                    }

                    // Empty state or Note Cards
                    if (noteCards.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            GlassCard(
                                onClick = { showCreateDialog = true },
                                modifier = Modifier.fillMaxWidth().testTag("empty_notes_card")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                        contentDescription = null,
                                        tint = glass.primaryAccent,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = strings.emptyNotesTitle,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = glass.inkPrimary
                                    )
                                    Text(
                                        text = strings.emptyNotesTip,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = glass.inkSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        items(noteCards, key = { it.note.id }) { summary ->
                            NoteGridCard(
                                summary = summary,
                                strings = strings,
                                onOpen = { onOpenNote(summary.note.id) },
                                onTogglePin = {
                                    onUpdateNote(summary.note.copy(isPinned = !summary.note.isPinned))
                                },
                                onDelete = { onDeleteNote(summary.note.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateNoteDialog(
            strings = strings,
            onDismiss = { showCreateDialog = false },
            onCreate = { title, tag, colorIdx ->
                showCreateDialog = false
                onCreateNote(title, tag, colorIdx)
            }
        )
    }
    if (zoomedPhotoPath != null) {
        FullScreenPhotoDialog(
            photoPath = zoomedPhotoPath!!,
            strings = strings,
            onDismiss = { zoomedPhotoPath = null }
        )
    }
}

@Composable
private fun PinnedMyWhyCard(
    strings: AppStrings,
    whyProfile: WhyProfileEntity?,
    streakDays: Int,
    playingAudioPath: String?,
    onClick: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onTogglePlayAudio: (String) -> Unit
) {
    val glass = LocalGlassPalette.current
    GlassCard(
        onClick = onClick,
        cornerRadius = 28.dp,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pinned_my_why_card")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_why_horizon_1795323310321),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(28.dp)),
                contentScale = ContentScale.Crop,
                alpha = if (glass.isDark) 0.36f else 0.25f
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                glass.glassElevated.copy(alpha = 0.85f),
                                glass.glassSurface.copy(alpha = 0.65f)
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val whyPhoto = whyProfile?.photoPath
                if (!whyPhoto.isNullOrBlank() && File(whyPhoto).exists()) {
                    AsyncImage(
                        model = File(whyPhoto),
                        contentDescription = strings.pinnedMyWhy,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .border(2.dp, glass.glassBorder, CircleShape)
                            .clickable { onPhotoClick(whyPhoto) },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(glass.primaryAccent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = strings.pinnedMyWhy,
                            tint = glass.primaryAccent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val studentName = whyProfile?.studentName
                        Text(
                            text = if (!studentName.isNullOrBlank()) {
                                "${strings.pinnedMyWhy} • $studentName"
                            } else {
                                strings.pinnedMyWhy
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = glass.primaryAccent
                        )
                        if (streakDays > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = glass.dueContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = glass.dueColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = strings.streakDaysLabel(streakDays),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = glass.dueColor
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val mainLine = whyProfile?.whyText?.takeIf { it.isNotBlank() }
                        ?: whyProfile?.whoForText?.takeIf { it.isNotBlank() }
                        ?: strings.yourTruthForStudying
                    Text(
                        text = mainLine,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = glass.inkPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val whoFor = whyProfile?.whoForText
                    if (!whoFor.isNullOrBlank() && !whyProfile.whyText.isBlank()) {
                        Text(
                            text = whoFor,
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.inkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val whyVoice = whyProfile?.voicePath
                if (!whyVoice.isNullOrBlank() && File(whyVoice).exists()) {
                    val isPlaying = playingAudioPath == whyVoice
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = glass.glassElevated,
                        border = BorderStroke(1.dp, glass.glassBorder)
                    ) {
                        IconButton(onClick = { onTogglePlayAudio(whyVoice) }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = strings.playVoice,
                                tint = glass.primaryAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteGridCard(
    summary: NoteCardSummary,
    strings: AppStrings,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val glass = LocalGlassPalette.current
    val tint = glass.noteTints.getOrElse(summary.note.colorIndex) { glass.glassSurface }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    GlassCard(
        tintColor = tint,
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(16.dp),
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("note_card_${summary.note.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = glass.glassElevated.copy(alpha = 0.7f)
            ) {
                Text(
                    text = summary.note.subjectTag.ifBlank { strings.generalNoteLabel },
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.inkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Row {
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = if (summary.note.isPinned) strings.unpinNote else strings.pinNote,
                        tint = if (summary.note.isPinned) glass.primaryAccent else glass.inkSecondary.copy(alpha = 0.55f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = strings.deleteNote,
                        tint = glass.inkSecondary.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = summary.note.title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = glass.inkPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = strings.entriesCount(summary.entryCount),
            style = MaterialTheme.typography.bodyMedium,
            color = glass.inkSecondary
        )
        Spacer(modifier = Modifier.height(10.dp))

        val (badgeColor, badgeBg, badgeText) = when {
            summary.missedCount > 0 -> Triple(glass.missedColor, glass.missedContainer, strings.missedCount(summary.missedCount))
            summary.dueCount > 0 -> Triple(glass.dueColor, glass.dueContainer, strings.dueNowCount(summary.dueCount))
            summary.entryCount > 0 -> Triple(glass.doneColor, glass.doneContainer, strings.doneBadge)
            else -> Triple(glass.inkSecondary, glass.glassElevated.copy(alpha = 0.6f), "+")
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = badgeBg
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelLarge,
                    color = badgeColor
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(strings.deleteNote) },
            text = { Text(summary.note.title) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text(strings.deleteNote, color = glass.missedColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun CreateNoteDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onCreate: (title: String, tag: String, colorIdx: Int) -> Unit
) {
    val glass = LocalGlassPalette.current
    var title by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var selectedColor by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = {
            if (title.isNotBlank()) {
                onCreate(title, tag, selectedColor)
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(strings.newNote) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.noteTitleHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_note_title_input"),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text(strings.subjectHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_note_tag_input"),
                    shape = RoundedCornerShape(16.dp)
                )
                Text(strings.colorLabel, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    glass.noteTints.forEachIndexed { idx, color ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (idx == selectedColor) 3.dp else 1.dp,
                                    color = if (idx == selectedColor) glass.primaryAccent else glass.glassBorder,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = idx }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) onCreate(title, tag, selectedColor)
                },
                modifier = Modifier.testTag("confirm_create_note_button")
            ) {
                Text(strings.createNote)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
