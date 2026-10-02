package com.haoze.keynote.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 双人请客平衡账本实体
 * 记录请客账本信息及双方参与者姓名
 */
@Entity(tableName = "treat_ledgers")
data class TreatLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val personAName: String = "我",
    val personBName: String = "对方",
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false
)
