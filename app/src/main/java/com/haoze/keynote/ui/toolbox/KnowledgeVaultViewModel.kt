package com.haoze.keynote.ui.toolbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.db.entity.KnowledgeVaultEntity
import com.haoze.keynote.data.repository.SmartModuleRepository
import com.haoze.keynote.util.AppConstants
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KnowledgeVaultViewModel(
    private val repository: SmartModuleRepository
) : ViewModel() {

    val knowledgeItems = repository.knowledgeItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.STATE_IN_TIMEOUT_MILLIS), emptyList())

    fun saveKnowledgeItem(item: KnowledgeVaultEntity) = viewModelScope.launch { repository.saveKnowledgeItem(item) }

    fun deleteKnowledgeItem(item: KnowledgeVaultEntity) = viewModelScope.launch { repository.deleteKnowledgeItem(item) }
}