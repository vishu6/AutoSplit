package com.context.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.context.data.Expense
import com.context.data.ExpenseDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val expenseDao: ExpenseDao
) : ViewModel() {

    val allGroups = expenseDao.getAllGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.insert(expense)
            // After inserting, if it's a group expense, we must recalculate the total
            expense.groupId?.let {
                expenseDao.recalculateGroupTotal(it)
            }
        }
    }
}