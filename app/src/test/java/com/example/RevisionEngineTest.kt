package com.example

import com.example.data.backup.BackupSerializer
import com.example.data.backup.ReviseKeepBackupPayload
import com.example.data.local.DueRevisionItem
import com.example.data.local.FutureLetterEntity
import com.example.data.local.MilestoneStatus
import com.example.data.local.NoteEntity
import com.example.data.local.RevisionActivityEntity
import com.example.data.local.RevisionMilestoneEntity
import com.example.data.local.StudyEntryEntity
import com.example.data.local.WhyProfileEntity
import com.example.data.security.CryptoManager
import com.example.domain.RevisionEngine
import com.example.domain.RevisionStage
import com.example.i18n.AppLang
import com.example.i18n.StringsBnEn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

class RevisionEngineTest {

    @Test
    fun `16 stages follow exact forgetting curve and calendar month arithmetic`() {
        assertEquals(16, RevisionStage.entries.size)
        val jan31 = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 31, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val due1Month = RevisionStage.M1.calculateDueFrom(jan31)
        val cal1 = Calendar.getInstance().apply { timeInMillis = due1Month }
        assertEquals(Calendar.FEBRUARY, cal1.get(Calendar.MONTH))
        assertEquals(28, cal1.get(Calendar.DAY_OF_MONTH))

        val due2H = RevisionStage.H2.calculateDueFrom(jan31)
        assertEquals(TimeUnit.HOURS.toMillis(2), due2H - jan31)
    }

    @Test
    fun `serial due queue orders by stage index ascending then oldest dueAt first`() {
        val now = 1_700_000_000_000L
        val note = NoteEntity(id = 1, title = "Physics", createdAt = now, updatedAt = now)
        val entry = StudyEntryEntity(id = 1, noteId = 1, contentText = "Optics", createdAt = now)

        val m24hOlder = RevisionMilestoneEntity(
            id = 1, entryId = 1, noteId = 1, stageIndex = RevisionStage.H24.index,
            status = MilestoneStatus.ACTIVE_DUE, unlockedAt = now - 90_000, dueAt = now - 5_000
        )
        val m2hNewer = RevisionMilestoneEntity(
            id = 2, entryId = 1, noteId = 1, stageIndex = RevisionStage.H2.index,
            status = MilestoneStatus.ACTIVE_DUE, unlockedAt = now - 20_000, dueAt = now - 1_000
        )
        val m2hOlder = RevisionMilestoneEntity(
            id = 3, entryId = 1, noteId = 1, stageIndex = RevisionStage.H2.index,
            status = MilestoneStatus.MISSED, unlockedAt = now - 50_000, dueAt = now - 10_000
        )
        val mFuture = RevisionMilestoneEntity(
            id = 4, entryId = 1, noteId = 1, stageIndex = RevisionStage.D3.index,
            status = MilestoneStatus.ACTIVE_DUE, unlockedAt = now, dueAt = now + 100_000
        )

        val sorted = RevisionEngine.sortSerialDueQueue(
            listOf(
                DueRevisionItem(m24hOlder, entry, note),
                DueRevisionItem(m2hNewer, entry, note),
                DueRevisionItem(m2hOlder, entry, note),
                DueRevisionItem(mFuture, entry, note)
            ),
            effectiveNowMillis = now
        )

        assertEquals(listOf(3L, 2L, 1L), sorted.map { it.milestone.id })
    }

    @Test
    fun `overdue active milestones transition to missed and are preserved`() {
        val now = 10_000_000L
        val overdue = RevisionMilestoneEntity(
            id = 1, entryId = 1, noteId = 1, stageIndex = 0,
            status = MilestoneStatus.ACTIVE_DUE, unlockedAt = 1000L, dueAt = 5000L
        )
        val notYetDue = RevisionMilestoneEntity(
            id = 2, entryId = 1, noteId = 1, stageIndex = 1,
            status = MilestoneStatus.ACTIVE_DUE, unlockedAt = 1000L, dueAt = 15_000_000L
        )
        val missed = RevisionEngine.findNewlyMissedMilestones(listOf(overdue, notYetDue), now)
        assertEquals(1, missed.size)
        assertEquals(MilestoneStatus.MISSED, missed.first().status)
        assertEquals(1L, missed.first().id)
    }

