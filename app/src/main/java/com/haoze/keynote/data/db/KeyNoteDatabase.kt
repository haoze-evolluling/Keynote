package com.haoze.keynote.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.haoze.keynote.data.db.dao.*
import com.haoze.keynote.data.db.entity.*

@Database(
    entities = [
        NoteEntity::class, TagEntity::class, NoteTagCrossRef::class,
        BillEntity::class, CategoryEntity::class, AaSplitEntity::class,
        ScheduleEntity::class,
        TodoEntity::class, TodoCategoryEntity::class,
        HabitEntity::class, HabitCheckInEntity::class,
        AIChatConversationEntity::class, AIChatMessageEntity::class,
        KnowledgeVaultEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KeyNoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun billDao(): BillDao
    abstract fun categoryDao(): CategoryDao
    abstract fun aaSplitDao(): AaSplitDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun todoDao(): TodoDao
    abstract fun habitDao(): HabitDao
    abstract fun aiChatDao(): AIChatDao
    abstract fun smartModuleDao(): SmartModuleDao

    companion object {
        @Volatile
        private var INSTANCE: KeyNoteDatabase? = null

        fun getDatabase(context: Context): KeyNoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KeyNoteDatabase::class.java,
                    "keynote_unified.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}