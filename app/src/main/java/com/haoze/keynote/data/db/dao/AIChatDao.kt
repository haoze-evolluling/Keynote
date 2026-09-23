package com.haoze.keynote.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.haoze.keynote.data.db.entity.AIChatConversationEntity
import com.haoze.keynote.data.db.entity.AIChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIChatDao {
    @Query("SELECT * FROM ai_chat_conversations WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getActiveConversations(): Flow<List<AIChatConversationEntity>>

    @Query("SELECT * FROM ai_chat_conversations WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedConversations(): Flow<List<AIChatConversationEntity>>

    @Query("SELECT * FROM ai_chat_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC, id ASC")
    suspend fun getMessages(conversationId: Long): List<AIChatMessageEntity>

    @Query("DELETE FROM ai_chat_messages WHERE conversationId = :conversationId")
    suspend fun clearMessages(conversationId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: AIChatConversationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AIChatMessageEntity): Long

    @Query("UPDATE ai_chat_conversations SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateConversationTime(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE ai_chat_conversations SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteConversation(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE ai_chat_conversations SET isDeleted = 0, deletedAt = NULL, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreConversation(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM ai_chat_conversations WHERE id = :id")
    suspend fun permanentlyDeleteConversation(id: Long)

    @Query("DELETE FROM ai_chat_conversations WHERE isDeleted = 1 AND deletedAt < :expireTime")
    suspend fun deleteExpiredTrashConversations(expireTime: Long)
}
