package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import coil.compose.AsyncImage
import com.example.data.local.FutureLetterEntity
import com.example.data.local.WhyProfileEntity
import com.example.i18n.AppStrings
import com.example.ui.components.FrostedBackground
import com.example.ui.components.FullScreenPhotoDialog
import com.example.ui.components.GiantActionButton
import com.example.ui.components.GlassCard
import com.example.ui.components.ReviseTopBar
import com.example.ui.components.VoiceMemoRecorderBar
import com.example.ui.theme.LocalGlassPalette
import java.io.File

@Composable
fun MyWhyAndLettersScreen(
    whyProfile: WhyProfileEntity?,
    letters: List<FutureLetterEntity>,
    currentStreak: Int,
    strings: AppStrings,
    isStudyingFocusMode: Boolean,
    hapticEnabled: Boolean,
    isRecording: Boolean,
    playingAudioPath: String?,
    onBack: () -> Unit,
    onToggleLang: () -> Unit,
    onToggleFocusMode: () -> Unit,
    onPickPhotoUri: (Uri, (String?) -> Unit) -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: ((String?) -> Unit) -> Unit,
    onTogglePlayAudio: (String) -> Unit,
    onSaveWhy: (String, String, String, String, String?, String?) -> Unit,
    onCreateLetter: (Int, String, String) -> Unit,
    onDeleteLetter: (Long) -> Unit
) {
    val glass = LocalGlassPalette.current
    var name by remember { mutableStateOf(whyProfile?.studentName.orEmpty()) }
    var why by remember { mutableStateOf(whyProfile?.whyText.orEmpty()) }
    var whoFor by remember { mutableStateOf(whyProfile?.whoForText.orEmpty()) }
    var dream by remember { mutableStateOf(whyProfile?.dreamText.orEmpty()) }
    var photoPath by remember { mutableStateOf(whyProfile?.photoPath) }
    var voicePath by remember { mutableStateOf(whyProfile?.voicePath) }
    var hasUserEditedWhy by remember { mutableStateOf(false) }
    var zoomedPhotoPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(whyProfile) {
        if (!hasUserEditedWhy && whyProfile != null) {
            name = whyProfile.studentName
            why = whyProfile.whyText
            whoFor = whyProfile.whoForText
            dream = whyProfile.dreamText
            photoPath = whyProfile.photoPath
            voicePath = whyProfile.voicePath
        }
    }

    LaunchedEffect(name, why, whoFor, dream, photoPath, voicePath, hasUserEditedWhy) {
        if (hasUserEditedWhy) {
            kotlinx.coroutines.delay(500L)
            onSaveWhy(name, why, whoFor, dream, photoPath, voicePath)
        }
    }

    var letterMilestone by remember { mutableIntStateOf(3) }
    var letterTitle by remember { mutableStateOf("") }
    var letterBody by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onPickPhotoUri(uri) { saved ->
            hasUserEditedWhy = true
            photoPath = saved
        }
    }

    FrostedBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ReviseTopBar(
                    title = strings.myWhy,
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        GlassCard {
                            Text(
                                text = strings.onboardingSubtitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = glass.primaryAccent
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    hasUserEditedWhy = true
                                    name = it
                                },
                                label = { Text(strings.studentNamePrompt) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = why,
                                onValueChange = {
                                    hasUserEditedWhy = true
                                    why = it
                                },
                                label = { Text(strings.whyStudyingPrompt) },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = whoFor,
                                onValueChange = {
                                    hasUserEditedWhy = true
                                    whoFor = it
                                },
                                label = { Text(strings.whoForPrompt) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = dream,
                                onValueChange = {
                                    hasUserEditedWhy = true
                                    dream = it
                                },
                                label = { Text(strings.dreamPrompt) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

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

                            val currentPhoto = photoPath
                            if (!currentPhoto.isNullOrBlank() && File(currentPhoto).exists()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { zoomedPhotoPath = currentPhoto }
                                        .testTag("my_why_photo_preview")
                                ) {
                                    AsyncImage(
                                        model = File(currentPhoto),
                                        contentDescription = strings.addPhotoOptional,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .size(36.dp),
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = Color.Black.copy(alpha = 0.6f)
                                    ) {
                                        IconButton(onClick = {
                                            hasUserEditedWhy = true
                                            photoPath = null
                                        }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = strings.removePhoto,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp),
                                        shape = androidx.compose.foundation.shape.CircleShape,
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

                            Spacer(modifier = Modifier.height(10.dp))
                            val validVoice = voicePath?.takeIf { File(it).exists() }
                            VoiceMemoRecorderBar(
                                audioPath = validVoice,
                                isRecording = isRecording,
                                isPlaying = playingAudioPath != null && playingAudioPath == validVoice,
                                strings = strings,
                                onStartRecord = onStartRecord,
                                onStopRecord = {
                                    onStopRecord { path ->
                                        if (path != null) {
                                            hasUserEditedWhy = true
                                            voicePath = path
                                        }
                                    }
                                },
                                onTogglePlay = onTogglePlayAudio,
                                onClearAudio = {
                                    hasUserEditedWhy = true
                                    voicePath = null
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            GiantActionButton(
                                text = strings.save,
                                icon = Icons.Rounded.Favorite,
                                hapticEnabled = hapticEnabled,
                                minHeight = 76.dp,
                                testTag = "save_my_why_button",
                                onClick = { onSaveWhy(name, why, whoFor, dream, photoPath, voicePath) }
                            )
                        }
                    }

                    // Letters to my future self
                    item {
                        GlassCard {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Mail,
                                    contentDescription = strings.futureLettersTitle,
                                    tint = glass.primaryAccent
                                )
                                Text(
                                    text = strings.futureLettersTitle,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = glass.inkPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(3, 7, 30).forEach { days ->
                                    FilterChip(
                                        selected = letterMilestone == days,
                                        onClick = { letterMilestone = days },
                                        label = { Text(strings.streakDaysLabel(days)) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = letterTitle,
                                onValueChange = { letterTitle = it },
                                label = { Text(strings.letterTitleHint) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("future_letter_title"),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = letterBody,
                                onValueChange = { letterBody = it },
                                placeholder = { Text(strings.writeLetterHint) },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth().testTag("future_letter_body"),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            GiantActionButton(
                                text = strings.saveLetter,
                                icon = Icons.Rounded.Mail,
                                containerColor = glass.secondaryAccent,
                                minHeight = 72.dp,
                                testTag = "save_future_letter_button",
                                onClick = {
                                    if (letterBody.isNotBlank()) {
                                        onCreateLetter(letterMilestone, letterTitle, letterBody)
                                        letterTitle = ""
                                        letterBody = ""
                                    }
                                }
                            )
                        }
                    }

                    items(letters, key = { it.id }) { letter ->
                        val unlocked = letter.isUnlocked || currentStreak >= letter.unlockStreakDays
                        GlassCard(
                            tintColor = if (unlocked) glass.doneContainer else glass.glassSurface
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (unlocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = if (unlocked) glass.doneColor else glass.inkSecondary
                                    )
                                    Column {
                                        Text(
                                            text = letter.title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = glass.inkPrimary
                                        )
                                        Text(
                                            text = if (unlocked) {
                                                strings.unlockedAtDays(letter.unlockStreakDays)
                                            } else {
                                                strings.lockedUntilDays(currentStreak, letter.unlockStreakDays)
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (unlocked) glass.doneColor else glass.inkSecondary
                                        )
                                    }
                                }
                                IconButton(onClick = { onDeleteLetter(letter.id) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = strings.deleteNote,
                                        tint = glass.inkSecondary
                                    )
                                }
                            }
                            if (unlocked) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = letter.bodyText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = glass.inkPrimary
                                )
                            } else {
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(22.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(glass.glassBorder.copy(alpha = 0.25f))
                                )
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