    @Test
    fun `quiet hours handles overnight 22 to 7 range`() {
        val night23 = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 15)
        }.timeInMillis
        val morning6 = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 45)
        }.timeInMillis
        val afternoon14 = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
        }.timeInMillis

        assertTrue(RevisionEngine.isWithinQuietHours(night23, 22, 7))
        assertTrue(RevisionEngine.isWithinQuietHours(morning6, 22, 7))
        assertFalse(RevisionEngine.isWithinQuietHours(afternoon14, 22, 7))
    }

    @Test
    fun `gemini model ranking prefers Pro then Flash then Flash-Lite and filters non-text`() {
        val raw = listOf(
            "models/text-embedding-004" to listOf("embedContent"),
            "models/gemini-2.5-flash-tts" to listOf("generateContent"),
            "models/gemini-2.5-flash-image" to listOf("generateContent"),
            "models/gemini-2.5-flash-lite" to listOf("generateContent"),
            "models/gemini-3-flash-preview" to listOf("generateContent"),
            "models/gemini-2.5-flash" to listOf("generateContent"),
            "models/gemini-2.5-pro" to listOf("generateContent"),
            "models/imagen-3.0-generate-001" to listOf("predict")
        )
        val ranked = RevisionEngine.rankGeminiModels(raw)
        assertEquals(
            listOf("gemini-2.5-pro", "gemini-2.5-flash", "gemini-3-flash-preview", "gemini-2.5-flash-lite"),
            ranked.map { it.id }
        )
        assertTrue(RevisionEngine.shouldFallbackOnHttpStatus(503))
        assertTrue(RevisionEngine.shouldFallbackOnHttpStatus(429))
        assertTrue(RevisionEngine.shouldFallbackOnHttpStatus(404))
        assertTrue(RevisionEngine.shouldFallbackOnHttpStatus(403))
        assertFalse(RevisionEngine.shouldFallbackOnHttpStatus(401))
    }

    @Test
    fun `sanitization removes script tags and bilingual missed messages match exact copy`() {
        val cleaned = RevisionEngine.sanitizeInput("<script>alert(1)</script><b>Newton</b>")
        assertEquals("Newton", cleaned)
        assertEquals("আজকের এই সময়ের রিভিশন দেওয়া হয়নি", StringsBnEn.bengali.missedNotificationBody)
        assertEquals("Today's revision for this time slot is not done yet.", StringsBnEn.english.missedNotificationBody)
    }

    @Test
    fun `backup passphrase encryption and salted pin hashing roundtrip`() {
        val payload = "{\"notes\":[\"Physics\"]}"
        val encrypted = CryptoManager.encryptBackupWithPassphrase(payload, "secret123")
        assertTrue(encrypted.startsWith("REVISEKEEP_ENC_V1:"))
        val decrypted = CryptoManager.decryptBackupWithPassphrase(encrypted, "secret123")
        assertEquals(payload, decrypted)

        val salt = CryptoManager.generateSaltBase64()
        val hash = CryptoManager.hashPin("2580", salt)
        assertTrue(CryptoManager.verifyPin("2580", hash, salt))
        assertFalse(CryptoManager.verifyPin("1234", hash, salt))
    }

    @Test
    fun `full BackupSerializer encrypted roundtrip preserves all entities and rejects bad passphrase`() {
        val now = 1_750_000_000_000L
        val payload = ReviseKeepBackupPayload(
            exportedAt = now,
            notes = listOf(
                NoteEntity(id = 10, title = "পদার্থবিজ্ঞান", subjectTag = "HSC", colorIndex = 2, isPinned = true, createdAt = now, updatedAt = now)
            ),
            entries = listOf(
                StudyEntryEntity(id = 20, noteId = 10, contentText = "নিউটনের গতিসূত্র", audioPath = null, photoPath = null, createdAt = now)
            ),
            milestones = listOf(
                RevisionMilestoneEntity(id = 30, entryId = 20, noteId = 10, stageIndex = 0, status = MilestoneStatus.ACTIVE_DUE, unlockedAt = now, dueAt = now + 7_200_000L)
            ),
            whyProfile = WhyProfileEntity(id = 1, studentName = "Tanvir", whyText = "মায়ের স্বপ্ন", whoForText = "মা", dreamText = "Engineer", updatedAt = now),
            futureLetters = listOf(
                FutureLetterEntity(id = 40, unlockStreakDays = 7, title = "7-day note", bodyText = "Keep going!", createdAt = now, isUnlocked = false)
            ),
            activities = listOf(
                RevisionActivityEntity(dayKey = "2026-11-22", completedCount = 3)
            )
        )

        val encrypted = BackupSerializer.encryptPayload(payload, "myPass99")
        val restored = BackupSerializer.decryptPayload(encrypted, "myPass99")
        assertEquals(payload, restored)

        assertThrows(IllegalArgumentException::class.java) {
            BackupSerializer.encryptPayload(payload, "123")
        }
        assertThrows(Exception::class.java) {
            BackupSerializer.decryptPayload(encrypted, "wrongPass")
        }
    }

    @Test
    fun `cloud sync API key encryption roundtrips for owner UID and fails for another UID`() {
        val secretKey = "AIzaSyExampleTestKey12345"
        val uid = "google_uid_abc"
        val encrypted = CryptoManager.encryptForCloudSync(secretKey, uid)
        assertTrue(encrypted.startsWith("RK_CLOUD_V1:"))
        assertFalse(encrypted.contains(secretKey))

        val decrypted = CryptoManager.decryptFromCloudSync(encrypted, uid)
        assertEquals(secretKey, decrypted)

        val wrongUserDecrypted = CryptoManager.decryptFromCloudSync(encrypted, "other_uid_xyz")
        assertEquals("", wrongUserDecrypted)
    }
}
