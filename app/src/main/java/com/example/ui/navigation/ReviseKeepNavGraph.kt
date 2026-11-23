package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.MilestoneStatus
import com.example.i18n.StringsBnEn
import com.example.ui.components.FrostedBackground
import com.example.ui.screens.AiSettingsScreen
import com.example.ui.screens.BackupAndSecurityScreen
import com.example.ui.screens.CheckRevisionScreen
import com.example.ui.screens.HamburgerHubSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HubDestination
import com.example.ui.screens.LockScreen
import com.example.ui.screens.LookAndFeelScreen
import com.example.ui.screens.MyWhyAndLettersScreen
import com.example.ui.screens.NoteDetailScreen
import com.example.ui.screens.NotificationSettingsScreen
import com.example.ui.screens.OnboardingWhyScreen
import com.example.ui.screens.ProgressStreakScreen
import com.example.ui.viewmodel.MainViewModel

object ReviseRoutes {
    const val HOME = "home"
    const val NOTE_DETAIL = "note_detail/{noteId}"
    const val CHECK_REVISION = "check_revision"
    const val NOTE_CHECK_REVISION = "check_revision/{noteId}"
    const val MY_WHY = "my_why"
    const val AI = "ai"
    const val PROGRESS = "progress"
    const val NOTIFICATIONS = "notifications"
    const val LOOK_AND_FEEL = "look_and_feel"
    const val BACKUP = "backup"
    const val SECURITY = "security"

    fun noteDetail(noteId: Long): String = "note_detail/$noteId"
    fun noteCheckRevision(noteId: Long): String = "check_revision/$noteId"
}

