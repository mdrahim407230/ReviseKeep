package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.ai.AiConnectResult
import com.example.data.ai.GeminiAiClient
import com.example.data.backup.BackupManager
import com.example.data.local.DueRevisionItem
import com.example.data.local.EntryWithMilestones
import com.example.data.local.FutureLetterEntity
import com.example.data.local.MilestoneStatus
import com.example.data.local.NoteCardSummary
import com.example.data.local.NoteEntity
import com.example.data.local.ReviseKeepDatabase
import com.example.data.local.RevisionActivityEntity
import com.example.data.local.RevisionMilestoneEntity
import com.example.data.local.StudyEntryEntity
import com.example.data.local.WhyProfileEntity
import com.example.data.media.MediaHelper
import com.example.data.prefs.AccentPreset
import com.example.data.prefs.AppPreferencesRepository
import com.example.data.prefs.AppSettings
import com.example.data.prefs.ThemeMode
import com.example.data.security.CryptoManager
import com.example.domain.RevisionEngine
import com.example.domain.RevisionStage
import com.example.i18n.AppLang
import androidx.room.withTransaction
import com.example.data.cloud.CloudSyncRepository
import com.example.data.cloud.CloudSyncSummary
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class CompletionOutcome(
    val message: String,
    val unlockedLetterTitles: List<String> = emptyList()
)

class ReviseKeepRepository(private val context: Context) {
    private val db = ReviseKeepDatabase.getInstance(context)
    val prefs = AppPreferencesRepository(context)
    val media = MediaHelper(context)
    private val aiClient = GeminiAiClient(context)
    private val backupManager = BackupManager(context, db)
    private val cloudSync = CloudSyncRepository(context)

    val settingsFlow: Flow<AppSettings> = prefs.settingsFlow
    val whyProfileFlow: Flow<WhyProfileEntity?> = db.whyProfileDao().observeWhyProfile()
    val futureLettersFlow: Flow<List<FutureLetterEntity>> = db.futureLetterDao().observeLetters()
    val recentActivityFlow: Flow<List<RevisionActivityEntity>> = db.revisionActivityDao().observeRecentActivity()
    val allMilestonesFlow: Flow<List<RevisionMilestoneEntity>> = db.milestoneDao().observeAllMilestones()

    val noteCardsFlow: Flow<List<NoteCardSummary>> = combine(
        db.noteDao().observeAllNotes(),
        db.studyEntryDao().observeAllEntries(),
        db.milestoneDao().observeAllMilestones(),
        prefs.settingsFlow
    ) { notes, entries, milestones, settings ->
        val effectiveNow = System.currentTimeMillis() + settings.developerTimeOffsetMillis
        val entriesByNote = entries.groupBy { it.noteId }
        val milestonesByNote = milestones.groupBy { it.noteId }

        notes.map { note ->
            val noteEntries = entriesByNote[note.id].orEmpty()
            val noteMilestones = milestonesByNote[note.id].orEmpty()
            val missedCount = noteMilestones.count {
                it.status == MilestoneStatus.MISSED ||
                    (it.status == MilestoneStatus.ACTIVE_DUE && effectiveNow > it.dueAt)
            }
            val dueCount = noteMilestones.count {
                it.status == MilestoneStatus.ACTIVE_DUE && it.dueAt <= effectiveNow
            }
            val nextDueAt = noteMilestones
                .filter { it.status == MilestoneStatus.ACTIVE_DUE || it.status == MilestoneStatus.MISSED }
                .minOfOrNull { it.dueAt }
            val completedCount = noteMilestones.count { it.status == MilestoneStatus.COMPLETED }
            NoteCardSummary(
                note = note,
                entryCount = noteEntries.size,
                dueCount = dueCount + missedCount,
                missedCount = missedCount,
                nextDueAt = nextDueAt,
                completedStagesCount = completedCount
            )
        }
    }

