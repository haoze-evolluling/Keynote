package com.haoze.keynote.data.db.dao

import androidx.room.*
import com.haoze.keynote.data.db.entity.AaSplitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AaSplitDao {
    @Insert
    suspend fun insertAaSplit(aaSplit: AaSplitEntity): Long

    @Query("SELECT * FROM aa_splits ORDER BY date DESC")
    fun getAllAaSplits(): Flow<List<AaSplitEntity>>

    @Query("SELECT * FROM aa_splits WHERE date >= :start AND date <= :end ORDER BY date DESC")
    fun getAaSplitsByDateRange(start: Long, end: Long): Flow<List<AaSplitEntity>>

    @Delete
    suspend fun deleteAaSplit(aaSplit: AaSplitEntity)
}
