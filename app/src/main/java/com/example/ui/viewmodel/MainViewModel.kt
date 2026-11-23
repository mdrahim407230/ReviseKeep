package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ReviseKeepApp
import com.example.data.ai.AiConnectResult
import com.example.data.cloud.authStateFlow
import com.example.data.local.DueRevisionItem
import com.example.data.local.EntryWithMilestones
import com.example.data.local.FutureLetterEntity
import com.example.data.local.NoteCardSummary
import com.example.data.local.NoteEntity
import com.example.data.local.RevisionActivityEntity
import com.example.data.local.RevisionMilestoneEntity
import com.example.data.local.WhyProfileEntity
import com.example.data.prefs.AccentPreset
import com.example.data.prefs.AppSettings
import com.example.data.prefs.ThemeMode
import com.example.data.repository.CompletionOutcome
import com.example.domain.RevisionEngine
import com.example.i18n.AppLang
import com.example.notifications.NotificationHelper
import com.example.notifications.RevisionCheckWorker
import com.example.data.repository.ReviseKeepRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AiConnectionUiState {
    data object Idle : AiConnectionUiState
    data object Connecting : AiConnectionUiState
    data class Connected(val modelId: String) : AiConnectionUiState
    data class Error(val message: String) : AiConnectionUiState
}

sealed interface CloudSyncUiState {
    data object Idle : CloudSyncUiState
    data object Syncing : CloudSyncUiState
    data class Success(val message: String) : CloudSyncUiState
    data class Error(val message: String) : CloudSyncUiState
}

