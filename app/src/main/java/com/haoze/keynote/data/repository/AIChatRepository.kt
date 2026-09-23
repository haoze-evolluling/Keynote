package com.haoze.keynote.data.repository

import com.haoze.keynote.data.db.dao.AIChatDao
import com.haoze.keynote.data.db.entity.AIChatConversationEntity
import com.haoze.keynote.data.db.entity.AIChatMessageEntity
import kotlinx.coroutines.flow.Flow

class AIChatRepository(private val dao: AIChatDao) {
    fun getActiveConversations(): Flow<List<AIChatConversationEntity>> = dao.getActiveConversations()

    fun getDeletedConversations(): Flow<List<AIChatConversationEntity>> = dao.getDeletedConversations()

    suspend fun getMessages(conversationId: Long): List<AIChatMessageEntity> = dao.getMessages(conversationId)

    suspend fun createConversation(assistantType: String, title: String): Long {
        return dao.insertConversation(
            AIChatConversationEntity(
                assistantType = assistantType,
                title = title
            )
        )
    }

    suspend fun addMessage(conversationId: Long, message: AIChatMessageEntity): Long {
        val id = dao.insertMessage(message.copy(conversationId = conversationId))
        dao.updateConversationTime(conversationId)
        return id
    }

    suspend fun replaceMessages(conversationId: Long, messages: List<AIChatMessageEntity>) {
        dao.clearMessages(conversationId)
        messages.forEach { dao.insertMessage(it.copy(id = 0, conversationId = conversationId)) }
        dao.updateConversationTime(conversationId)
    }

    suspend fun softDeleteConversation(id: Long) = dao.softDeleteConversation(id)

    suspend fun restoreConversation(id: Long) = dao.restoreConversation(id)

    suspend fun permanentlyDeleteConversation(id: Long) = dao.permanentlyDeleteConversation(id)
}
