package com.context.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.context.data.BudgetRepository
import com.context.utils.BudgetUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _selectedCalendar = MutableStateFlow(Calendar.getInstance())
    val selectedCalendar = _selectedCalendar.asStateFlow()

    private val _monthlyBudgetValue = MutableStateFlow("")
    val monthlyBudgetValue = _monthlyBudgetValue.asStateFlow()

    private val _categoryLimits = MutableStateFlow<Map<String, String>>(emptyMap())
    val categoryLimits = _categoryLimits.asStateFlow()

    private val _isCustom = MutableStateFlow(false)
    val isCustom = _isCustom.asStateFlow()

    init {
        loadBudgetForCurrentSelection()
    }

    fun onMonthChanged(isNext: Boolean) {
        val newCal = _selectedCalendar.value.clone() as Calendar
        newCal.add(Calendar.MONTH, if (isNext) 1 else -1)
        _selectedCalendar.value = newCal
        loadBudgetForCurrentSelection()
    }

    fun onTotalBudgetChange(value: String) {
        if (value.all { it.isDigit() }) {
            _monthlyBudgetValue.value = value
        }
    }

    fun onCategoryLimitChange(category: String, value: String) {
        val current = _categoryLimits.value.toMutableMap()
        if (value.all { it.isDigit() }) {
            current[category] = value
            _categoryLimits.value = current
        }
    }

    private fun loadBudgetForCurrentSelection() {
        viewModelScope.launch {
            val monthKey = getMonthKey(_selectedCalendar.value)
            val budget = budgetRepository.getMonthlyBudget(monthKey)
            val limits = budgetRepository.getCategoryLimits(monthKey)
            
            _monthlyBudgetValue.value = if (budget > 0) budget.toInt().toString() else ""
            _categoryLimits.value = limits.mapValues { it.value.toInt().toString() }
            _isCustom.value = budgetRepository.hasCustomBudget(monthKey)
        }
    }

    fun saveForMonthOnly(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val monthKey = getMonthKey(_selectedCalendar.value)
            val total = _monthlyBudgetValue.value.toDoubleOrNull() ?: 0.0
            val limits = _categoryLimits.value.mapValues { it.value.toDoubleOrNull() ?: 0.0 }
            
            budgetRepository.setMonthlyBudget(monthKey, total)
            budgetRepository.setCategoryLimits(monthKey, limits)
            onSuccess()
        }
    }

    fun saveAsGlobalDefault(onContext: android.content.Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val total = _monthlyBudgetValue.value.toDoubleOrNull() ?: 0.0
            val limits = _categoryLimits.value.mapValues { it.value.toDoubleOrNull() ?: 0.0 }
            
            BudgetUtils.setMonthlyBudget(onContext, total)
            BudgetUtils.setCategoryLimits(onContext, limits)
            onSuccess()
        }
    }

    private fun getMonthKey(cal: Calendar): String {
        return String.format(Locale.US, "%d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }
}
