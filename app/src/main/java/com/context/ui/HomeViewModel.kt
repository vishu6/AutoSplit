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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive = _isSearchActive.asStateFlow()

    val filteredExpenses = combine(
        allExpenses, 
        selectedTimeRange, 
        currentCalendar, 
        searchQuery, 
        isSearchActive
    ) { expenses, range, calendar, query, searchActive ->
        val (start, end) = DateFilterUtils.getTimeRange(range, calendar)
        
        expenses.filter { expense ->
            val matchesTime = if (range == TimeRange.ALL || searchActive) true else expense.timestamp in start..end
            val matchesSearch = if (searchActive && query.isNotEmpty()) {
                expense.merchant.contains(query, ignoreCase = true) || 
                expense.amount.toString().contains(query) ||
                expense.category.contains(query, ignoreCase = true)
            } else true
            
            matchesTime && matchesSearch
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

    val previousPeriodTotalSpent = combine(allExpenses, selectedTimeRange, currentCalendar) { expenses, range, calendar ->
        val previousCalendar = calendar.clone() as Calendar
        when (range) {
            TimeRange.TODAY -> previousCalendar.add(Calendar.DAY_OF_YEAR, -1)
            TimeRange.WEEK -> previousCalendar.add(Calendar.WEEK_OF_YEAR, -1)
            TimeRange.MONTH -> previousCalendar.add(Calendar.MONTH, -1)
            TimeRange.YEAR -> previousCalendar.add(Calendar.YEAR, -1)
            TimeRange.ALL -> { /* No comparison for ALL */ }
        }
        val (start, end) = DateFilterUtils.getTimeRange(range, previousCalendar)
        expenses
            .filter { it.timestamp in start..end && it.category != "Settlement" }
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

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }
}