    val serialDueQueueFlow: Flow<List<DueRevisionItem>> = combine(
        db.noteDao().observeAllNotes(),
        db.studyEntryDao().observeAllEntries(),
        db.milestoneDao().observeAllMilestones(),
        prefs.settingsFlow
    ) { notes, entries, milestones, settings ->
        val effectiveNow = System.currentTimeMillis() + settings.developerTimeOffsetMillis
        val noteMap = notes.associateBy { it.id }
        val entryMap = entries.associateBy { it.id }
        val joined = milestones.mapNotNull { m ->
            val entry = entryMap[m.entryId] ?: return@mapNotNull null
            val note = noteMap[m.noteId] ?: return@mapNotNull null
            DueRevisionItem(milestone = m, entry = entry, note = note)
        }
        RevisionEngine.sortSerialDueQueue(joined, effectiveNow)
    }

    fun observeNote(noteId: Long): Flow<NoteEntity?> = db.noteDao().observeNoteById(noteId)

    fun observeEntriesWithMilestones(noteId: Long): Flow<List<EntryWithMilestones>> =
        db.studyEntryDao().observeEntriesWithMilestones(noteId)

    suspend fun effectiveNowMillis(): Long {
        val offset = prefs.getSettingsSync().developerTimeOffsetMillis
        return System.currentTimeMillis() + offset
    }

    /**
     * Sweeps ACTIVE_DUE milestones that passed dueAt and marks them MISSED (never deletes them).
     */
    suspend fun sweepOverdueMilestones(): List<RevisionMilestoneEntity> {
        val now = effectiveNowMillis()
        val all = db.milestoneDao().getAllMilestonesSync()
        val newlyMissed = RevisionEngine.findNewlyMissedMilestones(all, now)
        if (newlyMissed.isNotEmpty()) {
            db.milestoneDao().updateMilestones(newlyMissed)
        }
        return newlyMissed
    }

