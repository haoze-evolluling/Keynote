package com.haoze.keynote.ui.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.db.entity.HabitEntity
import com.haoze.keynote.data.repository.HabitRepository
import com.haoze.keynote.util.AppConstants
import com.haoze.keynote.util.toDayStartMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HabitViewModel(
    private val repository: HabitRepository
) : ViewModel() {
    private val todayStart: Long
        get() = System.currentTimeMillis().toDayStartMillis()

    private val checkInStart: Long
        get() = todayStart - 60L * 86_400_000L

    private val activeHabits = repository.getActiveHabits()
    private val recentCheckIns = repository.getCheckInsSince(checkInStart)

    val progressItems: StateFlow<List<HabitProgressItem>> = combine(
        activeHabits,
        recentCheckIns
    ) { habits, checkIns ->
        buildHabitProgressItems(
            habits = habits,
            checkIns = checkIns.map {
                HabitCheckInSnapshot(
                    habitId = it.habitId,
                    dayStartMillis = it.dayStartMillis
                )
            },
            todayStart = todayStart
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.STATE_IN_TIMEOUT_MILLIS), emptyList())

    private val _showEditor = MutableStateFlow(false)
    val showEditor: StateFlow<Boolean> = _showEditor.asStateFlow()

    private val _editingHabit = MutableStateFlow<HabitEntity?>(null)
    val editingHabit: StateFlow<HabitEntity?> = _editingHabit.asStateFlow()

    fun openCreateEditor() {
        _editingHabit.value = null
        _showEditor.value = true
    }

    fun openEditEditor(habit: HabitEntity) {
        _editingHabit.value = HabitEntity(
            id = habit.id,
            title = habit.title,
            description = habit.description,
            targetDaysPerWeek = habit.targetDaysPerWeek,
            color = habit.color,
            createdAt = habit.createdAt
        )
        _showEditor.value = true
    }

    fun dismissEditor() {
        _showEditor.value = false
        _editingHabit.value = null
    }

    fun saveHabit(
        title: String,
        description: String,
        targetDaysPerWeek: Int,
        color: Long
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val editing = _editingHabit.value
            if (editing == null) {
                repository.insertHabit(
                    title = title.trim(),
                    description = description.trim(),
                    targetDaysPerWeek = targetDaysPerWeek,
                    color = color
                )
            } else {
                repository.updateHabit(
                    editing.copy(
                        title = title.trim(),
                        description = description.trim(),
                        targetDaysPerWeek = targetDaysPerWeek,
                        color = color
                    )
                )
            }
            dismissEditor()
        }
    }

    fun softDeleteHabit(habitId: Long) {
        viewModelScope.launch {
            repository.softDeleteHabit(habitId)
        }
    }

    fun toggleTodayCheckIn(habitId: Long) {
        viewModelScope.launch {
            repository.toggleCheckIn(habitId, todayStart)
        }
    }
}
