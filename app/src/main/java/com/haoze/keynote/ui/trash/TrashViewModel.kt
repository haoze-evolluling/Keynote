package com.haoze.keynote.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.db.entity.AIChatConversationEntity
import com.haoze.keynote.data.db.entity.BillEntity
import com.haoze.keynote.data.db.entity.HabitEntity
import com.haoze.keynote.data.db.entity.NoteWithTags
import com.haoze.keynote.data.db.entity.ScheduleEntity
import com.haoze.keynote.data.db.entity.TodoEntity
import com.haoze.keynote.data.repository.AIChatRepository
import com.haoze.keynote.data.repository.BillRepository
import com.haoze.keynote.data.repository.HabitRepository
import com.haoze.keynote.data.repository.NoteRepository
import com.haoze.keynote.data.repository.ScheduleRepository
import com.haoze.keynote.data.repository.TodoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class TrashItem {
    abstract val deletedAt: Long

    data class TrashNote(val data: NoteWithTags) : TrashItem() {
        override val deletedAt: Long get() = data.note.deletedAt ?: data.note.updatedAt
    }

    data class TrashBill(val data: BillEntity) : TrashItem() {
        override val deletedAt: Long get() = data.deletedAt ?: 0
    }

    data class TrashSchedule(val data: ScheduleEntity) : TrashItem() {
        override val deletedAt: Long get() = data.deletedAt ?: 0
    }

    data class TrashTodo(val data: TodoEntity) : TrashItem() {
        override val deletedAt: Long get() = data.deletedAt ?: data.updatedAt
    }

    data class TrashHabit(val data: HabitEntity) : TrashItem() {
        override val deletedAt: Long get() = data.deletedAt ?: data.updatedAt
    }

    data class TrashAIChatConversation(val data: AIChatConversationEntity) : TrashItem() {
        override val deletedAt: Long get() = data.deletedAt ?: data.updatedAt
    }
}

class TrashViewModel(
    private val noteRepository: NoteRepository,
    private val billRepository: BillRepository,
    private val scheduleRepository: ScheduleRepository,
    private val todoRepository: TodoRepository,
    private val habitRepository: HabitRepository,
    private val aiChatRepository: AIChatRepository
) : ViewModel() {

    private val _trashItems = MutableStateFlow<List<TrashItem>>(emptyList())
    val trashItems: StateFlow<List<TrashItem>> = _trashItems.asStateFlow()

    init {
        viewModelScope.launch {
            val existingTrashFlow = combine(
                noteRepository.getAllDeletedNotes(),
                billRepository.getAllDeletedBills(),
                scheduleRepository.getAllDeletedSchedules(),
                todoRepository.getAllDeletedTodos(),
                habitRepository.getAllDeletedHabits()
            ) { notes, bills, schedules, todos, habits ->
                val items = mutableListOf<TrashItem>()
                notes.mapTo(items) { TrashItem.TrashNote(it) }
                bills.mapTo(items) { TrashItem.TrashBill(it) }
                schedules.mapTo(items) { TrashItem.TrashSchedule(it) }
                todos.mapTo(items) { TrashItem.TrashTodo(it) }
                habits.mapTo(items) { TrashItem.TrashHabit(it) }
                items
            }

            combine(existingTrashFlow, aiChatRepository.getDeletedConversations()) { items, conversations ->
                (items + conversations.map { TrashItem.TrashAIChatConversation(it) })
                    .sortedByDescending { it.deletedAt }
            }.collect { _trashItems.value = it }
        }
    }

    fun restore(item: TrashItem) {
        viewModelScope.launch {
            when (item) {
                is TrashItem.TrashNote -> noteRepository.restoreNote(item.data.note)
                is TrashItem.TrashBill -> billRepository.restore(item.data)
                is TrashItem.TrashSchedule -> scheduleRepository.restore(item.data)
                is TrashItem.TrashTodo -> todoRepository.restoreTodo(item.data.id)
                is TrashItem.TrashHabit -> habitRepository.restoreHabit(item.data.id)
                is TrashItem.TrashAIChatConversation -> aiChatRepository.restoreConversation(item.data.id)
            }
        }
    }

    fun permanentlyDelete(item: TrashItem) {
        viewModelScope.launch {
            when (item) {
                is TrashItem.TrashNote -> noteRepository.permanentlyDeleteNote(item.data.note)
                is TrashItem.TrashBill -> billRepository.permanentlyDelete(item.data)
                is TrashItem.TrashSchedule -> scheduleRepository.permanentlyDelete(item.data)
                is TrashItem.TrashTodo -> todoRepository.permanentlyDeleteTodo(item.data)
                is TrashItem.TrashHabit -> habitRepository.permanentlyDeleteHabit(item.data)
                is TrashItem.TrashAIChatConversation -> aiChatRepository.permanentlyDeleteConversation(item.data.id)
            }
        }
    }
}