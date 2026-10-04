package com.haoze.keynote.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
        KnowledgeVaultEntity::class,
        TreatLedgerEntity::class, TreatRecordEntity::class
    ],
    version = 2,
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
    abstract fun treatDao(): TreatDao

    companion object {
        @Volatile
        private var INSTANCE: KeyNoteDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `treat_ledgers` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `personAName` TEXT NOT NULL,
                        `personBName` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `isDefault` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `treat_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ledgerId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `payer` INTEGER NOT NULL,
                        `amountA` REAL NOT NULL,
                        `amountB` REAL NOT NULL,
                        `date` INTEGER NOT NULL,
                        `note` TEXT,
                        FOREIGN KEY(`ledgerId`) REFERENCES `treat_ledgers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_treat_records_ledgerId` ON `treat_records` (`ledgerId`)")
                val now = System.currentTimeMillis()
                db.execSQL(
                    "INSERT OR IGNORE INTO treat_ledgers (id, name, personAName, personBName, createdAt, isDefault) VALUES (1, '默认请客账本', '我', '对方', $now, 1)"
                )
            }
        }

        fun getDatabase(context: Context): KeyNoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KeyNoteDatabase::class.java,
                    "keynote_unified.db"
                ).addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration(true)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("INSERT OR IGNORE INTO todo_categories (name, color, isDefault) VALUES ('工作', 0xFF4CAF50, 1)")
                        db.execSQL("INSERT OR IGNORE INTO todo_categories (name, color, isDefault) VALUES ('个人', 0xFF2196F3, 1)")
                        db.execSQL("INSERT OR IGNORE INTO todo_categories (name, color, isDefault) VALUES ('学习', 0xFFFF9800, 1)")
                        db.execSQL("INSERT OR IGNORE INTO todo_categories (name, color, isDefault) VALUES ('健康', 0xFFF44336, 1)")
                        db.execSQL("INSERT OR IGNORE INTO todo_categories (name, color, isDefault) VALUES ('购物', 0xFF9C27B0, 1)")
                        val now = System.currentTimeMillis()
                        db.execSQL("INSERT OR IGNORE INTO treat_ledgers (id, name, personAName, personBName, createdAt, isDefault) VALUES (1, '默认请客账本', '我', '对方', $now, 1)")
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}