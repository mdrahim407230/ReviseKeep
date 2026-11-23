package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getAllNotesSync(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    fun observeNoteById(noteId: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    suspend fun getNoteByIdSync(noteId: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    @Query("DELETE FROM notes")
    suspend fun deleteAll()
}

@Dao
interface StudyEntryDao {
    @Transaction
    @Query("SELECT * FROM study_entries WHERE noteId = :noteId ORDER BY createdAt DESC")
    fun observeEntriesWithMilestones(noteId: Long): Flow<List<EntryWithMilestones>>

    @Query("SELECT * FROM study_entries ORDER BY createdAt DESC")
    fun observeAllEntries(): Flow<List<StudyEntryEntity>>

    @Query("SELECT * FROM study_entries")
    suspend fun getAllEntriesSync(): List<StudyEntryEntity>

    @Query("SELECT * FROM study_entries WHERE id = :entryId LIMIT 1")
    suspend fun getEntryByIdSync(entryId: Long): StudyEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: StudyEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: StudyEntryEntity)

    @Query("DELETE FROM study_entries WHERE id = :entryId")
    suspend fun deleteEntryById(entryId: Long)

    @Query("DELETE FROM study_entries")
    suspend fun deleteAll()
}

@Dao
interface MilestoneDao {
    @Query("SELECT * FROM revision_milestones")
    fun observeAllMilestones(): Flow<List<RevisionMilestoneEntity>>

    @Query("SELECT * FROM revision_milestones")
    suspend fun getAllMilestonesSync(): List<RevisionMilestoneEntity>

    @Query("SELECT * FROM revision_milestones WHERE entryId = :entryId ORDER BY stageIndex ASC")
    suspend fun getMilestonesForEntry(entryId: Long): List<RevisionMilestoneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(milestones: List<RevisionMilestoneEntity>)

    @Update
    suspend fun updateMilestone(milestone: RevisionMilestoneEntity)

    @Update
    suspend fun updateMilestones(milestones: List<RevisionMilestoneEntity>)

    @Query("DELETE FROM revision_milestones")
    suspend fun deleteAll()
}

@Dao
interface WhyProfileDao {
    @Query("SELECT * FROM why_profile WHERE id = 1 LIMIT 1")
    fun observeWhyProfile(): Flow<WhyProfileEntity?>

    @Query("SELECT * FROM why_profile WHERE id = 1 LIMIT 1")
    suspend fun getWhyProfileSync(): WhyProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWhyProfile(profile: WhyProfileEntity)

    @Query("DELETE FROM why_profile")
    suspend fun deleteAll()
}

@Dao
interface FutureLetterDao {
    @Query("SELECT * FROM future_letters ORDER BY unlockStreakDays ASC, createdAt DESC")
    fun observeLetters(): Flow<List<FutureLetterEntity>>

    @Query("SELECT * FROM future_letters")
    suspend fun getAllLettersSync(): List<FutureLetterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLetter(letter: FutureLetterEntity): Long

    @Update
    suspend fun updateLetters(letters: List<FutureLetterEntity>)

    @Query("DELETE FROM future_letters WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM future_letters")
    suspend fun deleteAll()
}

@Dao
interface RevisionActivityDao {
    @Query("SELECT * FROM revision_activity ORDER BY dayKey DESC LIMIT 30")
    fun observeRecentActivity(): Flow<List<RevisionActivityEntity>>

    @Query("SELECT * FROM revision_activity")
    suspend fun getAllActivitySync(): List<RevisionActivityEntity>

    @Query("SELECT * FROM revision_activity WHERE dayKey = :dayKey LIMIT 1")
    suspend fun getByDayKey(dayKey: String): RevisionActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivity(activity: RevisionActivityEntity)

    @Query("DELETE FROM revision_activity")
    suspend fun deleteAll()
}
