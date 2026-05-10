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

data class MemberBalance(
    val name: String,
    val amount: Double
)

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

    val memberBalances: StateFlow<List<MemberBalance>> = combine(expenses, group) { list, groupData ->
        val members = groupData?.getMemberList() ?: return@combine emptyList()
        val memberCount = members.size
        if (memberCount <= 0) return@combine emptyList()

        val balances = mutableMapOf<String, Double>()
        members.forEach { balances[it] = 0.0 }

        list.forEach { expense ->
            if (expense.category == "Settlement") {
                // Settlement means someone paid back. 
                // Currently SettleUp records payment FROM [PayerName] TO You.
                // merchant is "Payment from [PayerName]"
                // But in logic we need to know who paid whom.
                // Assuming it's recorded as "Payment from X" (to "You")
                val payer = expense.merchant.removePrefix("Payment from ").trim()
                if (balances.containsKey(payer)) {
                    balances[payer] = balances[payer]!! + expense.amount
                    balances["You"] = (balances["You"] ?: 0.0) - expense.amount
                }
            } else {
                val share = expense.amount / memberCount
                val paidBy = expense.paidBy
                
                members.forEach { member ->
                    if (member == paidBy) {
                        balances[member] = balances[member]!! + (expense.amount - share)
                    } else {
                        balances[member] = balances[member]!! - share
                    }
                }
            }
        }

        members.map { MemberBalance(it, balances[it] ?: 0.0) }
            .filter { it.name != "You" } // We only show others in the breakdown
            .sortedByDescending { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Your Balance Calculation:
    val yourBalance: StateFlow<Double> = combine(expenses, group) { list, groupData ->
        val memberCount = groupData?.getMemberList()?.size ?: 1
        if (memberCount <= 0) return@combine 0.0

        var balance = 0.0
        list.forEach { expense ->
            if (expense.category == "Settlement") {
                balance -= expense.amount
            } else {
                val share = expense.amount / memberCount
                if (expense.paidBy == "You") {
                    balance += (expense.amount - share)
                } else {
                    balance -= share
                }
            }
        }
        balance
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun deleteGroup() {
        viewModelScope.launch {
            val expensesInGroup = expenses.first()
            expensesInGroup.forEach { expense ->
                expenseDao.update(expense.copy(groupId = null))
            }

            group.value?.let { groupToDelete ->
                expenseDao.deleteGroup(groupToDelete)
            }
        }
    }
}