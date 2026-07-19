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
class EditExpenseViewModel @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val groupSyncManager: GroupSyncManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val allGroups = expenseDao.getAllGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = expenseDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateExpense(expense: Expense, oldGroupId: Int?) {
        viewModelScope.launch {
            expenseDao.update(expense)
            
            // Trigger instant widget update
            WidgetUpdateHelper.updateWidget(context)
            
            // Recalculate totals for both old and new groups if they changed
            expense.groupId?.let { expenseDao.recalculateGroupTotal(it) }
            if (oldGroupId != null && oldGroupId != expense.groupId) {
                expenseDao.recalculateGroupTotal(oldGroupId)
            }

            // Sync with Cloud
            if (expense.groupId != null) {
                groupSyncManager.pushExpense(expense)
            } else if (oldGroupId != null) {
                // If it was moved from a group to personal, delete from remote
                groupSyncManager.deleteRemoteExpense(expense)
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            val groupId = expense.groupId
            expenseDao.delete(expense)
            
            // Trigger instant widget update
            WidgetUpdateHelper.updateWidget(context)
            
            groupId?.let { 
                expenseDao.recalculateGroupTotal(it)
                // Remove from cloud so it disappears for everyone
                groupSyncManager.deleteRemoteExpense(expense)
            }
        }
    }
}
