package com.context.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.context.data.Category
import com.context.data.Expense
import com.context.data.ExpenseDao
import com.context.data.RecurringExpense
import com.context.data.RecurringExpenseDao
import com.context.sync.GroupSyncManager
import com.context.utils.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val recurringExpenseDao: RecurringExpenseDao,
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
        
    val activeRecurringExpenses = recurringExpenseDao.getAllTrackedRecurringExpenses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Dynamic Category Map for icon resolution
    val categoryMap = expenseDao.getAllCategories()
        .map { list -> list.associateBy { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val allCategoriesList = expenseDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTimeRange = MutableStateFlow(TimeRange.MONTH)
    val selectedTimeRange = _selectedTimeRange.asStateFlow()
    
    private val _currentCalendar = MutableStateFlow(Calendar.getInstance())
    val currentCalendar = _currentCalendar.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val customDateRange = _customDateRange.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive = _isSearchActive.asStateFlow()

    // Multiple category selection state
    private val _selectedCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategories = _selectedCategories.asStateFlow()

    private val _selectedSortOrder = MutableStateFlow(SortOrder.NEWEST)
    val selectedSortOrder = _selectedSortOrder.asStateFlow()

    // Category selection state (for charts)
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    // Category Limits state
    private val _categoryLimits = MutableStateFlow<Map<String, Double>>(emptyMap())
    val categoryLimits = _categoryLimits.asStateFlow()

    init {
        refreshBudgetLimits()
        seedCategoriesIfEmpty()
    }

    private fun seedCategoriesIfEmpty() {
        viewModelScope.launch {
            val currentCategories = expenseDao.getAllCategories().first()
            if (currentCategories.isEmpty()) {
                val defaults = ExpenseCategory.entries.map { 
                    Category(
                        name = it.label,
                        iconName = it.name,
                        colorHex = "#2962FF", 
                        isSystem = true
                    )
                }
                defaults.forEach { expenseDao.insertCategory(it) }
            }
        }
    }

    fun refreshBudgetLimits() {
        _categoryLimits.value = BudgetUtils.getCategoryLimits(context)
    }

    @Suppress("UNCHECKED_CAST")
    val filteredExpenses = combine(
        allExpenses, 
        selectedTimeRange, 
        currentCalendar, 
        customDateRange,
        searchQuery, 
        isSearchActive,
        selectedCategories,
        selectedSortOrder
    ) { args: Array<Any?> ->
        val expenses = args[0] as List<Expense>
        val range = args[1] as TimeRange
        val calendar = args[2] as Calendar
        val customRange = args[3] as Pair<Long, Long>?
        val query = args[4] as String
        val searchActive = args[5] as Boolean
        val selectedCats = args[6] as Set<String>
        val sortOrder = args[7] as SortOrder
        
        val (start, end) = if (range == TimeRange.CUSTOM && customRange != null) {
            customRange
        } else {
            DateFilterUtils.getTimeRange(range, calendar)
        }
        
        val filtered = expenses.filter { expense ->
            val matchesTime = if (range == TimeRange.ALL || searchActive) true else expense.timestamp in start..end
            val matchesSearch = if (searchActive && query.isNotEmpty()) {
                expense.merchant.contains(query, ignoreCase = true) || 
                expense.amount.toString().contains(query) ||
                expense.category.contains(query, ignoreCase = true)
            } else true
            
            val matchesCategory = selectedCats.isEmpty() || selectedCats.contains(expense.category)
            
            matchesTime && matchesSearch && matchesCategory
        }

        when (sortOrder) {
            SortOrder.NEWEST -> filtered.sortedByDescending { it.timestamp }
            SortOrder.OLDEST -> filtered.sortedBy { it.timestamp }
            SortOrder.HIGHEST_AMOUNT -> filtered.sortedByDescending { it.amount }
            SortOrder.LOWEST_AMOUNT -> filtered.sortedBy { it.amount }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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

    val currentMonthCategorySpent = allExpenses.map { expenses ->
        val (start, end) = DateFilterUtils.getTimeRange(TimeRange.MONTH, Calendar.getInstance())
        expenses
            .filter { it.timestamp in start..end && it.category != "Settlement" }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
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

    fun onTimeRangeSelected(range: TimeRange) {
        _selectedTimeRange.value = range
        if (range != TimeRange.CUSTOM) {
            _currentCalendar.value = Calendar.getInstance()
            _customDateRange.value = null
        }
        _selectedCategory.value = null
    }

    fun onCustomDateRangeSelected(start: Long, end: Long) {
        _selectedTimeRange.value = TimeRange.CUSTOM
        _customDateRange.value = Pair(start, end)
    }

    fun onNextPeriod() {
        val newCal = _currentCalendar.value.clone() as Calendar
        when (selectedTimeRange.value) {
            TimeRange.TODAY -> newCal.add(Calendar.DAY_OF_YEAR, 1)
            TimeRange.WEEK -> newCal.add(Calendar.WEEK_OF_YEAR, 1)
            TimeRange.MONTH -> newCal.add(Calendar.MONTH, 1)
            TimeRange.YEAR -> newCal.add(Calendar.YEAR, 1)
            else -> { }
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
            else -> { }
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

    fun toggleCategorySelection(category: String) {
        val current = _selectedCategories.value.toMutableSet()
        if (current.contains(category)) {
            current.remove(category)
        } else {
            current.add(category)
        }
        _selectedCategories.value = current
    }

    fun clearCategoryFilters() {
        _selectedCategories.value = emptySet()
    }

    fun onSortOrderSelected(order: SortOrder) {
        _selectedSortOrder.value = order
    }

    fun resetAllFilters() {
        _selectedTimeRange.value = TimeRange.MONTH
        _currentCalendar.value = Calendar.getInstance()
        _customDateRange.value = null
        _selectedCategories.value = emptySet()
        _selectedSortOrder.value = SortOrder.NEWEST
        _searchQuery.value = ""
        _isSearchActive.value = false
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun joinGroupManual(url: String, onComplete: (String) -> Unit, onError: (String) -> Unit) {
        groupSyncManager.joinByUrl(url, onComplete, onError)
    }

    fun markAsRecurring(expenseId: Int) {
        viewModelScope.launch {
            val expense = expenseDao.getExpenseById(expenseId) ?: return@launch
            val recurring = RecurringExpense(
                merchant = expense.merchant,
                averageAmount = expense.amount,
                frequencyDays = 30, // Default to monthly
                lastPaidDate = expense.timestamp,
                nextExpectedDate = expense.timestamp + (30L * 24 * 60 * 60 * 1000),
                isAutoDetected = false,
                confidenceScore = 1.0f,
                category = expense.category
            )
            recurringExpenseDao.insert(recurring)
        }
    }

    fun markPaidManually(id: Int) {
        viewModelScope.launch {
            val recurring = recurringExpenseDao.getAllRecurringExpensesSync().find { it.id == id } ?: return@launch
            val now = System.currentTimeMillis()
            val nextDate = now + (recurring.frequencyDays.toLong() * 24 * 60 * 60 * 1000)
            recurringExpenseDao.update(recurring.copy(
                lastPaidDate = now,
                nextExpectedDate = nextDate,
                isActive = true
            ))
        }
    }

    fun acceptSuggestion(id: Int) {
        viewModelScope.launch {
            val recurring = recurringExpenseDao.getAllRecurringExpensesSync().find { it.id == id } ?: return@launch
            recurringExpenseDao.update(recurring.copy(isActive = true))
        }
    }

    fun suppressRecurring(id: Int) {
        viewModelScope.launch {
            recurringExpenseDao.suppressRecurringExpense(id)
        }
    }
}