data class BackupFeedback(
    val message: String,
    val isError: Boolean
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as ReviseKeepApp).repository

    val settings: StateFlow<AppSettings?> = repo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isStartupReady = MutableStateFlow(false)
    val isStartupReady: StateFlow<Boolean> = _isStartupReady.asStateFlow()

    val currentGoogleUser: StateFlow<FirebaseUser?> = Firebase.auth.authStateFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Firebase.auth.currentUser)

    val whyProfile: StateFlow<WhyProfileEntity?> = repo.whyProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val noteCards: StateFlow<List<NoteCardSummary>> = repo.noteCardsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val serialDueQueue: StateFlow<List<DueRevisionItem>> = repo.serialDueQueueFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val futureLetters: StateFlow<List<FutureLetterEntity>> = repo.futureLettersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentActivity: StateFlow<List<RevisionActivityEntity>> = repo.recentActivityFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMilestones: StateFlow<List<RevisionMilestoneEntity>> = repo.allMilestonesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _savedApiKey = MutableStateFlow("")
    val savedApiKey: StateFlow<String> = _savedApiKey.asStateFlow()

    private val _aiConnectionState = MutableStateFlow<AiConnectionUiState>(AiConnectionUiState.Idle)
    val aiConnectionState: StateFlow<AiConnectionUiState> = _aiConnectionState.asStateFlow()

    private val _aiPreviewPair = MutableStateFlow<Pair<String, String>?>(null)
    val aiPreviewPair: StateFlow<Pair<String, String>?> = _aiPreviewPair.asStateFlow()

    private val _completionOutcome = MutableStateFlow<CompletionOutcome?>(null)
    val completionOutcome: StateFlow<CompletionOutcome?> = _completionOutcome.asStateFlow()

    private val _isRecordingAudio = MutableStateFlow(false)
    val isRecordingAudio: StateFlow<Boolean> = _isRecordingAudio.asStateFlow()

    private val _playingAudioPath = MutableStateFlow<String?>(null)
    val playingAudioPath: StateFlow<String?> = _playingAudioPath.asStateFlow()

    private val _backupFeedback = MutableStateFlow<BackupFeedback?>(null)
    val backupFeedback: StateFlow<BackupFeedback?> = _backupFeedback.asStateFlow()

    private val _cloudSyncState = MutableStateFlow<CloudSyncUiState>(CloudSyncUiState.Idle)
    val cloudSyncState: StateFlow<CloudSyncUiState> = _cloudSyncState.asStateFlow()

    private var autoSyncJob: Job? = null

    init {
        viewModelScope.launch {
            val currentSettings = repo.prefs.getSettingsSync()
            _isUnlocked.value = !currentSettings.hasPinLock
            val decryptedKey = repo.prefs.getDecryptedApiKey()
            if (decryptedKey.isNotBlank()) {
                _savedApiKey.value = decryptedKey
                _aiConnectionState.value = AiConnectionUiState.Connected(
                    currentSettings.cachedModelId.ifBlank { "gemini" }
                )
            } else if (currentSettings.hasApiKey) {
                repo.disconnectApiKey()
                _savedApiKey.value = ""
            }
            _isStartupReady.value = true
            repo.sweepOverdueMilestones()
            if (Firebase.auth.currentUser != null) {
                if (repo.hasLocalStudyData()) {
                    repo.syncToCloud(allowEmptyOverwrite = false)
                } else {
                    repo.onSignedInSmartSync()
                    refreshAiStateFromPrefs()
                }
            }
        }
    }

    private suspend fun refreshAiStateFromPrefs() {
        val currentSettings = repo.prefs.getSettingsSync()
        val decryptedKey = repo.prefs.getDecryptedApiKey()
        _savedApiKey.value = decryptedKey
        if (decryptedKey.isNotBlank() && _aiConnectionState.value !is AiConnectionUiState.Connected) {
            if (currentSettings.cachedModelId.isNotBlank()) {
                _aiConnectionState.value = AiConnectionUiState.Connected(currentSettings.cachedModelId)
            }
        } else if (decryptedKey.isBlank()) {
            _aiConnectionState.value = AiConnectionUiState.Idle
        }
    }

    private fun scheduleAutoCloudSync() {
        if (currentGoogleUser.value == null && Firebase.auth.currentUser == null) return
        autoSyncJob?.cancel()
        val app = getApplication<ReviseKeepApp>()
        autoSyncJob = app.appScope.launch {
            delay(400L)
            if (Firebase.auth.currentUser != null) {
                repo.syncToCloud(allowEmptyOverwrite = false)
            }
        }
    }

    fun syncInBackgroundOnStop() {
        if (Firebase.auth.currentUser == null) return
        autoSyncJob?.cancel()
        val app = getApplication<ReviseKeepApp>()
        app.appScope.launch {
            if (Firebase.auth.currentUser != null) {
                repo.syncToCloud(allowEmptyOverwrite = false)
            }
        }
    }

    private fun currentSettingsOrDefault(): AppSettings = settings.value ?: AppSettings()

    fun sweepOverdueOnResume() {
        viewModelScope.launch {
            repo.sweepOverdueMilestones()
        }
    }

    fun observeNote(noteId: Long): Flow<NoteEntity?> = repo.observeNote(noteId)

    fun observeEntriesForNote(noteId: Long): Flow<List<EntryWithMilestones>> =
        repo.observeEntriesWithMilestones(noteId)

    fun toggleLanguage() {
        viewModelScope.launch {
            val next = currentSettingsOrDefault().language.toggled()
            repo.setLanguage(next)
            scheduleAutoCloudSync()
        }
    }

    fun toggleStudyingFocusMode() {
        viewModelScope.launch {
            val next = !currentSettingsOrDefault().isStudyingFocusMode
            repo.prefs.setStudyingFocusMode(next)
            if (next) {
                NotificationHelper.cancelRevisionNotification(getApplication())
            }
        }
    }

    fun createNote(title: String, subjectTag: String, colorIndex: Int, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repo.createNote(title, subjectTag, colorIndex)
            onCreated(id)
            scheduleAutoCloudSync()
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            repo.updateNote(note)
            scheduleAutoCloudSync()
        }
    }

    fun deleteNote(noteId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repo.deleteNote(noteId)
            onDeleted()
            scheduleAutoCloudSync()
        }
    }

    fun addStudyEntry(noteId: Long, text: String, audioPath: String?, photoPath: String?) {
        if (text.isBlank() && audioPath.isNullOrBlank() && photoPath.isNullOrBlank()) return
        viewModelScope.launch {
            repo.addStudyEntry(noteId, text, audioPath, photoPath)
            scheduleAutoCloudSync()
        }
    }

    fun updateStudyEntry(entryId: Long, text: String, audioPath: String?, photoPath: String?) {
        if (text.isBlank() && audioPath.isNullOrBlank() && photoPath.isNullOrBlank()) return
        viewModelScope.launch {
            repo.updateStudyEntry(entryId, text, audioPath, photoPath)
            scheduleAutoCloudSync()
        }
    }

    fun deleteStudyEntry(entryId: Long) {
        viewModelScope.launch {
            repo.deleteStudyEntry(entryId)
            scheduleAutoCloudSync()
        }
    }

    fun completeMilestone(milestoneId: Long) {
        viewModelScope.launch {
            val outcome = repo.completeMilestone(milestoneId)
            if (currentSettingsOrDefault().soundFeedbackEnabled) {
                repo.media.playGentleChime()
            }
            _completionOutcome.value = outcome
            scheduleAutoCloudSync()
        }
    }

    fun dismissCompletionOutcome() {
        _completionOutcome.value = null
    }

    fun saveWhyProfile(
        studentName: String,
        whyText: String,
        whoForText: String,
        dreamText: String,
        photoPath: String?,
        voicePath: String?
    ) {
        viewModelScope.launch {
            repo.saveWhyProfile(studentName, whyText, whoForText, dreamText, photoPath, voicePath)
            scheduleAutoCloudSync()
        }
    }

    fun skipOnboarding() {
        viewModelScope.launch {
            repo.prefs.setOnboardingCompleted(true)
        }
    }

    fun createFutureLetter(unlockStreakDays: Int, title: String, body: String) {
        if (body.isBlank()) return
        viewModelScope.launch {
            repo.createFutureLetter(unlockStreakDays, title, body)
            scheduleAutoCloudSync()
        }
    }

    fun deleteFutureLetter(id: Long) {
        viewModelScope.launch {
            repo.deleteFutureLetter(id)
            scheduleAutoCloudSync()
        }
    }

    fun copyPickedPhoto(uri: Uri, onCopied: (String?) -> Unit) {
        viewModelScope.launch {
            val path = repo.media.copyImageFromUri(uri)
            onCopied(path)
        }
    }

    fun startRecordingAudio() {
        val started = repo.media.startRecording()
        _isRecordingAudio.value = started != null
    }

    fun stopRecordingAudio(onRecorded: (String?) -> Unit) {
        val path = repo.media.stopRecording()
        _isRecordingAudio.value = false
        onRecorded(path)
    }

    fun togglePlayAudio(path: String) {
        if (_playingAudioPath.value == path) {
            repo.media.stopAudio()
            _playingAudioPath.value = null
        } else {
            _playingAudioPath.value = path
            repo.media.playAudio(path) {
                _playingAudioPath.value = null
            }
        }
    }

    fun autoSaveGeminiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repo.autoSaveGeminiApiKey(trimmed)
            _savedApiKey.value = repo.prefs.getDecryptedApiKey().ifBlank { trimmed }
            scheduleAutoCloudSync()
        }
    }

    fun connectGeminiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            _aiConnectionState.value = AiConnectionUiState.Connecting
            try {
                when (val res = repo.connectGeminiApiKey(trimmed)) {
                    is AiConnectResult.Success -> {
                        _savedApiKey.value = repo.prefs.getDecryptedApiKey().ifBlank { trimmed }
                        _aiConnectionState.value = AiConnectionUiState.Connected(res.modelId)
                        scheduleAutoCloudSync()
                    }
                    is AiConnectResult.Failure -> {
                        _savedApiKey.value = repo.prefs.getDecryptedApiKey().ifBlank { trimmed }
                        _aiConnectionState.value = AiConnectionUiState.Error(res.message)
                        scheduleAutoCloudSync()
                    }
                }
            } catch (e: Exception) {
                _aiConnectionState.value = AiConnectionUiState.Error(e.localizedMessage ?: "Failed to save key")
            }
        }
    }

    fun disconnectGeminiKey() {
        viewModelScope.launch {
            repo.disconnectApiKey()
            _savedApiKey.value = ""
            _aiConnectionState.value = AiConnectionUiState.Idle
            scheduleAutoCloudSync()
        }
    }

    fun saveAiStylePrompt(prompt: String) {
        viewModelScope.launch {
            repo.prefs.saveAiStylePrompt(prompt)
            scheduleAutoCloudSync()
        }
    }

    fun setSendFullNoteToAi(enabled: Boolean) {
        viewModelScope.launch {
            repo.prefs.setSendFullNoteToAi(enabled)
            scheduleAutoCloudSync()
        }
    }

    fun previewAiNotification() {
        viewModelScope.launch {
            val due = repo.getAllDueItemsSync()
            val cur = currentSettingsOrDefault()
            val pair = if (due.isNotEmpty()) {
                repo.generateSmartNotificationText(due)
            } else {
                val lang = cur.language
                val why = whyProfile.value
                RevisionEngine.buildFallbackNotificationText(
                    lang = lang,
                    dueTitles = listOf(if (lang == AppLang.BN) "পদার্থবিজ্ঞান" else "Physics"),
                    hasMissed = false,
                    missedCount = 0,
                    dueCount = 1,
                    whyText = why?.whyText.orEmpty(),
                    whoForText = why?.whoForText.orEmpty(),
                    streakDays = cur.currentStreak
                )
            }
            _aiPreviewPair.value = pair
        }
    }

    fun setDailyNotificationCap(cap: Int) {
        viewModelScope.launch { repo.prefs.setDailyNotificationCap(cap) }
    }

    fun setQuietHours(startHour: Int, endHour: Int) {
        viewModelScope.launch { repo.prefs.setQuietHours(startHour, endHour) }
    }

    fun clearSnooze() {
        viewModelScope.launch { repo.prefs.setSnoozedUntil(0L) }
    }

    fun resetAdaptiveThrottle() {
        viewModelScope.launch {
            repo.prefs.resetDismissalThrottle()
            RevisionCheckWorker.schedulePeriodic(getApplication(), throttled = false)
        }
    }

    fun triggerImmediateNotificationCheck() {
        RevisionCheckWorker.triggerImmediateCheck(getApplication())
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repo.setThemeMode(mode) }
    }

    fun setAccentPreset(preset: AccentPreset) {
        viewModelScope.launch { repo.setAccentPreset(preset) }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch { repo.prefs.setFontScale(scale) }
    }

    fun setSoundFeedback(enabled: Boolean) {
        viewModelScope.launch { repo.prefs.setSoundFeedback(enabled) }
    }

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { repo.prefs.setHapticFeedback(enabled) }
    }

    fun unlockDeveloperMenu() {
        viewModelScope.launch { repo.prefs.unlockDeveloperMenu() }
    }

    fun setDeveloperTimeOffset(offsetMillis: Long) {
        viewModelScope.launch {
            repo.prefs.setDeveloperTimeOffset(offsetMillis)
            repo.sweepOverdueMilestones()
        }
    }

    fun setPinLock(pin: String) {
        viewModelScope.launch {
            repo.prefs.setPinLock(pin)
            if (pin.isBlank()) _isUnlocked.value = true
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { repo.prefs.setBiometricEnabled(enabled) }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val ok = repo.verifyPin(pin)
        if (ok) _isUnlocked.value = true
        return ok
    }

    fun unlockViaBiometric() {
        _isUnlocked.value = true
    }

    fun showBackupValidationError(message: String) {
        _backupFeedback.value = BackupFeedback(message, isError = true)
    }

    fun exportEncryptedBackup(uri: Uri, passphrase: String, successMsg: String, errorMsg: String) {
        viewModelScope.launch {
            val ok = repo.exportEncryptedBackup(uri, passphrase)
            _backupFeedback.value = BackupFeedback(if (ok) successMsg else errorMsg, isError = !ok)
        }
    }

    fun importEncryptedBackup(uri: Uri, passphrase: String, successMsg: String, errorMsg: String) {
        viewModelScope.launch {
            val ok = repo.importEncryptedBackup(uri, passphrase)
            if (ok) {
                repo.sweepOverdueMilestones()
                refreshAiStateFromPrefs()
                scheduleAutoCloudSync()
            }
            _backupFeedback.value = BackupFeedback(if (ok) successMsg else errorMsg, isError = !ok)
        }
    }

    fun clearBackupMessage() {
        _backupFeedback.value = null
    }

    fun syncToCloud(
        successFormatter: (Int, Int) -> String,
        emptyGuardMsg: String,
        errorMsg: String
    ) {
        viewModelScope.launch {
            _cloudSyncState.value = CloudSyncUiState.Syncing
            val res = repo.syncToCloud(allowEmptyOverwrite = false)
            _cloudSyncState.value = res.fold(
                onSuccess = { summary ->
                    CloudSyncUiState.Success(successFormatter(summary.notesCount, summary.entriesCount))
                },
                onFailure = { err ->
                    val msg = if (err.message == "EMPTY_DEVICE_CLOUD_GUARD") {
                        emptyGuardMsg
                    } else {
                        err.localizedMessage?.takeIf { it.isNotBlank() } ?: errorMsg
                    }
                    CloudSyncUiState.Error(msg)
                }
            )
        }
    }

    fun handleGoogleSignInSuccess(
        restoredFormatter: (Int, Int) -> String,
        syncedFormatter: (Int, Int) -> String,
        bothExistFormatter: (Int, Int) -> String,
        errorMsg: String
    ) {
        viewModelScope.launch {
            _cloudSyncState.value = CloudSyncUiState.Syncing
            val res = repo.onSignedInSmartSync()
            _cloudSyncState.value = res.fold(
                onSuccess = { outcome ->
                    when (outcome) {
                        is ReviseKeepRepository.SmartSyncOutcome.Restored -> {
                            refreshAiStateFromPrefs()
                            CloudSyncUiState.Success(restoredFormatter(outcome.notesCount, outcome.entriesCount))
                        }
                        is ReviseKeepRepository.SmartSyncOutcome.Uploaded ->
                            CloudSyncUiState.Success(syncedFormatter(outcome.notesCount, outcome.entriesCount))
                        is ReviseKeepRepository.SmartSyncOutcome.BothHaveData ->
                            CloudSyncUiState.Success(bothExistFormatter(outcome.cloudNotesCount, outcome.cloudEntriesCount))
                        ReviseKeepRepository.SmartSyncOutcome.UpToDate ->
                            CloudSyncUiState.Idle
                    }
                },
                onFailure = { err ->
                    CloudSyncUiState.Error(err.localizedMessage?.takeIf { it.isNotBlank() } ?: errorMsg)
                }
            )
        }
    }

    fun restoreFromCloud(successFormatter: (Int, Int) -> String, errorMsg: String) {
        viewModelScope.launch {
            _cloudSyncState.value = CloudSyncUiState.Syncing
            val res = repo.restoreFromCloud()
            _cloudSyncState.value = res.fold(
                onSuccess = { (notesCount, entriesCount) ->
                    refreshAiStateFromPrefs()
                    CloudSyncUiState.Success(successFormatter(notesCount, entriesCount))
                },
                onFailure = { err ->
                    CloudSyncUiState.Error(err.localizedMessage?.takeIf { it.isNotBlank() } ?: errorMsg)
                }
            )
        }
    }

    fun setCloudSyncError(message: String) {
        _cloudSyncState.value = CloudSyncUiState.Error(message)
    }

    fun clearCloudSyncStatus() {
        _cloudSyncState.value = CloudSyncUiState.Idle
    }

    fun deleteAllData() {
        viewModelScope.launch {
            repo.deleteAllUserData()
            _savedApiKey.value = ""
            _aiConnectionState.value = AiConnectionUiState.Idle
            _aiPreviewPair.value = null
            _isUnlocked.value = true
        }
    }
}