    suspend fun createNote(title: String, subjectTag: String, colorIndex: Int): Long {
        val cleanTitle = RevisionEngine.sanitizeInput(title).ifBlank { "Study Note" }
        val cleanTag = RevisionEngine.sanitizeInput(subjectTag)
        val now = effectiveNowMillis()
        return db.noteDao().insertNote(
            NoteEntity(
                title = cleanTitle,
                subjectTag = cleanTag,
                colorIndex = colorIndex,
                isPinned = false,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateNote(note: NoteEntity) {
        val now = effectiveNowMillis()
        db.noteDao().updateNote(
            note.copy(
                title = RevisionEngine.sanitizeInput(note.title).ifBlank { note.title },
                subjectTag = RevisionEngine.sanitizeInput(note.subjectTag),
                updatedAt = now
            )
        )
    }

    suspend fun deleteNote(noteId: Long) {
        db.noteDao().deleteNoteById(noteId)
    }

    suspend fun addStudyEntry(
        noteId: Long,
        contentText: String,
        audioPath: String?,
        photoPath: String?
    ): Long {
        val cleanText = RevisionEngine.sanitizeInput(contentText)
        val createdAt = effectiveNowMillis()
        val entryId = db.studyEntryDao().insertEntry(
            StudyEntryEntity(
                noteId = noteId,
                contentText = cleanText,
                audioPath = audioPath,
                photoPath = photoPath,
                createdAt = createdAt
            )
        )

        val note = db.noteDao().getNoteByIdSync(noteId)
        if (note != null) {
            db.noteDao().updateNote(note.copy(updatedAt = createdAt))
        }

        // Stage 0 (2h) starts immediately from entry creation; stages 1..15 are LOCKED until previous stage completes.
        val milestones = RevisionStage.entries.map { stage ->
            if (stage.index == 0) {
                RevisionMilestoneEntity(
                    entryId = entryId,
                    noteId = noteId,
                    stageIndex = 0,
                    status = MilestoneStatus.ACTIVE_DUE,
                    unlockedAt = createdAt,
                    dueAt = stage.calculateDueFrom(createdAt)
                )
            } else {
                RevisionMilestoneEntity(
                    entryId = entryId,
                    noteId = noteId,
                    stageIndex = stage.index,
                    status = MilestoneStatus.LOCKED,
                    unlockedAt = 0L,
                    dueAt = 0L
                )
            }
        }
        db.milestoneDao().insertAll(milestones)
        return entryId
    }

    suspend fun updateStudyEntry(
        entryId: Long,
        contentText: String,
        audioPath: String?,
        photoPath: String?
    ) {
        val existing = db.studyEntryDao().getEntryByIdSync(entryId) ?: return
        val cleanText = RevisionEngine.sanitizeInput(contentText)
        if (cleanText.isBlank() && audioPath.isNullOrBlank() && photoPath.isNullOrBlank()) return
        db.studyEntryDao().updateEntry(
            existing.copy(
                contentText = cleanText,
                audioPath = audioPath,
                photoPath = photoPath
            )
        )
        val now = effectiveNowMillis()
        val note = db.noteDao().getNoteByIdSync(existing.noteId)
        if (note != null) {
            db.noteDao().updateNote(note.copy(updatedAt = now))
        }
    }

    suspend fun deleteStudyEntry(entryId: Long) {
        val existing = db.studyEntryDao().getEntryByIdSync(entryId) ?: return
        db.studyEntryDao().deleteEntryById(entryId)
        val now = effectiveNowMillis()
        val note = db.noteDao().getNoteByIdSync(existing.noteId)
        if (note != null) {
            db.noteDao().updateNote(note.copy(updatedAt = now))
        }
    }

    suspend fun completeMilestone(milestoneId: Long): CompletionOutcome {
        val now = effectiveNowMillis()
        val all = db.milestoneDao().getAllMilestonesSync()
        val target = all.firstOrNull { it.id == milestoneId }
            ?: return CompletionOutcome(message = "Done")

        if (target.status == MilestoneStatus.COMPLETED) {
            return CompletionOutcome(message = "Already completed")
        }

        val entryMilestones = db.milestoneDao().getMilestonesForEntry(target.entryId)
        val completedTarget = target.copy(
            status = MilestoneStatus.COMPLETED,
            completedAt = now
        )
        val toUpdate = mutableListOf(completedTarget)

        val currentStage = RevisionStage.fromIndex(target.stageIndex)
        val nextStage = currentStage.nextStage()
        if (nextStage != null) {
            val nextEntity = entryMilestones.firstOrNull { it.stageIndex == nextStage.index }
            if (nextEntity != null && nextEntity.status == MilestoneStatus.LOCKED) {
                toUpdate.add(
                    nextEntity.copy(
                        status = MilestoneStatus.ACTIVE_DUE,
                        unlockedAt = now,
                        dueAt = nextStage.calculateDueFrom(now)
                    )
                )
            }
        }
        db.milestoneDao().updateMilestones(toUpdate)

        // Update daily activity + streak
        val todayKey = RevisionEngine.dayKey(now)
        val existingDay = db.revisionActivityDao().getByDayKey(todayKey)
        db.revisionActivityDao().upsertActivity(
            RevisionActivityEntity(
                dayKey = todayKey,
                completedCount = (existingDay?.completedCount ?: 0) + 1
            )
        )

        val settings = prefs.getSettingsSync()
        val yesterdayKey = RevisionEngine.previousDayKey(now)
        val newStreak = when (settings.lastRevisionDayKey) {
            todayKey -> settings.currentStreak.coerceAtLeast(1)
            yesterdayKey -> settings.currentStreak + 1
            else -> 1
        }
        val newBest = maxOf(settings.bestStreak, newStreak)
        prefs.updateStreakStats(newStreak, newBest, todayKey)

        // Unlock Future Self letters when streak reaches 3, 7, 30 days
        val letters = db.futureLetterDao().getAllLettersSync()
        val newlyUnlocked = letters.filter { !it.isUnlocked && newStreak >= it.unlockStreakDays }
            .map { it.copy(isUnlocked = true, unlockedAt = now) }
        if (newlyUnlocked.isNotEmpty()) {
            db.futureLetterDao().updateLetters(newlyUnlocked)
        }

        val why = db.whyProfileDao().getWhyProfileSync()
        val nextLabel = nextStage?.label(settings.language)
        val message = RevisionEngine.buildCompletionMessage(
            lang = settings.language,
            whyText = why?.whyText.orEmpty(),
            whoForText = why?.whoForText.orEmpty(),
            studentName = why?.studentName.orEmpty(),
            nextStageLabel = nextLabel
        )
        return CompletionOutcome(
            message = message,
            unlockedLetterTitles = newlyUnlocked.map { it.title }
        )
    }

    suspend fun saveWhyProfile(
        studentName: String,
        whyText: String,
        whoForText: String,
        dreamText: String,
        photoPath: String?,
        voicePath: String?
    ) {
        val now = effectiveNowMillis()
        db.whyProfileDao().upsertWhyProfile(
            WhyProfileEntity(
                id = 1,
                studentName = RevisionEngine.sanitizeInput(studentName),
                whyText = RevisionEngine.sanitizeInput(whyText),
                whoForText = RevisionEngine.sanitizeInput(whoForText),
                dreamText = RevisionEngine.sanitizeInput(dreamText),
                photoPath = photoPath,
                voicePath = voicePath,
                updatedAt = now
            )
        )
        prefs.setOnboardingCompleted(true)
    }

    suspend fun createFutureLetter(unlockStreakDays: Int, title: String, bodyText: String) {
        val now = effectiveNowMillis()
        val currentStreak = prefs.getSettingsSync().currentStreak
        val unlocked = currentStreak >= unlockStreakDays
        db.futureLetterDao().insertLetter(
            FutureLetterEntity(
                unlockStreakDays = unlockStreakDays,
                title = RevisionEngine.sanitizeInput(title).ifBlank { "${unlockStreakDays}-Day Letter" },
                bodyText = RevisionEngine.sanitizeInput(bodyText),
                createdAt = now,
                isUnlocked = unlocked,
                unlockedAt = if (unlocked) now else null
            )
        )
    }

    suspend fun deleteFutureLetter(id: Long) {
        db.futureLetterDao().deleteById(id)
    }

    suspend fun autoSaveGeminiApiKey(rawKey: String) {
        val trimmed = rawKey.trim()
        if (trimmed.isNotBlank()) {
            prefs.saveEncryptedApiKey(trimmed)
        }
    }

    suspend fun connectGeminiApiKey(rawKey: String): AiConnectResult {
        val trimmed = rawKey.trim()
        if (trimmed.isNotBlank()) {
            // Save immediately so transient 503/offline errors never erase the user's entered key
            prefs.saveEncryptedApiKey(trimmed)
        }
        val result = aiClient.discoverAndSelectWorkingModel(trimmed)
        if (result is AiConnectResult.Success) {
            prefs.saveCachedModel(result.modelId, System.currentTimeMillis())
        }
        return result
    }

    suspend fun disconnectApiKey() {
        prefs.saveEncryptedApiKey("")
    }

    suspend fun generateSmartNotificationText(
        dueItems: List<DueRevisionItem>
    ): Pair<String, String> {
        val settings = prefs.getSettingsSync()
        val why = db.whyProfileDao().getWhyProfileSync()
        val now = effectiveNowMillis()
        val missedCount = dueItems.count { it.milestone.status == MilestoneStatus.MISSED || now > it.milestone.dueAt }
        val apiKey = prefs.getDecryptedApiKey()

        val stageNames = dueItems.map { RevisionStage.fromIndex(it.milestone.stageIndex).label(settings.language) }
        val noteTitles = dueItems.map { it.note.title }
        val excerpt = if (settings.sendFullNoteToAi) {
            dueItems.firstOrNull()?.entry?.contentText
        } else {
            null
        }

        return aiClient.generateNotificationCopy(
            apiKey = apiKey,
            cachedModelId = settings.cachedModelId,
            cachedModelAtMillis = settings.cachedModelAtMillis,
            onModelUpdated = { modelId, at -> prefs.saveCachedModel(modelId, at) },
            lang = settings.language,
            styleRequirement = settings.aiStylePrompt,
            whyText = why?.whyText.orEmpty(),
            whoForText = why?.whoForText.orEmpty(),
            stageNames = stageNames,
            noteTitles = noteTitles,
            optionalEntryExcerpt = excerpt,
            streakDays = settings.currentStreak,
            hasMissed = missedCount > 0,
            missedCount = missedCount,
            dueCount = dueItems.size,
            nowMillis = now
        )
    }

    suspend fun getAllDueItemsSync(): List<DueRevisionItem> {
        val now = effectiveNowMillis()
        val notes = db.noteDao().getAllNotesSync().associateBy { it.id }
        val entries = db.studyEntryDao().getAllEntriesSync().associateBy { it.id }
        val milestones = db.milestoneDao().getAllMilestonesSync()
        val joined = milestones.mapNotNull { m ->
            val e = entries[m.entryId] ?: return@mapNotNull null
            val n = notes[m.noteId] ?: return@mapNotNull null
            DueRevisionItem(m, e, n)
        }
        return RevisionEngine.sortSerialDueQueue(joined, now)
    }

    suspend fun markMissedItemsNotified(items: List<RevisionMilestoneEntity>) {
        val updated = items.map { it.copy(missedReminderCount = it.missedReminderCount + 1) }
        if (updated.isNotEmpty()) {
            db.milestoneDao().updateMilestones(updated)
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val settings = prefs.getSettingsSync()
        return CryptoManager.verifyPin(pin, settings.pinHash, settings.pinSalt)
    }

    suspend fun exportEncryptedBackup(uri: Uri, passphrase: String): Boolean =
        backupManager.exportEncryptedBackup(uri, passphrase)

    suspend fun importEncryptedBackup(uri: Uri, passphrase: String): Boolean =
        backupManager.importEncryptedBackup(uri, passphrase)

    sealed interface SmartSyncOutcome {
        data class Restored(val notesCount: Int, val entriesCount: Int) : SmartSyncOutcome
        data class Uploaded(val notesCount: Int, val entriesCount: Int) : SmartSyncOutcome
        data class BothHaveData(val cloudNotesCount: Int, val cloudEntriesCount: Int) : SmartSyncOutcome
        data object UpToDate : SmartSyncOutcome
    }

    suspend fun hasLocalStudyData(): Boolean {
        return db.noteDao().getAllNotesSync().isNotEmpty() ||
            db.studyEntryDao().getAllEntriesSync().isNotEmpty() ||
            db.futureLetterDao().getAllLettersSync().isNotEmpty()
    }

    suspend fun hasLocalData(): Boolean {
        return hasLocalStudyData() ||
            db.whyProfileDao().getWhyProfileSync() != null
    }

    suspend fun syncToCloud(allowEmptyOverwrite: Boolean = false): Result<CloudSyncSummary> {
        val notes = db.noteDao().getAllNotesSync()
        val entries = db.studyEntryDao().getAllEntriesSync()
        val milestones = db.milestoneDao().getAllMilestonesSync()
        val whyProfile = db.whyProfileDao().getWhyProfileSync()
        val letters = db.futureLetterDao().getAllLettersSync()
        val activities = db.revisionActivityDao().getAllActivitySync()
        val settings = prefs.getSettingsSync()
        val plainApiKey = prefs.getDecryptedApiKey()

        val localEmpty = notes.isEmpty() &&
            entries.isEmpty() &&
            whyProfile == null &&
            letters.isEmpty() &&
            settings.aiStylePrompt.isBlank() &&
            plainApiKey.isBlank()
        if (localEmpty && !allowEmptyOverwrite) {
            val existingCloud = cloudSync.fetchCloudBackup().getOrNull()
            val cloudHasData = existingCloud != null && (
                existingCloud.notes.isNotEmpty() ||
                    existingCloud.whyProfile != null ||
                    existingCloud.futureLetters.isNotEmpty() ||
                    existingCloud.aiStylePrompt.isNotBlank() ||
                    existingCloud.encryptedAiKey.isNotBlank()
                )
            if (cloudHasData) {
                return Result.failure(
                    IllegalStateException("EMPTY_DEVICE_CLOUD_GUARD")
                )
            }
        }

        val uid = Firebase.auth.currentUser?.uid.orEmpty()
        val encryptedCloudKey = if (plainApiKey.isNotBlank() && uid.isNotBlank()) {
            CryptoManager.encryptForCloudSync(plainApiKey, uid)
        } else {
            ""
        }

        val result = cloudSync.syncLocalToCloud(
            notes = notes,
            entries = entries,
            milestones = milestones,
            whyProfile = whyProfile,
            letters = letters,
            activities = activities,
            currentStreak = settings.currentStreak,
            bestStreak = settings.bestStreak,
            lastRevisionDayKey = settings.lastRevisionDayKey,
            aiStylePrompt = settings.aiStylePrompt,
            sendFullNoteToAi = settings.sendFullNoteToAi,
            encryptedAiKey = encryptedCloudKey,
            cachedModelId = settings.cachedModelId,
            languageCode = settings.language.code
        )
        result.onSuccess { summary ->
            prefs.setLastCloudSyncAt(summary.syncedAtMillis)
        }
        return result
    }

    suspend fun onSignedInSmartSync(): Result<SmartSyncOutcome> {
        val fetchResult = cloudSync.fetchCloudBackup()
        return fetchResult.fold(
            onFailure = { Result.failure(it) },
            onSuccess = { payload ->
                val settings = prefs.getSettingsSync()
                val cloudHasData = payload.notes.isNotEmpty() ||
                    payload.whyProfile != null ||
                    payload.futureLetters.isNotEmpty() ||
                    payload.aiStylePrompt.isNotBlank() ||
                    payload.encryptedAiKey.isNotBlank()
                val localStudyExists = hasLocalStudyData()
                val localAnyExists = hasLocalData() ||
                    settings.aiStylePrompt.isNotBlank() ||
                    settings.hasApiKey
                when {
                    !localStudyExists && cloudHasData -> {
                        val counts = applyCloudRestorePayload(payload, preserveLocalWhyIfCloudNull = true)
                        Result.success(SmartSyncOutcome.Restored(counts.first, counts.second))
                    }
                    localAnyExists && !cloudHasData -> {
                        syncToCloud(allowEmptyOverwrite = false).map { summary ->
                            SmartSyncOutcome.Uploaded(summary.notesCount, summary.entriesCount)
                        }
                    }
                    localStudyExists && cloudHasData -> {
                        Result.success(
                            SmartSyncOutcome.BothHaveData(
                                cloudNotesCount = payload.notes.size,
                                cloudEntriesCount = payload.entries.size
                            )
                        )
                    }
                    else -> Result.success(SmartSyncOutcome.UpToDate)
                }
            }
        )
    }

    suspend fun restoreFromCloud(): Result<Pair<Int, Int>> {
        val fetchResult = cloudSync.fetchCloudBackup()
        return fetchResult.map { payload ->
            applyCloudRestorePayload(payload, preserveLocalWhyIfCloudNull = false)
        }
    }

    private suspend fun applyCloudRestorePayload(
        payload: com.example.data.cloud.CloudRestorePayload,
        preserveLocalWhyIfCloudNull: Boolean
    ): Pair<Int, Int> {
        val existingEntriesById = db.studyEntryDao().getAllEntriesSync().associateBy { it.id }
        val existingWhy = db.whyProfileDao().getWhyProfileSync()

        db.withTransaction {
            db.milestoneDao().deleteAll()
            db.studyEntryDao().deleteAll()
            db.noteDao().deleteAll()
            db.whyProfileDao().deleteAll()
            db.futureLetterDao().deleteAll()
            db.revisionActivityDao().deleteAll()

            payload.notes.forEach { db.noteDao().insertNote(it) }
            payload.entries.forEach { cloudEntry ->
                val localMatch = existingEntriesById[cloudEntry.id]
                db.studyEntryDao().insertEntry(
                    cloudEntry.copy(
                        photoPath = localMatch?.photoPath,
                        audioPath = localMatch?.audioPath
                    )
                )
            }
            if (payload.milestones.isNotEmpty()) {
                db.milestoneDao().insertAll(payload.milestones)
            }
            val effectiveWhy = payload.whyProfile ?: if (preserveLocalWhyIfCloudNull) existingWhy else null
            effectiveWhy?.let { why ->
                db.whyProfileDao().upsertWhyProfile(
                    why.copy(
                        photoPath = existingWhy?.photoPath,
                        voicePath = existingWhy?.voicePath
                    )
                )
            }
            payload.futureLetters.forEach { db.futureLetterDao().insertLetter(it) }
            payload.activities.forEach { db.revisionActivityDao().upsertActivity(it) }
        }

        if (payload.currentStreak > 0 || payload.bestStreak > 0 || payload.lastRevisionDayKey.isNotBlank()) {
            prefs.updateStreakStats(
                current = payload.currentStreak,
                best = payload.bestStreak,
                dayKey = payload.lastRevisionDayKey
            )
        }
        if (payload.aiStylePrompt.isNotBlank()) {
            prefs.saveAiStylePrompt(payload.aiStylePrompt)
        }
        prefs.setSendFullNoteToAi(payload.sendFullNoteToAi)
        if (payload.languageCode == AppLang.BN.code) {
            prefs.setLanguage(AppLang.BN)
        } else if (payload.languageCode == AppLang.EN.code) {
            prefs.setLanguage(AppLang.EN)
        }
        val uid = Firebase.auth.currentUser?.uid.orEmpty()
        if (payload.encryptedAiKey.isNotBlank() && uid.isNotBlank()) {
            val decryptedKey = CryptoManager.decryptFromCloudSync(payload.encryptedAiKey, uid)
            if (decryptedKey.isNotBlank()) {
                prefs.saveEncryptedApiKey(decryptedKey)
                if (payload.cachedModelId.isNotBlank()) {
                    prefs.saveCachedModel(payload.cachedModelId, System.currentTimeMillis())
                }
            }
        }
        if (payload.whyProfile != null || payload.notes.isNotEmpty() || payload.futureLetters.isNotEmpty()) {
            prefs.setOnboardingCompleted(true)
        }
        prefs.setLastCloudSyncAt(System.currentTimeMillis())
        sweepOverdueMilestones()
        return payload.notes.size to payload.entries.size
    }

    suspend fun deleteAllUserData() {
        db.milestoneDao().deleteAll()
        db.studyEntryDao().deleteAll()
        db.noteDao().deleteAll()
        db.whyProfileDao().deleteAll()
        db.futureLetterDao().deleteAll()
        db.revisionActivityDao().deleteAll()
        media.deleteAllMediaFiles()
        prefs.clearAll()
    }

    suspend fun setLanguage(lang: AppLang) = prefs.setLanguage(lang)
    suspend fun setThemeMode(mode: ThemeMode) = prefs.setThemeMode(mode)
    suspend fun setAccentPreset(preset: AccentPreset) = prefs.setAccentPreset(preset)
}
