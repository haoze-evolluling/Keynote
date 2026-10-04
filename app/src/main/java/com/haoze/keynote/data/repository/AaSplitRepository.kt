package com.haoze.keynote.data.repository

import com.haoze.keynote.data.db.dao.AaSplitDao
import com.haoze.keynote.data.db.entity.AaSplitEntity
import kotlinx.coroutines.flow.Flow

class AaSplitRepository(
    private val aaSplitDao: AaSplitDao
) {

    fun getAllAaSplits(): Flow<List<AaSplitEntity>> = aaSplitDao.getAllAaSplits()

    suspend fun insertAaSplit(title: String, totalAmount: Double, personCount: Int, note: String? = null): Long {
        val perPersonAmount = totalAmount / personCount
        return aaSplitDao.insertAaSplit(
            AaSplitEntity(
                title = title,
                totalAmount = totalAmount,
                personCount = personCount,
                perPersonAmount = perPersonAmount,
                note = note
            )
        )
    }

    suspend fun deleteAaSplit(aaSplit: AaSplitEntity) = aaSplitDao.deleteAaSplit(aaSplit)
}