@Composable
fun ReviseKeepRoot(
    viewModel: MainViewModel,
    openCheckRevisionOnLaunch: Boolean
) {
    val settingsState by viewModel.settings.collectAsStateWithLifecycle()
    val isStartupReady by viewModel.isStartupReady.collectAsStateWithLifecycle()
    val whyProfile by viewModel.whyProfile.collectAsStateWithLifecycle()
    val noteCards by viewModel.noteCards.collectAsStateWithLifecycle()
    val dueQueue by viewModel.serialDueQueue.collectAsStateWithLifecycle()
    val futureLetters by viewModel.futureLetters.collectAsStateWithLifecycle()
    val recentActivity by viewModel.recentActivity.collectAsStateWithLifecycle()
    val allMilestones by viewModel.allMilestones.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    val savedApiKey by viewModel.savedApiKey.collectAsStateWithLifecycle()
    val aiConnectionState by viewModel.aiConnectionState.collectAsStateWithLifecycle()
    val aiPreviewPair by viewModel.aiPreviewPair.collectAsStateWithLifecycle()
    val completionOutcome by viewModel.completionOutcome.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecordingAudio.collectAsStateWithLifecycle()
    val playingAudioPath by viewModel.playingAudioPath.collectAsStateWithLifecycle()
    val backupFeedback by viewModel.backupFeedback.collectAsStateWithLifecycle()
    val currentGoogleUser by viewModel.currentGoogleUser.collectAsStateWithLifecycle()
    val cloudSyncState by viewModel.cloudSyncState.collectAsStateWithLifecycle()

    val settings = settingsState
    if (settings == null || !isStartupReady) {
        FrostedBackground {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val strings = StringsBnEn.forLang(settings.language)
    val effectiveNow = System.currentTimeMillis() + settings.developerTimeOffsetMillis

    if (settings.hasPinLock && !isUnlocked) {
        LockScreen(
            strings = strings,
            biometricEnabled = settings.biometricEnabled,
            onVerifyPin = { viewModel.verifyPin(it) },
            onBiometricUnlocked = { viewModel.unlockViaBiometric() }
        )
        return
    }

    if (!settings.onboardingCompleted) {
        OnboardingWhyScreen(
            strings = strings,
            isRecording = isRecording,
            playingAudioPath = playingAudioPath,
            hapticEnabled = settings.hapticFeedbackEnabled,
            cloudSyncState = cloudSyncState,
            onToggleLanguage = viewModel::toggleLanguage,
            onPickPhotoUri = viewModel::copyPickedPhoto,
            onStartRecord = viewModel::startRecordingAudio,
            onStopRecord = viewModel::stopRecordingAudio,
            onTogglePlayAudio = viewModel::togglePlayAudio,
            onSaveWhy = viewModel::saveWhyProfile,
            onRestoreFromGoogleSignIn = {
                viewModel.handleGoogleSignInSuccess(
                    restoredFormatter = strings.cloudRestoreSuccess,
                    syncedFormatter = strings.cloudSyncSuccess,
                    bothExistFormatter = strings.cloudBothExistPrompt,
                    errorMsg = strings.cloudSyncError
                )
            },
            onSkip = viewModel::skipOnboarding
        )
        return
    }

    val navController = rememberNavController()
    var showHubSheet by remember { mutableStateOf(false) }

    val startDestination = if (openCheckRevisionOnLaunch) {
        ReviseRoutes.CHECK_REVISION
    } else {
        ReviseRoutes.HOME
    }

    val totalDueCount = dueQueue.size
    val totalMissedCount = dueQueue.count {
        it.milestone.status == MilestoneStatus.MISSED || effectiveNow > it.milestone.dueAt
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(ReviseRoutes.HOME) {
            HomeScreen(
                strings = strings,
                lang = settings.language,
                whyProfile = whyProfile,
                noteCards = noteCards,
                totalDueCount = totalDueCount,
                totalMissedCount = totalMissedCount,
                streakDays = settings.currentStreak,
                isStudyingFocusMode = settings.isStudyingFocusMode,
                hapticEnabled = settings.hapticFeedbackEnabled,
                playingAudioPath = playingAudioPath,
                onMenuClick = { showHubSheet = true },
                onToggleLang = viewModel::toggleLanguage,
                onToggleFocusMode = viewModel::toggleStudyingFocusMode,
                onOpenCheckRevision = { navController.navigate(ReviseRoutes.CHECK_REVISION) },
                onOpenMyWhy = { navController.navigate(ReviseRoutes.MY_WHY) },
                onOpenNote = { noteId -> navController.navigate(ReviseRoutes.noteDetail(noteId)) },
                onCreateNote = { title, tag, color -> viewModel.createNote(title, tag, color) },
                onUpdateNote = viewModel::updateNote,
                onDeleteNote = { id -> viewModel.deleteNote(id) },
                onTogglePlayAudio = viewModel::togglePlayAudio
            )
        }

        composable(
            route = ReviseRoutes.NOTE_DETAIL,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
            val note by viewModel.observeNote(noteId).collectAsStateWithLifecycle(initialValue = null)
            val entries by viewModel.observeEntriesForNote(noteId).collectAsStateWithLifecycle(initialValue = emptyList())

            NoteDetailScreen(
                note = note,
                entries = entries,
                effectiveNowMillis = effectiveNow,
                strings = strings,
                lang = settings.language,
                isStudyingFocusMode = settings.isStudyingFocusMode,
                hapticEnabled = settings.hapticFeedbackEnabled,
                isRecording = isRecording,
                playingAudioPath = playingAudioPath,
                onBack = { navController.popBackStack() },
                onToggleLang = viewModel::toggleLanguage,
                onToggleFocusMode = viewModel::toggleStudyingFocusMode,
                onUpdateNote = viewModel::updateNote,
                onDeleteNote = { viewModel.deleteNote(noteId) { navController.popBackStack() } },
                onAddEntry = { text, audio, photo -> viewModel.addStudyEntry(noteId, text, audio, photo) },
                onUpdateEntry = { entryId, text, audio, photo -> viewModel.updateStudyEntry(entryId, text, audio, photo) },
                onDeleteEntry = viewModel::deleteStudyEntry,
                onCompleteMilestone = viewModel::completeMilestone,
                onOpenNoteCheckRevision = { navController.navigate(ReviseRoutes.noteCheckRevision(noteId)) },
                onPickPhotoUri = viewModel::copyPickedPhoto,
                onStartRecord = viewModel::startRecordingAudio,
                onStopRecord = viewModel::stopRecordingAudio,
                onTogglePlayAudio = viewModel::togglePlayAudio
            )
        }

        composable(ReviseRoutes.CHECK_REVISION) {
            CheckRevisionScreen(
                dueQueue = dueQueue,
                effectiveNowMillis = effectiveNow,
                strings = strings,
                lang = settings.language,
                isStudyingFocusMode = settings.isStudyingFocusMode,
                hapticEnabled = settings.hapticFeedbackEnabled,
                playingAudioPath = playingAudioPath,
                completionOutcome = completionOutcome,
                scopedNoteTitle = null,
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(ReviseRoutes.HOME)
                    }
                },
                onToggleLang = viewModel::toggleLanguage,
                onToggleFocusMode = viewModel::toggleStudyingFocusMode,
                onCompleteMilestone = viewModel::completeMilestone,
                onDismissCompletion = viewModel::dismissCompletionOutcome,
                onTogglePlayAudio = viewModel::togglePlayAudio
            )
        }

        composable(
            route = ReviseRoutes.NOTE_CHECK_REVISION,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
            val note by viewModel.observeNote(noteId).collectAsStateWithLifecycle(initialValue = null)
            val noteDueQueue = remember(dueQueue, noteId) {
                dueQueue.filter { it.note.id == noteId }
            }
            CheckRevisionScreen(
                dueQueue = noteDueQueue,
                effectiveNowMillis = effectiveNow,
                strings = strings,
                lang = settings.language,
                isStudyingFocusMode = settings.isStudyingFocusMode,
                hapticEnabled = settings.hapticFeedbackEnabled,
                playingAudioPath = playingAudioPath,
                completionOutcome = completionOutcome,
                scopedNoteTitle = note?.title,
                onBack = { navController.popBackStack() },
                onToggleLang = viewModel::toggleLanguage,
                onToggleFocusMode = viewModel::toggleStudyingFocusMode,
                onCompleteMilestone = viewModel::completeMilestone,
                onDismissCompletion = viewModel::dismissCompletionOutcome,
                onTogglePlayAudio = viewModel::togglePlayAudio
            )
        }

        composable(ReviseRoutes.MY_WHY) {
            MyWhyAndLettersScreen(
                whyProfile = whyProfile,
                letters = futureLetters,
                currentStreak = settings.currentStreak,
                strings = strings,
                isStudyingFocusMode = settings.isStudyingFocusMode,
                hapticEnabled = settings.hapticFeedbackEnabled,
                isRecording = isRecording,
                playingAudioPath = playingAudioPath,
                onBack = { navController.popBackStack() },
                onToggleLang = viewModel::toggleLanguage,
                onToggleFocusMode = viewModel::toggleStudyingFocusMode,
                onPickPhotoUri = viewModel::copyPickedPhoto,
                onStartRecord = viewModel::startRecordingAudio,
                onStopRecord = viewModel::stopRecordingAudio,
                onTogglePlayAudio = viewModel::togglePlayAudio,
                onSaveWhy = viewModel::saveWhyProfile,
                onCreateLetter = viewModel::createFutureLetter,
                onDeleteLetter = viewModel::deleteFutureLetter
            )
        }

        composable(ReviseRoutes.AI) {
            AiSettingsScreen(
                settings = settings,
                savedApiKey = savedApiKey,
                connectionState = aiConnectionState,
                previewPair = aiPreviewPair,
                strings = strings,
                onBack = { navController.popBackStack() },
                onToggleLang = viewModel::toggleLanguage,
                onToggleFocusMode = viewModel::toggleStudyingFocusMode,
                onConnectKey = viewModel::connectGeminiKey,
                onAutoSaveKey = viewModel::autoSaveGeminiKey,
                onDisconnectKey = viewModel::disconnectGeminiKey,
                onSaveStylePrompt = viewModel::saveAiStylePrompt,
                onToggleSendFullNote = viewModel::setSendFullNoteToAi,
                onPreviewNotification = viewModel::previewAiNotification
            )
        }

        composable(ReviseRoutes.PROGRESS) {
            ProgressStreakScreen(
                currentStreak = settings.currentStreak,
                bestStreak = settings.bestStreak,
                activities = recentActivity,
                milestones = allMilestones,
                effectiveNowMillis = effectiveNow,
                strings = strings,
                lang = settings.language,
                isStudyingFocusMode = settings.isStudyingFocusMode,
                onBack = { navController.popBackStack() },
                onToggleLang = viewModel::toggleLanguage,
                onToggleFocusMode = viewModel::toggleStudyingFocusMode
            )
        }

        composable(ReviseRoutes.NOTIFICATIONS) {
            NotificationSettingsScreen(
                settings = settings,
                strings = strings,
                onBack = { navController.popBackStack() },
                onToggleLang = viewModel::toggleLanguage,
                onToggleFocusMode = viewModel::toggleStudyingFocusMode,
                onSetDailyCap = viewModel::setDailyNotificationCap,
                onSetQuietHours = viewModel::setQuietHours,
                onClearSnooze = viewModel::clearSnooze,
                onResetAdaptive = viewModel::resetAdaptiveThrottle,
                onTestCheckNow = viewModel::triggerImmediateNotificationCheck
            )
        }

        composable(ReviseRoutes.LOOK_AND_FEEL) {
            LookAndFeelScreen(
                settings = settings,
                strings = strings,
                onBack = { navController.popBackStack() },
                onToggleLang = viewModel::toggleLanguage,
                onToggleFocusMode = viewModel::toggleStudyingFocusMode,
                onSetThemeMode = viewModel::setThemeMode,
                onSetAccentPreset = viewModel::setAccentPreset,
                onSetFontScale = viewModel::setFontScale,
                onSetSoundFeedback = viewModel::setSoundFeedback,
                onSetHapticFeedback = viewModel::setHapticFeedback,
                onUnlockDeveloperMenu = viewModel::unlockDeveloperMenu,
                onSetDeveloperOffset = viewModel::setDeveloperTimeOffset
            )
        }

        composable(ReviseRoutes.BACKUP) {
            BackupAndSecurityScreen(
                startOnSecurity = false,
                settings = settings,
                backupFeedback = backupFeedback,
                currentGoogleUser = currentGoogleUser,
                cloudSyncState = cloudSyncState,
                strings = strings,
                onBack = { navController.popBackStack() },
                onToggleLang = viewModel::toggleLanguage,
                onToggleFocusMode = viewModel::toggleStudyingFocusMode,
                onExportBackup = { uri, pass ->
                    viewModel.exportEncryptedBackup(uri, pass, strings.exportSuccess, strings.backupError)
                },
                onImportBackup = { uri, pass ->
                    viewModel.importEncryptedBackup(uri, pass, strings.importSuccess, strings.backupError)
                },
                onBackupValidationError = viewModel::showBackupValidationError,
                onClearBackupMessage = viewModel::clearBackupMessage,
                onGoogleSignInSuccess = {
                    viewModel.handleGoogleSignInSuccess(
                        restoredFormatter = strings.cloudRestoreSuccess,
                        syncedFormatter = strings.cloudSyncSuccess,
                        bothExistFormatter = strings.cloudBothExistPrompt,
                        errorMsg = strings.cloudSyncError
                    )
                },
                onSyncToCloud = {
                    viewModel.syncToCloud(strings.cloudSyncSuccess, strings.cloudEmptyGuardMsg, strings.cloudSyncError)
                },
                onRestoreFromCloud = {
                    viewModel.restoreFromCloud(strings.cloudRestoreSuccess, strings.cloudSyncError)
                },
                onCloudAuthError = viewModel::setCloudSyncError,
                onClearCloudSyncStatus = viewModel::clearCloudSyncStatus,
                onSetPinLock = viewModel::setPinLock,
                onSetBiometricEnabled = viewModel::setBiometricEnabled,
                onDeleteAllData = viewModel::deleteAllData
            )
        }

        composable(ReviseRoutes.SECURITY) {
            BackupAndSecurityScreen(
                startOnSecurity = true,
                settings = settings,
                backupFeedback = backupFeedback,
                currentGoogleUser = currentGoogleUser,
                cloudSyncState = cloudSyncState,
                strings = strings,
                onBack = { navController.popBackStack() },
                onToggleLang = viewModel::toggleLanguage,
                onToggleFocusMode = viewModel::toggleStudyingFocusMode,
                onExportBackup = { uri, pass ->
                    viewModel.exportEncryptedBackup(uri, pass, strings.exportSuccess, strings.backupError)
                },
                onImportBackup = { uri, pass ->
                    viewModel.importEncryptedBackup(uri, pass, strings.importSuccess, strings.backupError)
                },
                onBackupValidationError = viewModel::showBackupValidationError,
                onClearBackupMessage = viewModel::clearBackupMessage,
                onGoogleSignInSuccess = {
                    viewModel.handleGoogleSignInSuccess(
                        restoredFormatter = strings.cloudRestoreSuccess,
                        syncedFormatter = strings.cloudSyncSuccess,
                        bothExistFormatter = strings.cloudBothExistPrompt,
                        errorMsg = strings.cloudSyncError
                    )
                },
                onSyncToCloud = {
                    viewModel.syncToCloud(strings.cloudSyncSuccess, strings.cloudEmptyGuardMsg, strings.cloudSyncError)
                },
                onRestoreFromCloud = {
                    viewModel.restoreFromCloud(strings.cloudRestoreSuccess, strings.cloudSyncError)
                },
                onCloudAuthError = viewModel::setCloudSyncError,
                onClearCloudSyncStatus = viewModel::clearCloudSyncStatus,
                onSetPinLock = viewModel::setPinLock,
                onSetBiometricEnabled = viewModel::setBiometricEnabled,
                onDeleteAllData = viewModel::deleteAllData
            )
        }
    }

    if (showHubSheet) {
        HamburgerHubSheet(
            strings = strings,
            currentGoogleUser = currentGoogleUser,
            onDismiss = { showHubSheet = false },
            onSelect = { dest ->
                showHubSheet = false
                when (dest) {
                    HubDestination.HOME -> navController.navigate(ReviseRoutes.HOME) {
                        popUpTo(ReviseRoutes.HOME) { inclusive = true }
                    }
                    HubDestination.CHECK_REVISION -> navController.navigate(ReviseRoutes.CHECK_REVISION)
                    HubDestination.MY_WHY -> navController.navigate(ReviseRoutes.MY_WHY)
                    HubDestination.AI -> navController.navigate(ReviseRoutes.AI)
                    HubDestination.LANGUAGE_TOGGLE -> viewModel.toggleLanguage()
                    HubDestination.PROGRESS -> navController.navigate(ReviseRoutes.PROGRESS)
                    HubDestination.NOTIFICATIONS -> navController.navigate(ReviseRoutes.NOTIFICATIONS)
                    HubDestination.LOOK_AND_FEEL -> navController.navigate(ReviseRoutes.LOOK_AND_FEEL)
                    HubDestination.BACKUP -> navController.navigate(ReviseRoutes.BACKUP)
                    HubDestination.SECURITY -> navController.navigate(ReviseRoutes.SECURITY)
                }
            }
        )
    }
}
