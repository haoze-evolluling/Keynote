package com.haoze.keynote.data.repository

import com.haoze.keynote.data.db.dao.TreatDao
import com.haoze.keynote.data.db.entity.TreatLedgerEntity
import com.haoze.keynote.data.db.entity.TreatRecordEntity
import kotlinx.coroutines.flow.Flow

class TreatRepository(
    private val treatDao: TreatDao
) {
    fun getAllLedgers(): Flow<List<TreatLedgerEntity>> = treatDao.getAllLedgers()

    fun getLedgerById(id: Long): Flow<TreatLedgerEntity?> = treatDao.getLedgerById(id)

    suspend fun getLedgerByIdSync(id: Long): TreatLedgerEntity? = treatDao.getLedgerByIdSync(id)

    fun getDefaultLedger(): Flow<TreatLedgerEntity?> = treatDao.getDefaultLedger()

    suspend fun ensureDefaultLedger(): TreatLedgerEntity {
        val count = treatDao.getLedgerCount()
        if (count == 0) {
            val defaultLedger = TreatLedgerEntity(
                name = "默认请客账本",
                personAName = "我",
                personBName = "对方",
                isDefault = true
            )
            val id = treatDao.insertLedger(defaultLedger)
            return defaultLedger.copy(id = id)
        }
        return treatDao.getLedgerByIdSync(1) ?: TreatLedgerEntity(name = "默认请客账本", personAName = "我", personBName = "对方", isDefault = true)
    }

    suspend fun insertLedger(name: String, personAName: String, personBName: String): Long {
        return treatDao.insertLedger(
            TreatLedgerEntity(
                name = name,
                personAName = personAName.ifBlank { "我" },
                personBName = personBName.ifBlank { "对方" },
                isDefault = false
            )
        )
    }

    suspend fun updateLedger(ledger: TreatLedgerEntity) = treatDao.updateLedger(ledger)

    suspend fun deleteLedger(ledger: TreatLedgerEntity) = treatDao.deleteLedger(ledger)

    fun getRecordsByLedger(ledgerId: Long): Flow<List<TreatRecordEntity>> =
        treatDao.getRecordsByLedger(ledgerId)

    fun getRecordsByLedgerAsc(ledgerId: Long): Flow<List<TreatRecordEntity>> =
        treatDao.getRecordsByLedgerAsc(ledgerId)

    suspend fun insertRecord(
        ledgerId: Long,
        title: String,
        amount: Double,
        payer: Int,
        amountA: Double,
        amountB: Double,
        date: Long,
        note: String? = null
    ): Long {
        return treatDao.insertRecord(
            TreatRecordEntity(
                ledgerId = ledgerId,
                title = title.ifBlank { "请客消费" },
                amount = amount,
                payer = payer,
                amountA = amountA,
                amountB = amountB,
                date = date,
                note = note
            )
        )
    }

    suspend fun updateRecord(record: TreatRecordEntity) = treatDao.updateRecord(record)

    suspend fun deleteRecord(record: TreatRecordEntity) = treatDao.deleteRecord(record)

    suspend fun clearRecordsByLedger(ledgerId: Long) = treatDao.deleteRecordsByLedger(ledgerId)
}
