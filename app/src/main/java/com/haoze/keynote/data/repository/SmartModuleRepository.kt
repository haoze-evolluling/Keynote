package com.haoze.keynote.data.repository

import com.haoze.keynote.data.db.dao.SmartModuleDao
import com.haoze.keynote.data.db.entity.KnowledgeVaultEntity

class SmartModuleRepository(private val dao: SmartModuleDao) {
    val knowledgeItems = dao.getKnowledgeItems()

    suspend fun saveKnowledgeItem(item: KnowledgeVaultEntity) = dao.upsertKnowledgeItem(item.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteKnowledgeItem(item: KnowledgeVaultEntity) = dao.deleteKnowledgeItem(item)
}