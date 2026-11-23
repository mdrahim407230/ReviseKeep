package com.example.data.cloud

import android.content.Context
import com.example.R
import com.example.data.local.FutureLetterEntity
import com.example.data.local.MilestoneStatus
import com.example.data.local.NoteEntity
import com.example.data.local.RevisionActivityEntity
import com.example.data.local.RevisionMilestoneEntity
import com.example.data.local.StudyEntryEntity
import com.example.data.local.WhyProfileEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

data class CloudSyncSummary(
    val notesCount: Int,
    val entriesCount: Int,
    val milestonesCount: Int,
    val syncedAtMillis: Long
)

data class CloudRestorePayload(
    val notes: List<NoteEntity>,
    val entries: List<StudyEntryEntity>,
    val milestones: List<RevisionMilestoneEntity>,
    val whyProfile: WhyProfileEntity?,
    val futureLetters: List<FutureLetterEntity>,
    val activities: List<RevisionActivityEntity>,
    val currentStreak: Int,
    val bestStreak: Int,
    val lastRevisionDayKey: String,
    val aiStylePrompt: String = "",
    val sendFullNoteToAi: Boolean = false,
    val encryptedAiKey: String = "",
    val cachedModelId: String = "",
    val languageCode: String = ""
)

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        }
        return uid
    }

    suspend fun syncLocalToCloud(
        notes: List<NoteEntity>,
        entries: List<StudyEntryEntity>,
        milestones: List<RevisionMilestoneEntity>,
        whyProfile: WhyProfileEntity?,
        letters: List<FutureLetterEntity>,
        activities: List<RevisionActivityEntity>,
        currentStreak: Int,
        bestStreak: Int,
        lastRevisionDayKey: String,
        aiStylePrompt: String,
        sendFullNoteToAi: Boolean,
        encryptedAiKey: String,
        cachedModelId: String,
        languageCode: String
    ): Result<CloudSyncSummary> {
        var activePath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val user = auth.currentUser
            val userDoc = db.collection("users").document(uid)
            activePath = userDoc.path

            val userExists = userDoc.get().await().exists()
            val userPayload = buildMap<String, Any> {
                put("userId", uid)
                val displayName = user?.displayName?.take(120)
                if (!displayName.isNullOrBlank()) put("displayName", displayName)
                val email = user?.email?.take(200)
                if (!email.isNullOrBlank()) put("email", email)
                put("currentStreak", currentStreak.coerceIn(0, 100000))
                put("bestStreak", bestStreak.coerceIn(0, 100000))
                put("lastRevisionDayKey", lastRevisionDayKey.take(32))
                put("aiStylePrompt", aiStylePrompt.take(4000))
                put("sendFullNoteToAi", sendFullNoteToAi)
                put("encryptedAiKey", encryptedAiKey.take(2048))
                put("cachedModelId", cachedModelId.take(160))
                if (languageCode == "EN" || languageCode == "BN") {
                    put("language", languageCode)
                }
                if (!userExists) {
                    put("createdAt", FieldValue.serverTimestamp())
                }
                put("updatedAt", FieldValue.serverTimestamp())
            }
            if (userExists) {
                userDoc.update(userPayload - "userId").await()
            } else {
                userDoc.set(userPayload).await()
            }

            // Collect operations so we can chunk commits at 400 ops per WriteBatch
            val ops = mutableListOf<BatchOp>()

            // 1. Why Profile (/users/{uid}/profile/why_profile)
            val profileRef = userDoc.collection("profile").document("why_profile")
            activePath = profileRef.path
            val profileExists = profileRef.get().await().exists()
            if (whyProfile != null) {
                val data = mapOf(
                    "userId" to uid,
                    "studentName" to whyProfile.studentName.take(120),
                    "whyText" to whyProfile.whyText.take(4000),
                    "whoForText" to whyProfile.whoForText.take(1000),
                    "dreamText" to whyProfile.dreamText.take(1000),
                    "clientUpdatedAt" to whyProfile.updatedAt.coerceAtLeast(0L)
                )
                if (profileExists) {
                    ops.add(BatchOp.Update(profileRef, data - "userId" + ("updatedAt" to FieldValue.serverTimestamp())))
                } else {
                    ops.add(
                        BatchOp.Set(
                            profileRef,
                            data + mapOf(
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    )
                }
            }

            // 2. Notes
            activePath = "${userDoc.path}/notes"
            val existingNotes = userDoc.collection("notes").get().await().documents.map { it.id }.toSet()
            val targetNoteIds = notes.map { "note_${it.id}" }.toSet()
            for (note in notes) {
                val docId = "note_${note.id}"
                val ref = userDoc.collection("notes").document(docId)
                val base = mapOf(
                    "roomId" to note.id,
                    "title" to note.title.ifBlank { "Study Note" }.take(300),
                    "subjectTag" to note.subjectTag.take(120),
                    "colorIndex" to note.colorIndex.coerceIn(0, 20),
                    "pinned" to note.isPinned,
                    "clientCreatedAt" to note.createdAt.coerceAtLeast(0L),
                    "clientUpdatedAt" to note.updatedAt.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (docId in existingNotes) {
                    ops.add(BatchOp.Update(ref, base))
                } else {
                    ops.add(BatchOp.Set(ref, base + mapOf("userId" to uid, "createdAt" to FieldValue.serverTimestamp())))
                }
            }
            for (staleId in existingNotes - targetNoteIds) {
                ops.add(BatchOp.Delete(userDoc.collection("notes").document(staleId)))
            }

            // 3. Study Entries
            activePath = "${userDoc.path}/entries"
            val existingEntries = userDoc.collection("entries").get().await().documents.map { it.id }.toSet()
            val targetEntryIds = entries.map { "entry_${it.id}" }.toSet()
            for (entry in entries) {
                val docId = "entry_${entry.id}"
                val ref = userDoc.collection("entries").document(docId)
                val base = mapOf(
                    "roomId" to entry.id,
                    "noteRoomId" to entry.noteId,
                    "contentText" to entry.contentText.take(20000),
                    "clientCreatedAt" to entry.createdAt.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (docId in existingEntries) {
                    ops.add(BatchOp.Update(ref, base))
                } else {
                    ops.add(BatchOp.Set(ref, base + mapOf("userId" to uid, "createdAt" to FieldValue.serverTimestamp())))
                }
            }
            for (staleId in existingEntries - targetEntryIds) {
                ops.add(BatchOp.Delete(userDoc.collection("entries").document(staleId)))
            }

            // 4. Revision Milestones
            activePath = "${userDoc.path}/milestones"
            val existingMilestones = userDoc.collection("milestones").get().await().documents.map { it.id }.toSet()
            val targetMilestoneIds = milestones.map { "ms_${it.id}" }.toSet()
            for (m in milestones) {
                val docId = "ms_${m.id}"
                val ref = userDoc.collection("milestones").document(docId)
                val base = buildMap<String, Any> {
                    put("roomId", m.id)
                    put("entryRoomId", m.entryId)
                    put("noteRoomId", m.noteId)
                    put("stageIndex", m.stageIndex.coerceIn(0, 15))
                    put("status", m.status.name)
                    put("unlockedAt", m.unlockedAt.coerceAtLeast(0L))
                    put("dueAt", m.dueAt.coerceAtLeast(0L))
                    m.completedAt?.let { put("completedAt", it.coerceAtLeast(0L)) }
                    put("missedReminderCount", m.missedReminderCount.coerceIn(0, 1000))
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                if (docId in existingMilestones) {
                    ops.add(BatchOp.Update(ref, base))
                } else {
                    ops.add(BatchOp.Set(ref, base + mapOf("userId" to uid, "createdAt" to FieldValue.serverTimestamp())))
                }
            }
            for (staleId in existingMilestones - targetMilestoneIds) {
                ops.add(BatchOp.Delete(userDoc.collection("milestones").document(staleId)))
            }

            // 5. Future Letters
            activePath = "${userDoc.path}/future_letters"
            val existingLetters = userDoc.collection("future_letters").get().await().documents.map { it.id }.toSet()
            val targetLetterIds = letters.map { "letter_${it.id}" }.toSet()
            for (l in letters) {
                val docId = "letter_${l.id}"
                val ref = userDoc.collection("future_letters").document(docId)
                val base = buildMap<String, Any> {
                    put("roomId", l.id)
                    put("unlockStreakDays", l.unlockStreakDays.coerceIn(1, 3650))
                    put("title", l.title.ifBlank { "Letter" }.take(300))
                    put("bodyText", l.bodyText.ifBlank { "..." }.take(10000))
                    put("clientCreatedAt", l.createdAt.coerceAtLeast(0L))
                    put("unlocked", l.isUnlocked)
                    l.unlockedAt?.let { put("unlockedAt", it.coerceAtLeast(0L)) }
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                if (docId in existingLetters) {
                    ops.add(BatchOp.Update(ref, base))
                } else {
                    ops.add(BatchOp.Set(ref, base + mapOf("userId" to uid, "createdAt" to FieldValue.serverTimestamp())))
                }
            }
            for (staleId in existingLetters - targetLetterIds) {
                ops.add(BatchOp.Delete(userDoc.collection("future_letters").document(staleId)))
            }

            // 6. Activities
            activePath = "${userDoc.path}/activities"
            val existingActivities = userDoc.collection("activities").get().await().documents.map { it.id }.toSet()
            val validDayRegex = Regex("^[a-zA-Z0-9_\\-]+$")
            val filteredActivities = activities.filter { validDayRegex.matches(it.dayKey) }
            val targetActivityIds = filteredActivities.map { it.dayKey }.toSet()
            for (act in filteredActivities) {
                val docId = act.dayKey
                val ref = userDoc.collection("activities").document(docId)
                val base = mapOf(
                    "dayKey" to act.dayKey.take(32),
                    "completedCount" to act.completedCount.coerceIn(0, 10000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (docId in existingActivities) {
                    ops.add(BatchOp.Update(ref, base))
                } else {
                    ops.add(BatchOp.Set(ref, base + mapOf("userId" to uid, "createdAt" to FieldValue.serverTimestamp())))
                }
            }
            for (staleId in existingActivities - targetActivityIds) {
                ops.add(BatchOp.Delete(userDoc.collection("activities").document(staleId)))
            }

            // Commit in chunks of 400
            activePath = "${userDoc.path}/batch_commit"
            for (chunk in ops.chunked(400)) {
                val batch = db.batch()
                for (op in chunk) {
                    when (op) {
                        is BatchOp.Set -> batch.set(op.ref, op.data)
                        is BatchOp.Update -> batch.update(op.ref, op.data)
                        is BatchOp.Delete -> batch.delete(op.ref)
                    }
                }
                batch.commit().await()
            }

            Result.success(
                CloudSyncSummary(
                    notesCount = notes.size,
                    entriesCount = entries.size,
                    milestonesCount = milestones.size,
                    syncedAtMillis = System.currentTimeMillis()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, activePath)
            Result.failure(e)
        }
    }

    suspend fun fetchCloudBackup(): Result<CloudRestorePayload> {
        var activePath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val userDocRef = db.collection("users").document(uid)
            activePath = userDocRef.path

            val userSnap = userDocRef.get().await()
            val userMeta = userSnap.toObject(UserSyncMetaDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

            activePath = "${userDocRef.path}/profile/why_profile"
            val profileSnap = userDocRef.collection("profile").document("why_profile").get().await()
            val whyDoc = if (profileSnap.exists()) {
                profileSnap.toObject(WhyProfileSyncDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }

            activePath = "${userDocRef.path}/notes"
            val noteDocs = userDocRef.collection("notes").get().await()
                .toObjects(NoteSyncDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

            activePath = "${userDocRef.path}/entries"
            val entryDocs = userDocRef.collection("entries").get().await()
                .toObjects(StudyEntrySyncDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

            activePath = "${userDocRef.path}/milestones"
            val milestoneDocs = userDocRef.collection("milestones").get().await()
                .toObjects(MilestoneSyncDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

            activePath = "${userDocRef.path}/future_letters"
            val letterDocs = userDocRef.collection("future_letters").get().await()
                .toObjects(FutureLetterSyncDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

            activePath = "${userDocRef.path}/activities"
            val activityDocs = userDocRef.collection("activities").get().await()
                .toObjects(RevisionActivitySyncDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

            val notes = noteDocs.filter { it.roomId > 0L }.map {
                NoteEntity(
                    id = it.roomId,
                    title = it.title,
                    subjectTag = it.subjectTag,
                    colorIndex = it.colorIndex.toInt(),
                    isPinned = it.pinned,
                    createdAt = it.clientCreatedAt,
                    updatedAt = it.clientUpdatedAt
                )
            }
            val entries = entryDocs.filter { it.roomId > 0L && it.noteRoomId > 0L }.map {
                StudyEntryEntity(
                    id = it.roomId,
                    noteId = it.noteRoomId,
                    contentText = it.contentText,
                    audioPath = null,
                    photoPath = null,
                    createdAt = it.clientCreatedAt
                )
            }
            val milestones = milestoneDocs.filter { it.roomId > 0L }.map { doc ->
                val parsedStatus = MilestoneStatus.entries.firstOrNull { it.name == doc.status }
                    ?: MilestoneStatus.LOCKED
                RevisionMilestoneEntity(
                    id = doc.roomId,
                    entryId = doc.entryRoomId,
                    noteId = doc.noteRoomId,
                    stageIndex = doc.stageIndex.toInt().coerceIn(0, 15),
                    status = parsedStatus,
                    unlockedAt = doc.unlockedAt,
                    dueAt = doc.dueAt,
                    completedAt = doc.completedAt,
                    missedReminderCount = doc.missedReminderCount.toInt()
                )
            }
            val whyProfile = whyDoc?.let {
                WhyProfileEntity(
                    id = 1,
                    studentName = it.studentName,
                    whyText = it.whyText,
                    whoForText = it.whoForText,
                    dreamText = it.dreamText,
                    photoPath = null,
                    voicePath = null,
                    updatedAt = it.clientUpdatedAt
                )
            }
            val letters = letterDocs.filter { it.roomId > 0L }.map {
                FutureLetterEntity(
                    id = it.roomId,
                    unlockStreakDays = it.unlockStreakDays.toInt(),
                    title = it.title,
                    bodyText = it.bodyText,
                    createdAt = it.clientCreatedAt,
                    isUnlocked = it.unlocked,
                    unlockedAt = it.unlockedAt
                )
            }
            val activities = activityDocs.filter { it.dayKey.isNotBlank() }.map {
                RevisionActivityEntity(
                    dayKey = it.dayKey,
                    completedCount = it.completedCount.toInt()
                )
            }

            Result.success(
                CloudRestorePayload(
                    notes = notes,
                    entries = entries,
                    milestones = milestones,
                    whyProfile = whyProfile,
                    futureLetters = letters,
                    activities = activities,
                    currentStreak = (userMeta?.currentStreak ?: 0L).toInt(),
                    bestStreak = (userMeta?.bestStreak ?: 0L).toInt(),
                    lastRevisionDayKey = userMeta?.lastRevisionDayKey.orEmpty(),
                    aiStylePrompt = userMeta?.aiStylePrompt.orEmpty(),
                    sendFullNoteToAi = userMeta?.sendFullNoteToAi ?: false,
                    encryptedAiKey = userMeta?.encryptedAiKey.orEmpty(),
                    cachedModelId = userMeta?.cachedModelId.orEmpty(),
                    languageCode = userMeta?.language.orEmpty()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, activePath)
            Result.failure(e)
        }
    }

    private sealed interface BatchOp {
        data class Set(val ref: DocumentReference, val data: Map<String, Any>) : BatchOp
        data class Update(val ref: DocumentReference, val data: Map<String, Any>) : BatchOp
        data class Delete(val ref: DocumentReference) : BatchOp
    }
}
