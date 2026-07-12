package com.context.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.context.data.Category
import com.context.data.Expense
import com.context.data.ExpenseDao
import com.context.sync.GroupSyncManager
import com.context.utils.BudgetUtils
import com.context.utils.DateFilterUtils
import com.context.utils.TimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val expenseDao: ExpenseDao,
    val groupSyncManager: GroupSyncManager,
    @ApplicationContext private val context: Context
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
        
    // Dynamic Category Map for icon resolution
    val categoryMap = expenseDao.getAllCategories()
        .map { list -> list.associateBy { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _selectedTimeRange = MutableStateFlow(TimeRange.MONTH)
    val selectedTimeRange = _selectedTimeRange.asStateFlow()
    
    private val _currentCalendar = MutableStateFlow(Calendar.getInstance())
    val currentCalendar = _currentCalendar.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive = _isSearchActive.asStateFlow()

    // Category selection state
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    // Category Limits state
    private val _categoryLimits = MutableStateFlow<Map<String, Double>>(emptyMap())
    val categoryLimits = _categoryLimits.asStateFlow()

    init {
        refreshBudgetLimits()
    }

    fun refreshBudgetLimits() {
        _categoryLimits.value = BudgetUtils.getCategoryLimits(context)
    }

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

    // ALWAYS accurately track the total spent this month for Proactive Budgeting
    val currentMonthTotalSpent = allExpenses.map { expenses ->
        val (start, end) = DateFilterUtils.getTimeRange(TimeRange.MONTH, Calendar.getInstance())
        expenses
            .filter { it.timestamp in start..end && it.category != "Settlement" }
            .sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
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

    // Current month spend per category for budget tracking
    val currentMonthCategorySpent = filteredExpenses.map { expenses ->
        expenses
            .filter { it.category != "Settlement" }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoryTrendData = _selectedCategory.flatMapLatest { category ->
        if (category == null) flowOf(emptyList<Pair<String, Double>>())
        else flow {
            val trend = mutableListOf<Pair<String, Double>>()
            val cal = Calendar.getInstance()
            val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

            for (i in 5 downTo 0) {
                val tempCal = cal.clone() as Calendar
                tempCal.add(Calendar.MONTH, -i)
                tempCal.set(Calendar.DAY_OF_MONTH, 1)
                tempCal.set(Calendar.HOUR_OF_DAY, 0)
                val start = tempCal.timeInMillis
                
                tempCal.set(Calendar.DAY_OF_MONTH, tempCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                tempCal.set(Calendar.HOUR_OF_DAY, 23)
                val end = tempCal.timeInMillis

                val total = expenseDao.getCategoryTotalForPeriod(category, start, end) ?: 0.0
                trend.add(monthFormat.format(tempCal.time) to total)
            }
            emit(trend)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun onTimeRangeSelected(range: TimeRange) {
        _selectedTimeRange.value = range
        _currentCalendar.value = Calendar.getInstance()
        _selectedCategory.value = null
    }

    fun onNextPeriod() {
        val newCal = _currentCalendar.value.clone() as Calendar
        when (selectedTimeRange.value) {
            TimeRange.TODAY -> newCal.add(Calendar.DAY_OF_YEAR, 1)
            TimeRange.WEEK -> newCal.add(Calendar.WEEK_OF_YEAR, 1)
            TimeRange.MONTH -> newCal.add(Calendar.MONTH, 1)
            TimeRange.YEAR -> newCal.add(Calendar.YEAR, 1)
            TimeRange.ALL -> { }
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
            TimeRange.ALL -> { }
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

    /**
     * Attempts to join a group using a manual URL paste.
     */
    fun joinGroupManual(url: String, onComplete: (String) -> Unit, onError: (String) -> Unit) {
        groupSyncManager.joinByUrl(url, onComplete, onError)
    }
}
