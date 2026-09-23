package com.haoze.keynote.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bills",
    indices = [Index("categoryId")]
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val item: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val categoryId: Long? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
