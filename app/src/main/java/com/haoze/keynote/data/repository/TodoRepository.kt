package com.haoze.keynote.data.repository

import com.haoze.keynote.data.db.dao.TodoDao
import com.haoze.keynote.data.db.entity.TodoCategoryEntity
import com.haoze.keynote.data.db.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

class TodoRepository(private val dao: TodoDao) {

    fun getAllActiveTodos(): Flow<List<TodoEntity>> = dao.getAllActiveTodos()
    fun getTodosByDateRange(start: Long, end: Long): Flow<List<TodoEntity>> = dao.getTodosByDateRange(start, end)
    fun getAllCategories(): Flow<List<TodoCategoryEntity>> = dao.getAllCategories()
    fun getAllDeletedTodos(): Flow<List<TodoEntity>> = dao.getAllDeletedTodos()

    suspend fun insertTodo(
        title: String,
        priority: Int = 1,
        dueDate: Long? = null,
        hasTime: Boolean = false,
        categoryId: Long? = null,
        noteId: Long? = null,
        notes: String? = null
    ): Long {
        return dao.insertTodo(
            TodoEntity(
                title = title,
                priority = priority,
                dueDate = dueDate,
                hasTime = hasTime,
                categoryId = categoryId,
                noteId = noteId,
                notes = notes
            )
        )
    }

    suspend fun updateTodo(todo: TodoEntity) {
        dao.updateTodo(todo.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun softDeleteTodo(id: Long) = dao.softDeleteTodo(id)

    suspend fun restoreTodo(id: Long) = dao.restoreTodo(id)

    suspend fun permanentlyDeleteTodo(todo: TodoEntity) = dao.permanentlyDeleteTodo(todo)

    suspend fun insertCategory(name: String, color: Long = 0xFF6C63FF, isDefault: Boolean = false): Long {
        return dao.insertCategory(TodoCategoryEntity(name = name, color = color, isDefault = isDefault))
    }
}