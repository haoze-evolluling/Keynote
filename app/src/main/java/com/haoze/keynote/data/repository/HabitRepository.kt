package com.haoze.keynote.data.repository

import com.haoze.keynote.data.db.dao.HabitDao
import com.haoze.keynote.data.db.entity.HabitCheckInEntity
import com.haoze.keynote.data.db.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

class HabitRepository(private val dao: HabitDao) {

    fun getActiveHabits(): Flow<List<HabitEntity>> = dao.getActiveHabits()

    fun getAllDeletedHabits(): Flow<List<HabitEntity>> = dao.getDeletedHabits()

    fun getCheckInsSince(startDay: Long): Flow<List<HabitCheckInEntity>> {
        return dao.getCheckInsSince(startDay)
    }

    suspend fun insertHabit(
        title: String,
        description: String = "",
        targetDaysPerWeek: Int = 7,
        color: Long = 0xFF4CAF50
    ): Long {
        return dao.insertHabit(
            HabitEntity(
                title = title,
                description = description,
                targetDaysPerWeek = targetDaysPerWeek.coerceIn(1, 7),
                color = color
            )
        )
    }

    suspend fun updateHabit(habit: HabitEntity) {
        dao.updateHabit(
            habit.copy(
                targetDaysPerWeek = habit.targetDaysPerWeek.coerceIn(1, 7),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun softDeleteHabit(id: Long) {
        dao.softDeleteHabit(id)
    }

    suspend fun restoreHabit(id: Long) {
        dao.restoreHabit(id)
    }

    suspend fun permanentlyDeleteHabit(habit: HabitEntity) {
        dao.deleteCheckInsForHabit(habit.id)
        dao.permanentlyDeleteHabit(habit.id)
    }

    suspend fun toggleCheckIn(habitId: Long, dayStart: Long) {
        val existing = dao.getCheckIn(habitId, dayStart)
        if (existing == null) {
            dao.upsertCheckIn(HabitCheckInEntity(habitId = habitId, dayStartMillis = dayStart))
        } else {
            dao.deleteCheckIn(habitId, dayStart)
        }
    }
}