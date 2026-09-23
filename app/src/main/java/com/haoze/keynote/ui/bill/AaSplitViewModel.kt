package com.haoze.keynote.ui.bill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.db.entity.AaSplitEntity
import com.haoze.keynote.data.repository.AaSplitRepository
import com.haoze.keynote.util.AppConstants
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AaSplitViewModel(
    private val repository: AaSplitRepository
) : ViewModel() {

    val aaSplits: StateFlow<List<AaSplitEntity>>

    init {
        aaSplits = repository.getAllAaSplits()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.STATE_IN_TIMEOUT_MILLIS), emptyList())
    }

    fun createAaSplit(title: String, totalAmount: Double, personCount: Int, note: String? = null) {
        viewModelScope.launch {
            repository.insertAaSplit(title, totalAmount, personCount, note)
        }
    }

    fun deleteAaSplit(aaSplit: AaSplitEntity) {
        viewModelScope.launch { repository.deleteAaSplit(aaSplit) }
    }
}
