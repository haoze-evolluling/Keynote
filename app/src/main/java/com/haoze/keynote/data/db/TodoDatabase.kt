package com.haoze.keynote.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.haoze.keynote.data.db.dao.TodoDao
import com.haoze.keynote.data.db.entity.TodoCategoryEntity
import com.haoze.keynote.data.db.entity.TodoEntity

@Database(
    entities = [TodoEntity::class, TodoCategoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao

    companion object {
        @Volatile
        private var INSTANCE: TodoDatabase? = null

        fun getDatabase(context: Context): TodoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TodoDatabase::class.java,
                    "keynote_todos.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("INSERT OR IGNORE INTO todo_categories (name, color, isDefault) VALUES ('工作', 0xFF4CAF50, 1)")
                        db.execSQL("INSERT OR IGNORE INTO todo_categories (name, color, isDefault) VALUES ('个人', 0xFF2196F3, 1)")
                        db.execSQL("INSERT OR IGNORE INTO todo_categories (name, color, isDefault) VALUES ('学习', 0xFFFF9800, 1)")
                        db.execSQL("INSERT OR IGNORE INTO todo_categories (name, color, isDefault) VALUES ('健康', 0xFFF44336, 1)")
                        db.execSQL("INSERT OR IGNORE INTO todo_categories (name, color, isDefault) VALUES ('购物', 0xFF9C27B0, 1)")
                    }
                }).fallbackToDestructiveMigration(false).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
