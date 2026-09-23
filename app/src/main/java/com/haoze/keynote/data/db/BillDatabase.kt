package com.haoze.keynote.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.haoze.keynote.data.db.dao.AaSplitDao
import com.haoze.keynote.data.db.dao.BillDao
import com.haoze.keynote.data.db.dao.CategoryDao
import com.haoze.keynote.data.db.entity.AaSplitEntity
import com.haoze.keynote.data.db.entity.BillEntity
import com.haoze.keynote.data.db.entity.CategoryEntity

@Database(
    entities = [BillEntity::class, CategoryEntity::class, AaSplitEntity::class],
    version = 3,
    exportSchema = false
)
abstract class BillDatabase : RoomDatabase() {

    abstract fun billDao(): BillDao
    abstract fun categoryDao(): CategoryDao
    abstract fun aaSplitDao(): AaSplitDao

    companion object {
        @Volatile
        private var INSTANCE: BillDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bills ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE bills ADD COLUMN deletedAt INTEGER DEFAULT NULL")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE aa_splits (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        totalAmount REAL NOT NULL,
                        personCount INTEGER NOT NULL,
                        perPersonAmount REAL NOT NULL,
                        date INTEGER NOT NULL,
                        note TEXT
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): BillDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BillDatabase::class.java,
                    "keynote_bills.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
