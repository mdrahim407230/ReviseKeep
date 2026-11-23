package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.ZoomIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.local.EntryWithMilestones
import com.example.data.local.MilestoneStatus
import com.example.data.local.NoteEntity
import com.example.data.local.StudyEntryEntity
import com.example.i18n.AppLang
import com.example.i18n.AppStrings
import com.example.ui.components.FrostedBackground
import com.example.ui.components.FullScreenPhotoDialog
import com.example.ui.components.GiantActionButton
import com.example.ui.components.GlassCard
import com.example.ui.components.ReviseTopBar
import com.example.ui.components.StageBadge
import com.example.ui.components.VoiceMemoRecorderBar
import com.example.ui.theme.LocalGlassPalette
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NoteDetailScreen(
    note: NoteEntity?,
    entries: List<EntryWithMilestones>,
    effectiveNowMillis: Long,
    strings: AppStrings,
    lang: AppLang,
    isStudyingFocusMode: Boolean,
    hapticEnabled: Boolean,
    isRecording: Boolean,
    playingAudioPath: String?,
    onBack: () -> Unit,
    onToggleLang: () -> Unit,
    onToggleFocusMode: () -> Unit,
    onUpdateNote: (NoteEntity) -> Unit,
    onDeleteNote: () -> Unit,
    onAddEntry: (text: String, audioPath: String?, photoPath: String?) -> Unit,
    onUpdateEntry: (entryId: Long, text: String, audioPath: String?, photoPath: String?) -> Unit,
    onDeleteEntry: (entryId: Long) -> Unit,
    onCompleteMilestone: (Long) -> Unit,
    onOpenNoteCheckRevision: () -> Unit,
    onPickPhotoUri: (Uri, (String?) -> Unit) -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: ((String?) -> Unit) -> Unit,
    onTogglePlayAudio: (String) -> Unit
) {
    val glass = LocalGlassPalette.current
    var showAddEntryDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<StudyEntryEntity?>(null) }
    var deletingEntry by remember { mutableStateOf<StudyEntryEntity?>(null) }
    var showEditNoteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var fullScreenPhotoPath by remember { mutableStateOf<String?>(null) }

    val dateFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    val noteMissedCount = remember(entries, effectiveNowMillis) {
        entries.sumOf { e ->
            e.milestones.count {
                it.status == MilestoneStatus.MISSED ||
                    (it.status == MilestoneStatus.ACTIVE_DUE && effectiveNowMillis > it.dueAt)
            }
        }
    }
    val noteDueCount = remember(entries, effectiveNowMillis) {
        entries.sumOf { e ->
            e.milestones.count {
                it.status == MilestoneStatus.MISSED ||
                    (it.status == MilestoneStatus.ACTIVE_DUE && it.dueAt <= effectiveNowMillis)
            }
        }
    }

    FrostedBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ReviseTopBar(
                    title = note?.title ?: strings.appName,
                    strings = strings,
                    onBackClick = onBack,
                    onToggleLang = onToggleLang,
                    isStudyingFocusMode = isStudyingFocusMode,
                    onToggleFocusMode = onToggleFocusMode
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddEntryDialog = true },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .size(72.dp)
                        .testTag("add_entry_fab"),
                    shape = CircleShape,
                    containerColor = glass.primaryAccent,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = strings.addStudyEntry,
                        modifier = Modifier.size(36.dp)
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
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 112.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Note controls bar
                    if (note != null) {
                        item {
                            GlassCard(
                                tintColor = glass.noteTints.getOrElse(note.colorIndex) { glass.glassSurface },
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = note.title,
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = glass.inkPrimary
                                        )
                                        Text(
                                            text = note.subjectTag.ifBlank { strings.generalNoteLabel },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = glass.inkSecondary
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = { showEditNoteDialog = true }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = strings.renameNote,
                                                tint = glass.inkPrimary
                                            )
                                        }
                                        IconButton(onClick = { onUpdateNote(note.copy(isPinned = !note.isPinned)) }) {
                                            Icon(
                                                imageVector = Icons.Rounded.PushPin,
                                                contentDescription = strings.pinNote,
                                                tint = if (note.isPinned) glass.primaryAccent else glass.inkSecondary
                                            )
                                        }
                                        IconButton(onClick = { showDeleteConfirm = true }) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = strings.deleteNote,
                                                tint = glass.missedColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Per-Note Check Revision Button (only for this note)
                    item {
                        val checkColor = when {
                            noteMissedCount > 0 -> glass.missedColor
                            noteDueCount > 0 -> glass.dueColor
                            else -> glass.doneColor
                        }
                        val checkSubtitle = when {
                            noteMissedCount > 0 -> "${strings.dueNowCount(noteDueCount)} • ${strings.missedCount(noteMissedCount)}"
                            noteDueCount > 0 -> strings.dueNowCount(noteDueCount)
                            else -> strings.allCaughtUp
                        }
                        val checkIcon = if (noteDueCount > 0) Icons.Rounded.Schedule else Icons.Rounded.CheckCircle
                        GiantActionButton(
                            text = strings.checkRevision,
                            subtitle = checkSubtitle,
                            icon = checkIcon,
                            containerColor = checkColor,
                            hapticEnabled = hapticEnabled,
                            minHeight = 84.dp,
                            testTag = "note_check_revision_button",
                            onClick = onOpenNoteCheckRevision
                        )
                    }

                    // Prominent "+" Action Card for adding today's lesson
                    item {
                        GiantActionButton(
                            text = strings.addStudyEntry,
                            icon = Icons.Rounded.Add,
                            hapticEnabled = hapticEnabled,
                            minHeight = 84.dp,
                            testTag = "add_entry_banner_button",
                            onClick = { showAddEntryDialog = true }
                        )
                    }

                    if (entries.isEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = strings.noEntriesYet,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = glass.inkSecondary
                                )
                            }
                        }
                    } else {
                        items(entries, key = { it.entry.id }) { entryWithMilestones ->
                            val entry = entryWithMilestones.entry
                            val milestones = entryWithMilestones.milestones.sortedBy { it.stageIndex }
                            val activeOrMissed = milestones.firstOrNull {
                                it.status == MilestoneStatus.MISSED ||
                                    it.status == MilestoneStatus.ACTIVE_DUE
                            }
                            val isDueNow = activeOrMissed != null &&
                                (activeOrMissed.status == MilestoneStatus.MISSED || activeOrMissed.dueAt <= effectiveNowMillis)
                            val isMissed = activeOrMissed != null &&
                                (activeOrMissed.status == MilestoneStatus.MISSED || effectiveNowMillis > activeOrMissed.dueAt)
                            val allCompleted = milestones.isNotEmpty() &&
                                milestones.all { it.status == MilestoneStatus.COMPLETED }

                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("entry_card_${entry.id}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = strings.createdAtLabel(dateFormatter.format(Date(entry.createdAt))),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = glass.inkSecondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (activeOrMissed != null) {
                                            val effectiveStatus = if (isMissed) MilestoneStatus.MISSED else activeOrMissed.status
                                            StageBadge(
                                                stageIndex = activeOrMissed.stageIndex,
                                                status = effectiveStatus,
                                                lang = lang,
                                                strings = strings
                                            )
                                        } else if (allCompleted) {
                                            StageBadge(
                                                stageIndex = 15,
                                                status = MilestoneStatus.COMPLETED,
                                                lang = lang,
                                                strings = strings
                                            )
                                        }
                                        IconButton(
                                            onClick = { editingEntry = entry },
                                            modifier = Modifier.testTag("edit_entry_button_${entry.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = strings.editEntry,
                                                tint = glass.inkPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { deletingEntry = entry },
                                            modifier = Modifier.testTag("delete_entry_button_${entry.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = strings.deleteEntry,
                                                tint = glass.missedColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                if (entry.contentText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = entry.contentText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = glass.inkPrimary
                                    )
                                }

                                val photoPath = entry.photoPath
                                if (!photoPath.isNullOrBlank() && remember(photoPath) { File(photoPath).exists() }) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(190.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .clickable { fullScreenPhotoPath = photoPath }
                                            .testTag("entry_photo_${entry.id}")
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
                                                .padding(10.dp),
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.55f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ZoomIn,
                                                contentDescription = strings.zoomPhotoHint,
                                                tint = Color.White,
                                                modifier = Modifier.padding(7.dp).size(18.dp)
                                            )
                                        }
                                    }
                                }

                                val audioPath = entry.audioPath
                                if (!audioPath.isNullOrBlank() && remember(audioPath) { File(audioPath).exists() }) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val isPlaying = playingAudioPath == audioPath
                                    Surface(
                                        modifier = Modifier
                                            .heightIn(min = 52.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { onTogglePlayAudio(audioPath) },
                                        shape = RoundedCornerShape(16.dp),
                                        color = glass.doneContainer,
                                        border = BorderStroke(1.dp, glass.doneColor.copy(alpha = 0.45f))
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

                                if (activeOrMissed != null && isDueNow) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    GiantActionButton(
                                        text = strings.revisionComplete,
                                        icon = Icons.Rounded.CheckCircle,
                                        containerColor = if (isMissed) glass.missedColor else glass.dueColor,
                                        hapticEnabled = hapticEnabled,
                                        minHeight = 76.dp,
                                        testTag = "entry_revision_complete_${activeOrMissed.id}",
                                        onClick = { onCompleteMilestone(activeOrMissed.id) }
                                    )
                                } else if (activeOrMissed != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = strings.dueAtLabel(dateFormatter.format(Date(activeOrMissed.dueAt))),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = glass.inkSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddEntryDialog) {
        StudyEntryEditorDialog(
            title = strings.whatStudiedToday,
            initialText = "",
            initialPhotoPath = null,
            initialAudioPath = null,
            strings = strings,
            isRecording = isRecording,
            playingAudioPath = playingAudioPath,
            onDismiss = { showAddEntryDialog = false },
            onPickPhotoUri = onPickPhotoUri,
            onStartRecord = onStartRecord,
            onStopRecord = onStopRecord,
            onTogglePlayAudio = onTogglePlayAudio,
            onSave = { text, audio, photo ->
                showAddEntryDialog = false
                onAddEntry(text, audio, photo)
            }
        )
    }

    editingEntry?.let { entry ->
        StudyEntryEditorDialog(
            title = strings.editEntry,
            initialText = entry.contentText,
            initialPhotoPath = entry.photoPath,
            initialAudioPath = entry.audioPath,
            strings = strings,
            isRecording = isRecording,
            playingAudioPath = playingAudioPath,
            onDismiss = { editingEntry = null },
            onPickPhotoUri = onPickPhotoUri,
            onStartRecord = onStartRecord,
            onStopRecord = onStopRecord,
            onTogglePlayAudio = onTogglePlayAudio,
            onSave = { text, audio, photo ->
                editingEntry = null
                onUpdateEntry(entry.id, text, audio, photo)
            }
        )
    }

    deletingEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { deletingEntry = null },
            title = { Text(strings.deleteEntry) },
            text = { Text(strings.deleteEntryConfirmMsg) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingEntry = null
                        onDeleteEntry(entry.id)
                    },
                    modifier = Modifier.testTag("confirm_delete_entry_button")
                ) {
                    Text(strings.deleteEntry, color = glass.missedColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingEntry = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showEditNoteDialog && note != null) {
        EditNoteDialog(
            note = note,
            strings = strings,
            onDismiss = { showEditNoteDialog = false },
            onSave = { updated ->
                showEditNoteDialog = false
                onUpdateNote(updated)
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(strings.deleteNote) },
            text = { Text(note?.title.orEmpty()) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteNote()
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

    fullScreenPhotoPath?.let { path ->
        FullScreenPhotoDialog(
            photoPath = path,
            strings = strings,
            onDismiss = { fullScreenPhotoPath = null }
        )
    }
}

@Composable
private fun StudyEntryEditorDialog(
    title: String,
    initialText: String,
    initialPhotoPath: String?,
    initialAudioPath: String?,
    strings: AppStrings,
    isRecording: Boolean,
    playingAudioPath: String?,
    onDismiss: () -> Unit,
    onPickPhotoUri: (Uri, (String?) -> Unit) -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: ((String?) -> Unit) -> Unit,
    onTogglePlayAudio: (String) -> Unit,
    onSave: (String, String?, String?) -> Unit
) {
    val glass = LocalGlassPalette.current
    var text by remember(initialText) { mutableStateOf(initialText) }
    var photoPath by remember(initialPhotoPath) { mutableStateOf(initialPhotoPath) }
    var audioPath by remember(initialAudioPath) { mutableStateOf(initialAudioPath) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickPhotoUri(uri) { path -> photoPath = path }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (text.isNotBlank() || audioPath != null || photoPath != null) {
                onSave(text, audioPath, photoPath)
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(strings.whatStudiedHint) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("study_entry_text_input"),
                    shape = RoundedCornerShape(16.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = glass.glassElevated,
                        border = BorderStroke(1.dp, glass.glassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddPhotoAlternate,
                                contentDescription = strings.addPhotoOptional,
                                tint = glass.secondaryAccent
                            )
                            Text(strings.addPhotoOptional, style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    if (!photoPath.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = glass.missedContainer,
                            border = BorderStroke(1.dp, glass.missedColor.copy(alpha = 0.45f))
                        ) {
                            IconButton(onClick = { photoPath = null }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = strings.removePhoto,
                                    tint = glass.missedColor
                                )
                            }
                        }
                    }
                }

                if (!photoPath.isNullOrBlank() && remember(photoPath) { File(photoPath!!).exists() }) {
                    AsyncImage(
                        model = File(photoPath!!),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                VoiceMemoRecorderBar(
                    audioPath = audioPath?.takeIf { File(it).exists() },
                    isRecording = isRecording,
                    isPlaying = playingAudioPath != null && playingAudioPath == audioPath,
                    strings = strings,
                    onStartRecord = onStartRecord,
                    onStopRecord = { onStopRecord { path -> if (path != null) audioPath = path } },
                    onTogglePlay = onTogglePlayAudio,
                    onClearAudio = { audioPath = null }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isNotBlank() || audioPath != null || photoPath != null) {
                        onSave(text, audioPath, photoPath)
                    }
                },
                modifier = Modifier.testTag("save_study_entry_button")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
private fun EditNoteDialog(
    note: NoteEntity,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (NoteEntity) -> Unit
) {
    val glass = LocalGlassPalette.current
    var title by remember { mutableStateOf(note.title) }
    var tag by remember { mutableStateOf(note.subjectTag) }
    var colorIdx by remember { mutableIntStateOf(note.colorIndex) }

    AlertDialog(
        onDismissRequest = {
            if (title.isNotBlank()) {
                onSave(note.copy(title = title, subjectTag = tag, colorIndex = colorIdx))
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(strings.renameNote) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.noteTitleHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text(strings.subjectHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    glass.noteTints.forEachIndexed { idx, color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (idx == colorIdx) 3.dp else 1.dp,
                                    color = if (idx == colorIdx) glass.primaryAccent else glass.glassBorder,
                                    shape = CircleShape
                                )
                                .clickable { colorIdx = idx }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) onSave(note.copy(title = title, subjectTag = tag, colorIndex = colorIdx))
            }) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
