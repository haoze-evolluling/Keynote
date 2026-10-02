package com.haoze.keynote.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 请客消费记录实体
 * 记录单笔请客消费明细以及双方实际承担金额
 */
@Entity(
    tableName = "treat_records",
    foreignKeys = [
        ForeignKey(
            entity = TreatLedgerEntity::class,
            parentColumns = ["id"],
            childColumns = ["ledgerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["ledgerId"])]
)
data class TreatRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ledgerId: Long,
    val title: String,
    val amount: Double,
    val payer: Int, // 0: personA, 1: personB, 2: 自定义分担
    val amountA: Double,
    val amountB: Double,
    val date: Long = System.currentTimeMillis(),
    val note: String? = null
)
