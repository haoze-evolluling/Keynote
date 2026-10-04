package com.haoze.keynote.data.db.dao

import androidx.room.*
import com.haoze.keynote.data.db.entity.TreatLedgerEntity
import com.haoze.keynote.data.db.entity.TreatRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TreatDao {

    // --- 账本相关 ---
    @Query("SELECT * FROM treat_ledgers ORDER BY isDefault DESC, createdAt ASC")
    fun getAllLedgers(): Flow<List<TreatLedgerEntity>>

    @Query("SELECT * FROM treat_ledgers WHERE id = :id LIMIT 1")
    fun getLedgerById(id: Long): Flow<TreatLedgerEntity?>

    @Query("SELECT * FROM treat_ledgers WHERE id = :id LIMIT 1")
    suspend fun getLedgerByIdSync(id: Long): TreatLedgerEntity?

    @Query("SELECT * FROM treat_ledgers WHERE isDefault = 1 LIMIT 1")
    fun getDefaultLedger(): Flow<TreatLedgerEntity?>

    @Query("SELECT COUNT(*) FROM treat_ledgers")
    suspend fun getLedgerCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedger(ledger: TreatLedgerEntity): Long

    @Update
    suspend fun updateLedger(ledger: TreatLedgerEntity)

    @Delete
    suspend fun deleteLedger(ledger: TreatLedgerEntity)

    // --- 记录相关 ---
    @Query("SELECT * FROM treat_records WHERE ledgerId = :ledgerId ORDER BY date DESC, id DESC")
    fun getRecordsByLedger(ledgerId: Long): Flow<List<TreatRecordEntity>>

    @Query("SELECT * FROM treat_records WHERE ledgerId = :ledgerId ORDER BY date ASC, id ASC")
    fun getRecordsByLedgerAsc(ledgerId: Long): Flow<List<TreatRecordEntity>>

    @Query("SELECT * FROM treat_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<TreatRecordEntity>>

    @Query("SELECT * FROM treat_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): TreatRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TreatRecordEntity): Long

    @Update
    suspend fun updateRecord(record: TreatRecordEntity)

    @Delete
    suspend fun deleteRecord(record: TreatRecordEntity)

    @Query("DELETE FROM treat_records WHERE ledgerId = :ledgerId")
    suspend fun deleteRecordsByLedger(ledgerId: Long)
}
