package com.example.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.prefs.AppSettings
import com.example.i18n.AppStrings
import com.example.ui.components.FrostedBackground
import com.example.ui.components.GiantActionButton
import com.example.ui.components.GlassCard
import com.example.ui.components.ReviseTopBar
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.AiConnectionUiState

import androidx.compose.runtime.LaunchedEffect

@Composable
fun AiSettingsScreen(
    settings: AppSettings,
    savedApiKey: String,
    connectionState: AiConnectionUiState,
    previewPair: Pair<String, String>?,
    strings: AppStrings,
    onBack: () -> Unit,
    onToggleLang: () -> Unit,
    onToggleFocusMode: () -> Unit,
    onConnectKey: (String) -> Unit,
    onAutoSaveKey: (String) -> Unit,
    onDisconnectKey: () -> Unit,
    onSaveStylePrompt: (String) -> Unit,
    onToggleSendFullNote: (Boolean) -> Unit,
    onPreviewNotification: () -> Unit
) {
    val glass = LocalGlassPalette.current
    var keyInput by remember { mutableStateOf(savedApiKey) }
    var hasUserEditedKey by remember { mutableStateOf(false) }
    var maskKey by remember { mutableStateOf(true) }
    var stylePrompt by remember { mutableStateOf(settings.aiStylePrompt) }
    var hasUserEditedStyle by remember { mutableStateOf(false) }

    LaunchedEffect(savedApiKey) {
        if (!hasUserEditedKey && savedApiKey.isNotBlank() && keyInput != savedApiKey) {
            keyInput = savedApiKey
        }
    }

    LaunchedEffect(keyInput, hasUserEditedKey) {
        if (hasUserEditedKey) {
            val trimmed = keyInput.trim()
            if (trimmed.isNotBlank() && trimmed != savedApiKey) {
                kotlinx.coroutines.delay(450L)
                onAutoSaveKey(trimmed)
            }
        }
    }

    LaunchedEffect(settings.aiStylePrompt) {
        if (!hasUserEditedStyle && stylePrompt != settings.aiStylePrompt) {
            stylePrompt = settings.aiStylePrompt
        }
    }

    FrostedBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ReviseTopBar(
                    title = strings.aiTitle,
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
                    GlassCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = strings.aiTitle,
                                tint = glass.secondaryAccent,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = strings.aiScreenSubtitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = glass.inkPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = keyInput,
                            onValueChange = {
                                hasUserEditedKey = true
                                keyInput = it
                            },
                            label = { Text(strings.apiKeyLabel) },
                            placeholder = { Text(strings.apiKeyHint) },
                            singleLine = true,
                            visualTransformation = if (maskKey) PasswordVisualTransformation() else VisualTransformation.None,
                            leadingIcon = {
                                Icon(Icons.Rounded.Key, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { maskKey = !maskKey }) {
                                    Icon(
                                        imageVector = if (maskKey) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("ai_api_key_input"),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (connectionState is AiConnectionUiState.Connecting) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(26.dp))
                                Spacer(modifier = Modifier.size(12.dp))
                                Text(strings.connecting, color = glass.inkPrimary)
                            }
                        } else {
                            GiantActionButton(
                                text = strings.connectBtn,
                                icon = Icons.Rounded.CheckCircle,
                                minHeight = 78.dp,
                                testTag = "ai_connect_button",
                                onClick = {
                                    if (keyInput.isNotBlank()) onConnectKey(keyInput)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Small model status line as specified
                        when (connectionState) {
                            is AiConnectionUiState.Connected -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        if (settings.hasApiKey || savedApiKey.isNotBlank()) {
                                            Text(
                                                text = strings.apiKeySavedBadge,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = glass.doneColor,
                                                modifier = Modifier.testTag("ai_key_saved_badge")
                                            )
                                        }
                                        Text(
                                            text = strings.connectedModelLabel(connectionState.modelId),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = glass.doneColor,
                                            modifier = Modifier.testTag("ai_active_model_text")
                                        )
                                    }
                                    if (settings.hasApiKey || savedApiKey.isNotBlank()) {
                                        OutlinedButton(onClick = {
                                            hasUserEditedKey = false
                                            keyInput = ""
                                            onDisconnectKey()
                                        }) {
                                            Icon(Icons.Rounded.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.size(6.dp))
                                            Text("Clear")
                                        }
                                    }
                                }
                            }
                            is AiConnectionUiState.Error -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        if (settings.hasApiKey || savedApiKey.isNotBlank()) {
                                            Text(
                                                text = strings.apiKeySavedBadge,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = glass.doneColor,
                                                modifier = Modifier.testTag("ai_key_saved_badge")
                                            )
                                        }
                                        Text(
                                            text = connectionState.message,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = glass.missedColor
                                        )
                                    }
                                    if (settings.hasApiKey || savedApiKey.isNotBlank()) {
                                        OutlinedButton(onClick = {
                                            hasUserEditedKey = false
                                            keyInput = ""
                                            onDisconnectKey()
                                        }) {
                                            Icon(Icons.Rounded.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.size(6.dp))
                                            Text("Clear")
                                        }
                                    }
                                }
                            }
                            else -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        if (settings.hasApiKey || savedApiKey.isNotBlank()) {
                                            Text(
                                                text = strings.apiKeySavedBadge,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = glass.doneColor,
                                                modifier = Modifier.testTag("ai_key_saved_badge")
                                            )
                                        }
                                        Text(
                                            text = if (settings.cachedModelId.isNotBlank()) {
                                                strings.connectedModelLabel(settings.cachedModelId)
                                            } else {
                                                strings.aiNotConnected
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = glass.inkSecondary
                                        )
                                    }
                                    if (settings.hasApiKey || savedApiKey.isNotBlank()) {
                                        OutlinedButton(onClick = {
                                            hasUserEditedKey = false
                                            keyInput = ""
                                            onDisconnectKey()
                                        }) {
                                            Icon(Icons.Rounded.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.size(6.dp))
                                            Text("Clear")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Free-text notification requirement box
                    GlassCard {
                        Text(
                            text = strings.aiStylePromptLabel,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = glass.inkPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = stylePrompt,
                            onValueChange = {
                                hasUserEditedStyle = true
                                stylePrompt = it
                                onSaveStylePrompt(it)
                            },
                            placeholder = { Text(strings.aiStylePromptHint) },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth().testTag("ai_style_prompt_input"),
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = strings.aiSendFullNoteToggle,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = glass.inkPrimary
                                )
                                Text(
                                    text = strings.aiSendFullNoteDesc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glass.inkSecondary
                                )
                            }
                            Switch(
                                checked = settings.sendFullNoteToAi,
                                onCheckedChange = onToggleSendFullNote,
                                modifier = Modifier.testTag("ai_full_note_switch")
                            )
                        }
                    }

                    // Preview & offline fallback assurance
                    GlassCard {
                        GiantActionButton(
                            text = strings.aiTestPreviewBtn,
                            icon = Icons.Rounded.AutoAwesome,
                            containerColor = glass.secondaryAccent,
                            minHeight = 76.dp,
                            testTag = "ai_preview_button",
                            onClick = onPreviewNotification
                        )
                        if (previewPair != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = glass.glassElevated,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = previewPair.first,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = glass.inkPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = previewPair.second,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = glass.inkSecondary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = strings.aiOfflineFallbackNote,
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.inkSecondary
                        )
                    }
                }
            }
        }
    }
}
