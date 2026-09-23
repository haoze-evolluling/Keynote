package com.haoze.keynote.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aa_splits")
data class AaSplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val totalAmount: Double,
    val personCount: Int,
    val perPersonAmount: Double,
    val date: Long = System.currentTimeMillis(),
    val note: String? = null
)
