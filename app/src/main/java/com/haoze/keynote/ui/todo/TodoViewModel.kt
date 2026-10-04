package com.haoze.keynote.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.db.entity.TodoCategoryEntity
import com.haoze.keynote.data.db.entity.TodoEntity
import com.haoze.keynote.data.repository.TodoRepository
import com.haoze.keynote.util.AppConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(
    private val repository: TodoRepository
) : ViewModel() {

    val todos: StateFlow<List<TodoEntity>> = repository.getAllActiveTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.STATE_IN_TIMEOUT_MILLIS), emptyList())

    val categories: StateFlow<List<TodoCategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.STATE_IN_TIMEOUT_MILLIS), emptyList())

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private val _editingTodo = MutableStateFlow<TodoEntity?>(null)
    val editingTodo: StateFlow<TodoEntity?> = _editingTodo.asStateFlow()

    fun openCreateDialog() {
        _editingTodo.value = null
        _showDialog.value = true
    }

    fun openEditDialog(todo: TodoEntity) {
        _editingTodo.value = todo
        _showDialog.value = true
    }

    fun dismissDialog() {
        _showDialog.value = false
        _editingTodo.value = null
    }

    fun createTodo(
        title: String,
        priority: Int,
        dueDate: Long?,
        hasTime: Boolean,
        categoryId: Long?,
        noteId: Long?,
        notes: String?
    ) {
        viewModelScope.launch {
            repository.insertTodo(
                title = title,
                priority = priority,
                dueDate = dueDate,
                hasTime = hasTime,
                categoryId = categoryId,
                noteId = noteId,
                notes = notes
            )
            dismissDialog()
        }
    }

    fun updateTodo(todo: TodoEntity) {
        viewModelScope.launch {
            repository.updateTodo(todo)
            dismissDialog()
        }
    }

    fun deleteTodo(todo: TodoEntity) {
        viewModelScope.launch {
            repository.softDeleteTodo(todo.id)
        }
    }

    fun toggleComplete(todo: TodoEntity) {
        viewModelScope.launch {
            repository.updateTodo(todo.copy(isCompleted = !todo.isCompleted, updatedAt = System.currentTimeMillis()))
        }
    }
}
