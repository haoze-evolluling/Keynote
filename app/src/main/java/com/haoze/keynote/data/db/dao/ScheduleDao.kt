package com.haoze.keynote.data.db.dao

import androidx.room.*
import com.haoze.keynote.data.db.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Insert
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Query("SELECT * FROM schedules WHERE isDeleted = 0 ORDER BY date ASC")
    fun getAllSchedules(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): ScheduleEntity?

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity)

    @Query("SELECT * FROM schedules WHERE isDeleted = 0 AND date BETWEEN :start AND :end ORDER BY date ASC")
    fun getSchedulesByDateRange(start: Long, end: Long): Flow<List<ScheduleEntity>>

    @Query("UPDATE schedules SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteSchedule(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE schedules SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreSchedule(id: Long)

    @Query("SELECT * FROM schedules WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getAllDeletedSchedules(): Flow<List<ScheduleEntity>>

    @Query("DELETE FROM schedules WHERE isDeleted = 1 AND deletedAt < :expireTime")
    suspend fun deleteExpiredTrashSchedules(expireTime: Long)
}
