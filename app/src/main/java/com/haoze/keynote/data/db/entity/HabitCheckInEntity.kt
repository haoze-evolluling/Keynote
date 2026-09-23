package com.haoze.keynote.data.db.entity

import androidx.room.Entity

@Entity(
    tableName = "habit_check_ins",
    primaryKeys = ["habitId", "dayStartMillis"]
)
data class HabitCheckInEntity(
    val habitId: Long,
    val dayStartMillis: Long,
    val checkedAt: Long = System.currentTimeMillis(),
    val note: String? = null
)
