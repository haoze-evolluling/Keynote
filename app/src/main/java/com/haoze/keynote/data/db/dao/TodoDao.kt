package com.haoze.keynote.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.haoze.keynote.data.db.entity.TodoCategoryEntity
import com.haoze.keynote.data.db.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM todos WHERE isDeleted = 0 ORDER BY priority DESC, dueDate ASC, createdAt DESC")
    fun getAllActiveTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE isDeleted = 0 AND dueDate >= :start AND dueDate <= :end ORDER BY priority DESC, dueDate ASC")
    fun getTodosByDateRange(start: Long, end: Long): Flow<List<TodoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity): Long

    @Update
    suspend fun updateTodo(todo: TodoEntity)

    @Query("UPDATE todos SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteTodo(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE todos SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreTodo(id: Long)

    @Query("SELECT * FROM todos WHERE isDeleted = 1")
    fun getAllDeletedTodos(): Flow<List<TodoEntity>>

    @Query("DELETE FROM todos WHERE isDeleted = 1 AND deletedAt < :expireTime")
    suspend fun deleteExpiredTrashTodos(expireTime: Long)

    @Delete
    suspend fun permanentlyDeleteTodo(todo: TodoEntity)

    @Query("SELECT * FROM todo_categories ORDER BY isDefault DESC, name ASC")
    fun getAllCategories(): Flow<List<TodoCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: TodoCategoryEntity): Long
}
