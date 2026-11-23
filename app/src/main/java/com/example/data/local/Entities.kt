package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.TypeConverter
import com.squareup.moshi.JsonClass

enum class MilestoneStatus {
    LOCKED,
    ACTIVE_DUE,
    MISSED,
    COMPLETED
}

class RoomConverters {
    @TypeConverter
    fun fromStatus(status: MilestoneStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): MilestoneStatus =
        runCatching { MilestoneStatus.valueOf(value) }.getOrDefault(MilestoneStatus.LOCKED)
}

@JsonClass(generateAdapter = true)
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subjectTag: String = "",
    val colorIndex: Int = 0,
    val isPinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "study_entries",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class StudyEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val contentText: String,
    val audioPath: String? = null,
    val photoPath: String? = null,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "revision_milestones",
    foreignKeys = [
        ForeignKey(
            entity = StudyEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("entryId"), Index("noteId"), Index("status")]
)
data class RevisionMilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val noteId: Long,
    val stageIndex: Int,
    val status: MilestoneStatus,
    val unlockedAt: Long,
    val dueAt: Long,
    val completedAt: Long? = null,
    val missedReminderCount: Int = 0
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "why_profile")
data class WhyProfileEntity(
    @PrimaryKey val id: Int = 1,
    val studentName: String = "",
    val whyText: String = "",
    val whoForText: String = "",
    val dreamText: String = "",
    val photoPath: String? = null,
    val voicePath: String? = null,
    val updatedAt: Long = 0L
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "future_letters")
data class FutureLetterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unlockStreakDays: Int,
    val title: String,
    val bodyText: String,
    val createdAt: Long,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "revision_activity")
data class RevisionActivityEntity(
    @PrimaryKey val dayKey: String,
    val completedCount: Int = 0
)

data class EntryWithMilestones(
    @Embedded val entry: StudyEntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "entryId"
    )
    val milestones: List<RevisionMilestoneEntity>
)

data class DueRevisionItem(
    val milestone: RevisionMilestoneEntity,
    val entry: StudyEntryEntity,
    val note: NoteEntity
)

data class NoteCardSummary(
    val note: NoteEntity,
    val entryCount: Int,
    val dueCount: Int,
    val missedCount: Int,
    val nextDueAt: Long?,
    val completedStagesCount: Int
)
