package com.haoze.keynote.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.haoze.keynote.data.db.entity.HabitCheckInEntity
import com.haoze.keynote.data.db.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getActiveHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("UPDATE habits SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteHabit(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE habits SET isDeleted = 0, deletedAt = NULL, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreHabit(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun permanentlyDeleteHabit(id: Long)

    @Query("SELECT * FROM habits WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedHabits(): Flow<List<HabitEntity>>

    @Query("DELETE FROM habits WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun deleteExpiredHabits(cutoff: Long)

    @Query("SELECT * FROM habit_check_ins WHERE dayStartMillis >= :startDay ORDER BY dayStartMillis DESC")
    fun getCheckInsSince(startDay: Long): Flow<List<HabitCheckInEntity>>

    @Query("SELECT * FROM habit_check_ins WHERE habitId = :habitId AND dayStartMillis = :dayStart LIMIT 1")
    suspend fun getCheckIn(habitId: Long, dayStart: Long): HabitCheckInEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCheckIn(checkIn: HabitCheckInEntity)

    @Query("DELETE FROM habit_check_ins WHERE habitId = :habitId AND dayStartMillis = :dayStart")
    suspend fun deleteCheckIn(habitId: Long, dayStart: Long)

    @Query("DELETE FROM habit_check_ins WHERE habitId = :habitId")
    suspend fun deleteCheckInsForHabit(habitId: Long)
}
