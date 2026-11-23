package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        NoteEntity::class,
        StudyEntryEntity::class,
        RevisionMilestoneEntity::class,
        WhyProfileEntity::class,
        FutureLetterEntity::class,
        RevisionActivityEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class ReviseKeepDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun studyEntryDao(): StudyEntryDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun whyProfileDao(): WhyProfileDao
    abstract fun futureLetterDao(): FutureLetterDao
    abstract fun revisionActivityDao(): RevisionActivityDao

    companion object {
        @Volatile
        private var INSTANCE: ReviseKeepDatabase? = null

        fun getInstance(context: Context): ReviseKeepDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ReviseKeepDatabase::class.java,
                    "revisekeep_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
