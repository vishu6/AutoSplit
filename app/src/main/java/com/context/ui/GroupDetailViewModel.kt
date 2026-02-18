package com.context.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.context.data.ExpenseDao
import com.context.data.Group
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val expenseDao: ExpenseDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val groupId = savedStateHandle.get<Int>("groupId") ?: 0

    val expenses = expenseDao.getExpensesForGroup(groupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val group: StateFlow<Group?> = expenseDao.getAllGroups()
        .map { groups -> groups.find { it.groupId == groupId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val groupTotal: StateFlow<Double> = expenses
        .map { list -> list.filter { it.category != "Settlement" }.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
        
    val perHeadCost: StateFlow<Double> = combine(group, groupTotal) { group, total ->
        val memberCount = group?.getMemberList()?.size ?: 1
        if (memberCount > 0) total / memberCount else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun deleteGroup() {
        viewModelScope.launch {
            // First, update all expenses in this group to be personal expenses
            val expensesInGroup = expenses.first()
            expensesInGroup.forEach { expense ->
                expenseDao.update(expense.copy(groupId = null))
            }

            // Now, delete the group itself
            group.value?.let { groupToDelete ->
                expenseDao.deleteGroup(groupToDelete)
            }
        }
    }
}