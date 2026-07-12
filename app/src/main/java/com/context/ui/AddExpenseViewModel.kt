package com.context.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.context.data.Category
import com.context.data.Expense
import com.context.data.ExpenseDao
import com.context.sync.GroupSyncManager
import com.context.utils.WidgetUpdateHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val groupSyncManager: GroupSyncManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val allGroups = expenseDao.getAllGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = expenseDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveExpense(expense: Expense) {
        viewModelScope.launch {
            val id = expenseDao.insert(expense).toInt()
            val savedExpense = expense.copy(id = id)
            
            // Trigger instant widget update
            WidgetUpdateHelper.updateWidget(context)
            
            // After inserting, if it's a group expense, we must recalculate and sync
            savedExpense.groupId?.let { groupId ->
                expenseDao.recalculateGroupTotal(groupId)
                // Auto-push to cloud if sync is enabled
                groupSyncManager.pushExpense(savedExpense)
            }
        }
    }
}
