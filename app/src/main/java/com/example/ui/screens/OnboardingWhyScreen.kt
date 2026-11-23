package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import coil.compose.AsyncImage
import com.example.R
import com.example.data.cloud.GoogleAuthHelper
import com.example.data.cloud.findActivity
import com.example.i18n.AppStrings
import com.example.ui.components.FrostedBackground
import com.example.ui.components.FullScreenPhotoDialog
import com.example.ui.components.GiantActionButton
import com.example.ui.components.GlassCard
import com.example.ui.components.VoiceMemoRecorderBar
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.CloudSyncUiState
import java.io.File

@Composable
fun OnboardingWhyScreen(
    strings: AppStrings,
    isRecording: Boolean,
    playingAudioPath: String?,
    hapticEnabled: Boolean,
    cloudSyncState: CloudSyncUiState,
    onToggleLanguage: () -> Unit,
    onPickPhotoUri: (Uri, (String?) -> Unit) -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: ((String?) -> Unit) -> Unit,
    onTogglePlayAudio: (String) -> Unit,
    onSaveWhy: (name: String, why: String, whoFor: String, dream: String, photoPath: String?, voicePath: String?) -> Unit,
    onRestoreFromGoogleSignIn: () -> Unit,
    onSkip: () -> Unit
) {
    val glass = LocalGlassPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var name by remember { mutableStateOf("") }
    var why by remember { mutableStateOf("") }
    var whoFor by remember { mutableStateOf("") }
    var dream by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf<String?>(null) }
    var voicePath by remember { mutableStateOf<String?>(null) }
    var zoomedPhotoPath by remember { mutableStateOf<String?>(null) }
    var authBusy by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickPhotoUri(uri) { saved -> photoPath = saved }
        }
    }

    FrostedBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.appName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = glass.inkPrimary
                )
                Surface(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("onboarding_lang_toggle")
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onToggleLanguage),
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
                            style = MaterialTheme.typography.labelLarge,
                            color = glass.inkPrimary
                        )
                    }
                }
            }

            GlassCard(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                cornerRadius = 28.dp
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_why_horizon_1795323310321),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = strings.onboardingTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = glass.inkPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.onboardingSubtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = glass.primaryAccent
                )
            }

            GlassCard(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                cornerRadius = 28.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(strings.studentNamePrompt) },
                        placeholder = { Text(strings.studentNameHint) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input"),
                        shape = RoundedCornerShape(18.dp)
                    )
                    OutlinedTextField(
                        value = why,
                        onValueChange = { why = it },
                        label = { Text(strings.whyStudyingPrompt) },
                        placeholder = { Text(strings.whyStudyingHint) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("onboarding_why_input"),
                        shape = RoundedCornerShape(18.dp)
                    )
                    OutlinedTextField(
                        value = whoFor,
                        onValueChange = { whoFor = it },
                        label = { Text(strings.whoForPrompt) },
                        placeholder = { Text(strings.whoForHint) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("onboarding_whofor_input"),
                        shape = RoundedCornerShape(18.dp)
                    )
                    OutlinedTextField(
                        value = dream,
                        onValueChange = { dream = it },
                        label = { Text(strings.dreamPrompt) },
                        placeholder = { Text(strings.dreamHint) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("onboarding_dream_input"),
                        shape = RoundedCornerShape(18.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .heightIn(min = 56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            shape = RoundedCornerShape(18.dp),
                            color = glass.glassElevated,
                            border = BorderStroke(1.dp, glass.glassBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddPhotoAlternate,
                                    contentDescription = strings.addPhotoOptional,
                                    tint = glass.secondaryAccent
                                )
                                Text(
                                    text = strings.addPhotoOptional,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = glass.inkPrimary
                                )
                            }
                        }
                    }

                    val currentPhoto = photoPath
                    if (!currentPhoto.isNullOrBlank() && File(currentPhoto).exists()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { zoomedPhotoPath = currentPhoto }
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
                                IconButton(onClick = { photoPath = null }) {
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

                    val validVoice = voicePath?.takeIf { File(it).exists() }
                    VoiceMemoRecorderBar(
                        audioPath = validVoice,
                        isRecording = isRecording,
                        isPlaying = playingAudioPath != null && playingAudioPath == validVoice,
                        strings = strings,
                        onStartRecord = onStartRecord,
                        onStopRecord = { onStopRecord { path -> if (path != null) voicePath = path } },
                        onTogglePlay = onTogglePlayAudio,
                        onClearAudio = { voicePath = null }
                    )
                }
            }

            Box(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                GiantActionButton(
                    text = strings.beginJourney,
                    icon = Icons.Rounded.Favorite,
                    hapticEnabled = hapticEnabled,
                    testTag = "onboarding_save_button",
                    onClick = {
                        onSaveWhy(name, why, whoFor, dream, photoPath, voicePath)
                    }
                )
            }

            val isRestoring = cloudSyncState is CloudSyncUiState.Syncing
            OutlinedButton(
                onClick = {
                    if (authBusy || isRestoring) return@OutlinedButton
                    val activity = context.findActivity() ?: return@OutlinedButton
                    authBusy = true
                    authError = null
                    GoogleAuthHelper.signInInteractive(
                        activity = activity,
                        credentialManager = credentialManager,
                        scope = scope,
                        onAuthSuccess = {
                            authBusy = false
                            onRestoreFromGoogleSignIn()
                        },
                        onAuthCancelled = { authBusy = false },
                        onAuthError = { err ->
                            authBusy = false
                            authError = err
                        }
                    )
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccountCircle,
                    contentDescription = strings.signInWithGoogle,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    when {
                        authBusy -> strings.connecting
                        isRestoring -> strings.syncingNow
                        else -> "${strings.signInWithGoogle} • ${strings.restoreFromCloudBtn}"
                    }
                )
            }

            if (authError != null) {
                Text(
                    text = authError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.missedColor
                )
            } else if (cloudSyncState is CloudSyncUiState.Error) {
                Text(
                    text = cloudSyncState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.missedColor
                )
            } else if (cloudSyncState is CloudSyncUiState.Success) {
                Text(
                    text = cloudSyncState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.doneColor
                )
            }

            TextButton(
                onClick = onSkip,
                modifier = Modifier.heightIn(min = 48.dp).testTag("onboarding_skip_button")
            ) {
                Text(strings.skipForNow, color = glass.inkSecondary)
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
