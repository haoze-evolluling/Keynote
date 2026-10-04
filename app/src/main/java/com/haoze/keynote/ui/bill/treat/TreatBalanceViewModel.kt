package com.haoze.keynote.ui.bill.treat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.db.entity.TreatLedgerEntity
import com.haoze.keynote.data.db.entity.TreatRecordEntity
import com.haoze.keynote.data.repository.TreatRepository
import com.haoze.keynote.util.AppConstants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TreatBalanceViewModel(
    private val repository: TreatRepository
) : ViewModel() {

    val ledgers: StateFlow<List<TreatLedgerEntity>> = repository.getAllLedgers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.STATE_IN_TIMEOUT_MILLIS), emptyList())

    private val _selectedLedgerId = MutableStateFlow<Long?>(null)
    val selectedLedgerId: StateFlow<Long?> = _selectedLedgerId.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultLedger()
        }
        viewModelScope.launch {
            ledgers.collect { list ->
                if (_selectedLedgerId.value == null && list.isNotEmpty()) {
                    val defaultOne = list.find { it.isDefault } ?: list.first()
                    _selectedLedgerId.value = defaultOne.id
                } else if (_selectedLedgerId.value != null && list.none { it.id == _selectedLedgerId.value }) {
                    _selectedLedgerId.value = list.firstOrNull()?.id
                }
            }
        }
    }

    val currentLedger: StateFlow<TreatLedgerEntity?> = combine(ledgers, _selectedLedgerId) { list, id ->
        list.find { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.STATE_IN_TIMEOUT_MILLIS), null)

    val records: StateFlow<List<TreatRecordEntity>> = _selectedLedgerId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.getRecordsByLedger(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.STATE_IN_TIMEOUT_MILLIS), emptyList())

    val summary: StateFlow<TreatLedgerSummary?> = combine(currentLedger, records) { ledger, list ->
        if (ledger != null) {
            TreatBalanceCalculator.calculateSummary(ledger, list)
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.STATE_IN_TIMEOUT_MILLIS), null)

    val recordsWithBalance: StateFlow<List<TreatRecordWithBalance>> = combine(currentLedger, records) { ledger, list ->
        if (ledger != null) {
            TreatBalanceCalculator.calculateRecordsWithBalance(ledger, list)
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.STATE_IN_TIMEOUT_MILLIS), emptyList())

    fun selectLedger(id: Long) {
        _selectedLedgerId.value = id
    }

    fun createLedger(name: String, personAName: String, personBName: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val newId = repository.insertLedger(name, personAName, personBName)
            _selectedLedgerId.value = newId
            onCreated(newId)
        }
    }

    fun updateLedger(ledger: TreatLedgerEntity) {
        viewModelScope.launch {
            repository.updateLedger(ledger)
        }
    }

    fun deleteLedger(ledger: TreatLedgerEntity) {
        viewModelScope.launch {
            repository.deleteLedger(ledger)
        }
    }

    fun createRecord(
        title: String,
        amount: Double,
        payer: Int,
        amountA: Double,
        amountB: Double,
        date: Long,
        note: String? = null
    ) {
        val ledgerId = _selectedLedgerId.value ?: return
        viewModelScope.launch {
            repository.insertRecord(
                ledgerId = ledgerId,
                title = title,
                amount = amount,
                payer = payer,
                amountA = amountA,
                amountB = amountB,
                date = date,
                note = note
            )
        }
    }

    fun updateRecord(record: TreatRecordEntity) {
        viewModelScope.launch {
            repository.updateRecord(record)
        }
    }

    fun deleteRecord(record: TreatRecordEntity) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }

    fun settleDifference(title: String = "差额平衡结清") {
        val ledger = currentLedger.value ?: return
        val currentSummary = summary.value ?: return
        val gap = currentSummary.absDiff
        if (gap < 0.01) return

        // 差额由支出少的一方补足，使其支出增加 gap
        val (payer, amtA, amtB) = if (currentSummary.totalA < currentSummary.totalB) {
            // A 支出少，由 A 承担
            Triple(0, gap, 0.0)
        } else {
            // B 支出少，由 B 承担
            Triple(1, 0.0, gap)
        }

        viewModelScope.launch {
            repository.insertRecord(
                ledgerId = ledger.id,
                title = title,
                amount = gap,
                payer = payer,
                amountA = amtA,
                amountB = amtB,
                date = System.currentTimeMillis(),
                note = "系统结算记录：抹平历史累计消费差额"
            )
        }
    }
}
