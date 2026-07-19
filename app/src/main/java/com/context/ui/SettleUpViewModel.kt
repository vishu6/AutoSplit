package com.context.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.context.data.Expense
import com.context.data.ExpenseDao
import com.context.data.Group
import com.context.sync.GroupSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

data class SettlementSuggestion(
    val from: String,
    val to: String,
    val amount: Double
)

@HiltViewModel
class SettleUpViewModel @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val groupSyncManager: GroupSyncManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val groupId = savedStateHandle.get<Int>("groupId") ?: 0

    val group: StateFlow<Group?> = expenseDao.getAllGroups()
        .map { groups -> groups.find { it.groupId == groupId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val expenses = expenseDao.getExpensesForGroup(groupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Smart Settlement Engine: Minimizes transactions using a Greedy matching algorithm
     */
    val settlementSuggestions: StateFlow<List<SettlementSuggestion>> = combine(expenses, group) { list, groupData ->
        val members = groupData?.getMemberList() ?: return@combine emptyList()
        val memberCount = members.size
        if (memberCount <= 1) return@combine emptyList()

        // 1. Calculate net balance for each member
        val netBalances = mutableMapOf<String, Double>()
        members.forEach { netBalances[it] = 0.0 }

        list.forEach { expense ->
            if (expense.category == "Settlement") {
                // Handle existing settlement records
                val payer = expense.merchant.removePrefix("Payment from ").trim()
                if (netBalances.containsKey(payer)) {
                    netBalances[payer] = netBalances[payer]!! + expense.amount
                    netBalances[expense.paidBy] = (netBalances[expense.paidBy] ?: 0.0) - expense.amount
                }
            } else {
                val share = expense.amount / memberCount
                val paidBy = expense.paidBy
                
                members.forEach { member ->
                    if (member == paidBy) {
                        netBalances[member] = netBalances[member]!! + (expense.amount - share)
                    } else {
                        netBalances[member] = netBalances[member]!! - share
                    }
                }
            }
        }

        // 2. Separate into Debtor and Creditor lists
        val debtors = mutableListOf<Pair<String, Double>>() // People who owe
        val creditors = mutableListOf<Pair<String, Double>>() // People who are owed

        netBalances.forEach { (name, balance) ->
            if (balance < -0.01) debtors.add(name to abs(balance))
            else if (balance > 0.01) creditors.add(name to balance)
        }

        // 3. Greedy Matching to minimize transactions
        val suggestions = mutableListOf<SettlementSuggestion>()
        var d = 0
        var c = 0
        
        val dList = debtors.toMutableList()
        val cList = creditors.toMutableList()

        while (d < dList.size && c < cList.size) {
            val debtor = dList[d]
            val creditor = cList[c]
            val settleAmount = minOf(debtor.second, creditor.second)

            suggestions.add(SettlementSuggestion(debtor.first, creditor.first, settleAmount))

            dList[d] = debtor.first to (debtor.second - settleAmount)
            cList[c] = creditor.first to (creditor.second - settleAmount)

            if (dList[d].second < 0.01) d++
            if (cList[c].second < 0.01) c++
        }

        suggestions
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun recordSettlement(from: String, to: String, amount: Double) {
        viewModelScope.launch {
            val settlement = Expense(
                merchant = "Payment from $from",
                amount = amount,
                timestamp = System.currentTimeMillis(),
                category = "Settlement",
                groupId = groupId,
                paidBy = to, // Recorded as paid to the receiver
                isAuto = false
            )
            expenseDao.insert(settlement)
            expenseDao.recalculateGroupTotal(groupId)
            
            // Multiplayer Sync
            groupSyncManager.pushExpense(settlement)
        }
    }
}
