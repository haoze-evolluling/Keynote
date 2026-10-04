package com.haoze.keynote.ui.bill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.db.entity.BillEntity
import com.haoze.keynote.data.db.entity.BillWithCategoryRaw
import com.haoze.keynote.data.db.entity.CategoryEntity
import com.haoze.keynote.data.repository.BillRepository
import com.haoze.keynote.util.AppConstants
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BillViewModel(
    private val repository: BillRepository
) : ViewModel() {

    val bills: StateFlow<List<BillEntity>>
    val billsWithCategory: StateFlow<List<BillWithCategoryRaw>>
    val categories: StateFlow<List<CategoryEntity>>

    init {
        bills = repository.getAllBills()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.STATE_IN_TIMEOUT_MILLIS), emptyList())
        billsWithCategory = repository.getBillsWithCategory()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.STATE_IN_TIMEOUT_MILLIS), emptyList())
        categories = repository.getAllCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.STATE_IN_TIMEOUT_MILLIS), emptyList())
    }

    fun updateBill(bill: BillEntity) {
        viewModelScope.launch { repository.updateBill(bill) }
    }

    fun deleteBill(bill: BillEntity) {
        viewModelScope.launch { repository.softDelete(bill) }
    }

    fun createBill(item: String, amount: Double, date: Long, categoryId: Long? = null) {
        viewModelScope.launch { repository.insertBill(item, amount, date, categoryId) }
    }

    fun addCategory(name: String) {
        viewModelScope.launch { repository.insertCategory(name) }
    }
}
