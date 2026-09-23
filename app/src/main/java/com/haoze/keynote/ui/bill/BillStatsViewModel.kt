package com.haoze.keynote.ui.bill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.db.entity.CategorySpending
import com.haoze.keynote.data.db.entity.DailySpending
import com.haoze.keynote.data.db.entity.MonthlySpending
import com.haoze.keynote.data.db.entity.WeeklySpending
import com.haoze.keynote.data.repository.BillRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class RangePreset { THIS_WEEK, THIS_MONTH, THIS_YEAR, ALL, CUSTOM }
enum class Granularity { DAY, WEEK, MONTH }

class BillStatsViewModel(
    private val repository: BillRepository
) : ViewModel() {

    private val _dateRange = MutableStateFlow<Pair<Long, Long>>(getMonthRange())
    val dateRange: StateFlow<Pair<Long, Long>> = _dateRange.asStateFlow()

    private val _totalSpending = MutableStateFlow(0.0)
    val totalSpending: StateFlow<Double> = _totalSpending.asStateFlow()

    private val _billCount = MutableStateFlow(0)
    val billCount: StateFlow<Int> = _billCount.asStateFlow()

    private val _categoryStats = MutableStateFlow<List<CategorySpending>>(emptyList())
    val categoryStats: StateFlow<List<CategorySpending>> = _categoryStats.asStateFlow()

    private val _monthlyTrend = MutableStateFlow<List<MonthlySpending>>(emptyList())
    val monthlyTrend: StateFlow<List<MonthlySpending>> = _monthlyTrend.asStateFlow()

    private val _granularity = MutableStateFlow(Granularity.MONTH)
    val granularity: StateFlow<Granularity> = _granularity.asStateFlow()

    private val _dailySpending = MutableStateFlow<List<DailySpending>>(emptyList())
    val dailySpending: StateFlow<List<DailySpending>> = _dailySpending.asStateFlow()

    val dailyTrend: StateFlow<List<DailySpending>>
        get() = _dailySpending

    private val _weeklyTrend = MutableStateFlow<List<WeeklySpending>>(emptyList())
    val weeklyTrend: StateFlow<List<WeeklySpending>> = _weeklyTrend.asStateFlow()

    init {
        collectStats()
    }

    fun setPresetRange(preset: RangePreset) {
        _dateRange.value = when (preset) {
            RangePreset.THIS_WEEK -> getWeekRange()
            RangePreset.THIS_MONTH -> getMonthRange()
            RangePreset.THIS_YEAR -> getYearRange()
            RangePreset.ALL -> Pair(0L, System.currentTimeMillis())
            RangePreset.CUSTOM -> _dateRange.value
        }
    }

    fun setCustomRange(start: Long, end: Long) {
        _dateRange.value = Pair(start, end)
    }

    fun setGranularity(g: Granularity) {
        _granularity.value = g
    }

    private fun collectStats() {
        viewModelScope.launch {
            _dateRange.collectLatest { (start, end) ->
                launch {
                    repository.getSpendingInRange(start, end).collect {
                        _totalSpending.value = it ?: 0.0
                    }
                }
                launch {
                    repository.getBillCountInRange(start, end).collect {
                        _billCount.value = it
                    }
                }
                launch {
                    repository.getSpendingByCategory(start, end).collect {
                        _categoryStats.value = it
                    }
                }
                launch {
                    repository.getMonthlySpendingTrend(start, end).collect {
                        _monthlyTrend.value = it
                    }
                }
                launch {
                    repository.getDailySpending(start, end).collect {
                        _dailySpending.value = it
                    }
                }
                launch {
                    repository.getWeeklySpending(start, end).collect {
                        _weeklyTrend.value = it
                    }
                }
            }
        }
    }

    private fun toEpochMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun getWeekRange(): Pair<Long, Long> {
        val today = LocalDate.now()
        val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return Pair(toEpochMillis(start), System.currentTimeMillis())
    }

    private fun getMonthRange(): Pair<Long, Long> {
        val start = LocalDate.now().withDayOfMonth(1)
        return Pair(toEpochMillis(start), System.currentTimeMillis())
    }

    private fun getYearRange(): Pair<Long, Long> {
        val start = LocalDate.now().withDayOfYear(1)
        return Pair(toEpochMillis(start), System.currentTimeMillis())
    }
}