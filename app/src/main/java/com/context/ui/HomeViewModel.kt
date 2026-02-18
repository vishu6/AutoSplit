package com.context.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.context.data.ExpenseDao
import com.context.utils.DateFilterUtils
import com.context.utils.TimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val expenseDao: ExpenseDao
) : ViewModel() {

    val groups = expenseDao.getAllGroups()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allExpenses = expenseDao.getAllExpenses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    private val _selectedTimeRange = MutableStateFlow(TimeRange.MONTH)
    val selectedTimeRange = _selectedTimeRange.asStateFlow()
    
    private val _currentCalendar = MutableStateFlow(Calendar.getInstance())
    val currentCalendar = _currentCalendar.asStateFlow()

    val filteredExpenses = combine(allExpenses, selectedTimeRange, currentCalendar) { expenses, range, calendar ->
        val (start, end) = DateFilterUtils.getTimeRange(range, calendar)
        if (range == TimeRange.ALL) {
            expenses
        } else {
            expenses.filter { it.timestamp in start..end }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredTotalSpent = filteredExpenses.map { expenses ->
        expenses
            .filter { it.category != "Settlement" }
            .sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    fun onTimeRangeSelected(range: TimeRange) {
        _selectedTimeRange.value = range
        _currentCalendar.value = Calendar.getInstance() // Reset to current date on new selection
    }

    fun onNextPeriod() {
        val newCal = _currentCalendar.value.clone() as Calendar
        when (selectedTimeRange.value) {
            TimeRange.TODAY -> newCal.add(Calendar.DAY_OF_YEAR, 1)
            TimeRange.WEEK -> newCal.add(Calendar.WEEK_OF_YEAR, 1)
            TimeRange.MONTH -> newCal.add(Calendar.MONTH, 1)
            TimeRange.YEAR -> newCal.add(Calendar.YEAR, 1)
            TimeRange.ALL -> { /* Do nothing */ }
        }
        _currentCalendar.value = newCal
    }

    fun onPreviousPeriod() {
        val newCal = _currentCalendar.value.clone() as Calendar
        when (selectedTimeRange.value) {
            TimeRange.TODAY -> newCal.add(Calendar.DAY_OF_YEAR, -1)
            TimeRange.WEEK -> newCal.add(Calendar.WEEK_OF_YEAR, -1)
            TimeRange.MONTH -> newCal.add(Calendar.MONTH, -1)
            TimeRange.YEAR -> newCal.add(Calendar.YEAR, -1)
            TimeRange.ALL -> { /* Do nothing */ }
        }
        _currentCalendar.value = newCal
    }
}