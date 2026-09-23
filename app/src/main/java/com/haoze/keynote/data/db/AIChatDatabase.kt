package com.haoze.keynote.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.haoze.keynote.data.db.dao.AIChatDao
import com.haoze.keynote.data.db.entity.AIChatConversationEntity
import com.haoze.keynote.data.db.entity.AIChatMessageEntity

@Database(
    entities = [AIChatConversationEntity::class, AIChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AIChatDatabase : RoomDatabase() {
    abstract fun aiChatDao(): AIChatDao

    companion object {
        @Volatile
        private var INSTANCE: AIChatDatabase? = null

        fun getDatabase(context: Context): AIChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AIChatDatabase::class.java,
                    "keynote_ai_chat.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
