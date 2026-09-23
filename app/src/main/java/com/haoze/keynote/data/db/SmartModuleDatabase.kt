package com.haoze.keynote.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.haoze.keynote.data.db.dao.SmartModuleDao
import com.haoze.keynote.data.db.entity.KnowledgeVaultEntity

@Database(
    entities = [
        KnowledgeVaultEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SmartModuleDatabase : RoomDatabase() {
    abstract fun smartModuleDao(): SmartModuleDao

    companion object {
        @Volatile
        private var INSTANCE: SmartModuleDatabase? = null

        fun getDatabase(context: Context): SmartModuleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmartModuleDatabase::class.java,
                    "keynote_smart_modules.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}