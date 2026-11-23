package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.cloud.GoogleAuthHelper
import com.example.data.cloud.findActivity
import com.example.data.prefs.AppSettings
import com.example.i18n.AppStrings
import com.example.ui.components.FrostedBackground
import com.example.ui.components.GiantActionButton
import com.example.ui.components.GlassCard
import com.example.ui.components.ReviseTopBar
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.BackupFeedback
import com.example.ui.viewmodel.CloudSyncUiState
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupAndSecurityScreen(
    startOnSecurity: Boolean,
    settings: AppSettings,
    backupFeedback: BackupFeedback?,
    currentGoogleUser: FirebaseUser?,
    cloudSyncState: CloudSyncUiState,
    strings: AppStrings,
    onBack: () -> Unit,
    onToggleLang: () -> Unit,
    onToggleFocusMode: () -> Unit,
    onExportBackup: (Uri, String) -> Unit,
    onImportBackup: (Uri, String) -> Unit,
    onBackupValidationError: (String) -> Unit,
    onClearBackupMessage: () -> Unit,
    onGoogleSignInSuccess: () -> Unit,
    onSyncToCloud: () -> Unit,
    onRestoreFromCloud: () -> Unit,
    onCloudAuthError: (String) -> Unit,
    onClearCloudSyncStatus: () -> Unit,
    onSetPinLock: (String) -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onDeleteAllData: () -> Unit
) {
    val glass = LocalGlassPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var passphrase by remember { mutableStateOf("") }
    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var pinFeedback by remember { mutableStateOf<String?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showCloudRestoreConfirm by remember { mutableStateOf(false) }
    var authInFlight by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            if (passphrase.length >= 4) {
                onExportBackup(uri, passphrase)
            } else {
                onBackupValidationError(strings.passphraseTooShort)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            if (passphrase.length >= 4) {
                onImportBackup(uri, passphrase)
            } else {
                onBackupValidationError(strings.passphraseTooShort)
            }
        }
    }

    @Composable
    fun CloudSyncCard() {
        val isSyncing = cloudSyncState is CloudSyncUiState.Syncing
        GlassCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccountCircle,
                    contentDescription = strings.cloudSyncTitle,
                    tint = glass.primaryAccent
                )
                Text(
                    text = strings.cloudSyncTitle,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = glass.inkPrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.cloudSyncSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = glass.inkSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (currentGoogleUser == null) {
                GiantActionButton(
                    text = if (authInFlight) strings.connecting else strings.signInWithGoogle,
                    icon = Icons.Rounded.AccountCircle,
                    containerColor = glass.primaryAccent,
                    minHeight = 76.dp,
                    testTag = "google_sign_in_button",
                    onClick = {
                        if (authInFlight) return@GiantActionButton
                        val activity = context.findActivity()
                        if (activity == null) {
                            onCloudAuthError(strings.cloudSyncError)
                            return@GiantActionButton
                        }
                        authInFlight = true
                        onClearCloudSyncStatus()
                        GoogleAuthHelper.signInInteractive(
                            activity = activity,
                            credentialManager = credentialManager,
                            scope = scope,
                            onAuthSuccess = {
                                authInFlight = false
                                onGoogleSignInSuccess()
                            },
                            onAuthCancelled = {
                                authInFlight = false
                            },
                            onAuthError = { err ->
                                authInFlight = false
                                onCloudAuthError(err)
                            }
                        )
                    }
                )
            } else {
                val identityLabel = currentGoogleUser.displayName?.takeIf { it.isNotBlank() }
                    ?: currentGoogleUser.email?.takeIf { it.isNotBlank() }
                    ?: "Google"
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = glass.doneContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = strings.signedInAs(identityLabel),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = glass.doneColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.cloudAutoSyncActiveNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.inkSecondary
                        )
                        if (settings.lastCloudSyncAtMillis > 0L) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strings.lastSyncedLabel(
                                    dateFormatter.format(Date(settings.lastCloudSyncAtMillis))
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.inkSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (isSyncing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        Text(
                            text = strings.syncingNow,
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.inkPrimary
                        )
                    }
                }

                GiantActionButton(
                    text = strings.syncNowBtn,
                    icon = Icons.Rounded.CloudUpload,
                    containerColor = glass.doneColor,
                    minHeight = 76.dp,
                    testTag = "cloud_sync_now_button",
                    onClick = {
                        if (!isSyncing) onSyncToCloud()
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                GiantActionButton(
                    text = strings.restoreFromCloudBtn,
                    icon = Icons.Rounded.CloudDownload,
                    containerColor = glass.primaryAccent,
                    minHeight = 72.dp,
                    testTag = "cloud_restore_button",
                    onClick = {
                        if (!isSyncing) showCloudRestoreConfirm = true
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        onClearCloudSyncStatus()
                        GoogleAuthHelper.signOut(credentialManager, scope)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("google_sign_out_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = strings.signOutGoogle,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(strings.signOutGoogle)
                }
            }

            when (cloudSyncState) {
                is CloudSyncUiState.Success -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = cloudSyncState.message,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = glass.doneColor,
                        modifier = Modifier.testTag("cloud_sync_status_message")
                    )
                }
                is CloudSyncUiState.Error -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = cloudSyncState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.missedColor,
                        modifier = Modifier.testTag("cloud_sync_status_message")
                    )
                }
                else -> Unit
            }
        }
    }

    @Composable
    fun EncryptedBackupCard() {
        GlassCard {
            Text(
                text = strings.backupExport,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = glass.inkPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = passphrase,
                onValueChange = {
                    passphrase = it
                    onClearBackupMessage()
                },
                label = { Text(strings.passphrasePrompt) },
                placeholder = { Text(strings.passphraseHint) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().testTag("backup_passphrase_input"),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            GiantActionButton(
                text = strings.exportEncryptedBtn,
                icon = Icons.Rounded.Backup,
                containerColor = glass.doneColor,
                minHeight = 76.dp,
                testTag = "export_backup_button",
                onClick = {
                    if (passphrase.length >= 4) {
                        exportLauncher.launch("revisekeep_backup.revisekeep")
                    } else {
                        onBackupValidationError(strings.passphraseTooShort)
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            GiantActionButton(
                text = strings.importEncryptedBtn,
                icon = Icons.Rounded.Download,
                containerColor = glass.primaryAccent,
                minHeight = 76.dp,
                testTag = "import_backup_button",
                onClick = {
                    if (passphrase.length >= 4) {
                        importLauncher.launch(arrayOf("*/*"))
                    } else {
                        onBackupValidationError(strings.passphraseTooShort)
                    }
                }
            )
            if (backupFeedback != null && backupFeedback.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = backupFeedback.message,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (backupFeedback.isError) glass.missedColor else glass.doneColor,
                    modifier = Modifier.testTag("backup_feedback_message")
                )
            }
        }
    }

    @Composable
    fun SecurityLockCard() {
        GlassCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = strings.security,
                    tint = glass.primaryAccent
                )
                Text(
                    text = strings.appLockTitle,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = glass.inkPrimary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = pin1,
                onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pin1 = it },
                label = { Text(strings.enterPinPrompt) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("security_pin_1"),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = pin2,
                onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pin2 = it },
                label = { Text(strings.confirmPinPrompt) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("security_pin_2"),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GiantActionButton(
                    text = strings.setPinBtn,
                    icon = Icons.Rounded.Lock,
                    minHeight = 68.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "save_pin_button",
                    onClick = {
                        if (pin1.length == 4 && pin1 == pin2) {
                            onSetPinLock(pin1)
                            pin1 = ""
                            pin2 = ""
                            pinFeedback = "PIN active"
                        } else {
                            pinFeedback = strings.pinMismatch
                        }
                    }
                )
            }
            if (settings.hasPinLock) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { onSetPinLock("") }) {
                    Text("Disable PIN")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.biometricToggle, modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.biometricEnabled,
                        onCheckedChange = onSetBiometricEnabled
                    )
                }
            }
            if (pinFeedback != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(pinFeedback!!, color = glass.primaryAccent)
            }
        }
    }

    FrostedBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ReviseTopBar(
                    title = if (startOnSecurity) strings.security else strings.backupExport,
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
                    if (startOnSecurity) {
                        SecurityLockCard()
                        CloudSyncCard()
                        EncryptedBackupCard()
                    } else {
                        CloudSyncCard()
                        EncryptedBackupCard()
                        SecurityLockCard()
                    }

                    // One-Tap Delete All My Data
                    GlassCard(tintColor = glass.missedContainer) {
                        GiantActionButton(
                            text = strings.deleteAllDataBtn,
                            icon = Icons.Rounded.DeleteForever,
                            containerColor = glass.missedColor,
                            minHeight = 80.dp,
                            testTag = "delete_all_data_button",
                            onClick = { showDeleteAllDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showCloudRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showCloudRestoreConfirm = false },
            title = { Text(strings.restoreFromCloudBtn) },
            text = { Text(strings.cloudSyncSubtitle) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCloudRestoreConfirm = false
                        onRestoreFromCloud()
                    },
                    modifier = Modifier.testTag("confirm_cloud_restore")
                ) {
                    Text(strings.restoreFromCloudBtn, color = glass.primaryAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloudRestoreConfirm = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(strings.deleteAllConfirmTitle) },
            text = { Text(strings.deleteAllConfirmMsg) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAllDialog = false
                        onDeleteAllData()
                    },
                    modifier = Modifier.testTag("confirm_delete_all_data")
                ) {
                    Text(strings.deleteEverything, color = glass.missedColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
