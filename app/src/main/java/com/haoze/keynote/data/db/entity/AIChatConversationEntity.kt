package com.haoze.keynote.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_chat_conversations")
data class AIChatConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val assistantType: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
