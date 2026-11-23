package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.local.FutureLetterEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ReviseKeepDatabase
import com.example.data.local.RevisionActivityEntity
import com.example.data.local.RevisionMilestoneEntity
import com.example.data.local.StudyEntryEntity
import com.example.data.local.WhyProfileEntity
import com.example.data.security.CryptoManager
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

import androidx.room.withTransaction

@JsonClass(generateAdapter = true)
data class ReviseKeepBackupPayload(
    val schemaVersion: Int = 1,
    val exportedAt: Long,
    val notes: List<NoteEntity>,
    val entries: List<StudyEntryEntity>,
    val milestones: List<RevisionMilestoneEntity>,
    val whyProfile: WhyProfileEntity?,
    val futureLetters: List<FutureLetterEntity>,
    val activities: List<RevisionActivityEntity>
)

object BackupSerializer {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(ReviseKeepBackupPayload::class.java)

    fun encryptPayload(payload: ReviseKeepBackupPayload, passphrase: String): String {
        require(passphrase.length >= 4) { "Passphrase must be at least 4 characters" }
        val json = adapter.toJson(payload)
        return CryptoManager.encryptBackupWithPassphrase(json, passphrase)
    }

    fun decryptPayload(encrypted: String, passphrase: String): ReviseKeepBackupPayload {
        require(passphrase.length >= 4) { "Passphrase must be at least 4 characters" }
        val json = CryptoManager.decryptBackupWithPassphrase(encrypted, passphrase)
        return adapter.fromJson(json) ?: error("Invalid backup JSON")
    }
}

class BackupManager(
    private val context: Context,
    private val db: ReviseKeepDatabase
) {
    suspend fun exportEncryptedBackup(uri: Uri, passphrase: String): Boolean {
        if (passphrase.length < 4) return false
        return runCatching {
            val payload = ReviseKeepBackupPayload(
                exportedAt = System.currentTimeMillis(),
                notes = db.noteDao().getAllNotesSync(),
                entries = db.studyEntryDao().getAllEntriesSync(),
                milestones = db.milestoneDao().getAllMilestonesSync(),
                whyProfile = db.whyProfileDao().getWhyProfileSync(),
                futureLetters = db.futureLetterDao().getAllLettersSync(),
                activities = db.revisionActivityDao().getAllActivitySync()
            )
            val encrypted = BackupSerializer.encryptPayload(payload, passphrase)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(encrypted.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return false
            true
        }.getOrDefault(false)
    }

    suspend fun importEncryptedBackup(uri: Uri, passphrase: String): Boolean {
        if (passphrase.length < 4) return false
        return runCatching {
            val encrypted = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: return false
            val payload = BackupSerializer.decryptPayload(encrypted, passphrase)

            db.withTransaction {
                db.milestoneDao().deleteAll()
                db.studyEntryDao().deleteAll()
                db.noteDao().deleteAll()
                db.whyProfileDao().deleteAll()
                db.futureLetterDao().deleteAll()
                db.revisionActivityDao().deleteAll()

                payload.notes.forEach { db.noteDao().insertNote(it) }
                payload.entries.forEach { db.studyEntryDao().insertEntry(it) }
                if (payload.milestones.isNotEmpty()) {
                    db.milestoneDao().insertAll(payload.milestones)
                }
                payload.whyProfile?.let { db.whyProfileDao().upsertWhyProfile(it) }
                payload.futureLetters.forEach { db.futureLetterDao().insertLetter(it) }
                payload.activities.forEach { db.revisionActivityDao().upsertActivity(it) }
            }
            true
        }.getOrDefault(false)
    }
}
