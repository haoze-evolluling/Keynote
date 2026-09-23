package com.haoze.keynote.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.haoze.keynote.data.db.entity.KnowledgeVaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmartModuleDao {
    @Query("SELECT * FROM knowledge_vault_items ORDER BY isPinned DESC, updatedAt DESC")
    fun getKnowledgeItems(): Flow<List<KnowledgeVaultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKnowledgeItem(item: KnowledgeVaultEntity): Long

    @Delete
    suspend fun deleteKnowledgeItem(item: KnowledgeVaultEntity)
}