package com.context.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.context.data.ExpenseDao
import com.context.data.Group
import com.context.data.GroupMember
import com.context.sync.GroupSyncManager
import com.context.utils.OnboardingUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemberBalance(
    val name: String,
    val amount: Double,
    val upiId: String? = null,
    val phone: String? = null,
    val isSynced: Boolean = false
)

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val groupSyncManager: GroupSyncManager,
    @ApplicationContext private val context: Context,
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
        val uniqueMembers = group?.getMemberList()?.map { it.lowercase() }?.distinct() ?: emptyList()
        val memberCount = uniqueMembers.size
        if (memberCount > 0) total / memberCount else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val memberBalances: StateFlow<List<MemberBalance>> = combine(
        expenses, 
        group, 
        expenseDao.getMembersForGroup(groupId)
    ) { list, groupData, syncedMembers ->
        val rawMembers = groupData?.getMemberList() ?: return@combine emptyList()
        
        // Deduplicate members case-insensitively
        val memberMap = mutableMapOf<String, String>() // lowercase -> actual case
        rawMembers.forEach { name ->
            val lower = name.lowercase()
            if (!memberMap.containsKey(lower)) {
                memberMap[lower] = name
            }
        }
        
        val uniqueMembers = memberMap.keys.toList()
        val memberCount = uniqueMembers.size
        if (memberCount <= 0) return@combine emptyList()

        val myName = OnboardingUtils.getUserName(context)
        val myNameLower = myName.lowercase()
        
        val balances = mutableMapOf<String, Double>()
        uniqueMembers.forEach { balances[it] = 0.0 }

        list.forEach { expense ->
            if (expense.category == "Settlement") {
                val rawPayer = expense.merchant.removePrefix("Payment from ").trim().lowercase()
                val rawPaidBy = expense.paidBy.lowercase()
                
                val payer = if (rawPayer == "you") myNameLower else rawPayer
                val paidBy = if (rawPaidBy == "you") myNameLower else rawPaidBy
                
                if (balances.containsKey(payer)) {
                    balances[payer] = balances[payer]!! + expense.amount
                    balances[paidBy] = (balances[paidBy] ?: 0.0) - expense.amount
                }
            } else {
                val share = expense.amount / memberCount
                val rawPaidBy = expense.paidBy.lowercase()
                val paidBy = if (rawPaidBy == "you") myNameLower else rawPaidBy
                
                uniqueMembers.forEach { memberLower ->
                    if (memberLower == paidBy) {
                        balances[memberLower] = balances[memberLower]!! + (expense.amount - share)
                    } else {
                        balances[memberLower] = balances[memberLower]!! - share
                    }
                }
            }
        }

        uniqueMembers.map { lowerName ->
            val displayName = memberMap[lowerName]!!
            val syncedInfo = syncedMembers.find { it.name.equals(displayName, ignoreCase = true) }
            MemberBalance(
                name = displayName,
                amount = balances[lowerName] ?: 0.0,
                upiId = syncedInfo?.upiId,
                phone = syncedInfo?.phone,
                isSynced = syncedInfo != null
            )
        }.filter { it.name.lowercase() != "you" && it.name.lowercase() != myNameLower }
         .sortedByDescending { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val yourBalance: StateFlow<Double> = combine(expenses, group) { list, groupData ->
        val uniqueMembers = groupData?.getMemberList()?.map { it.lowercase() }?.distinct() ?: listOf("you")
        val memberCount = uniqueMembers.size
        if (memberCount <= 0) return@combine 0.0

        val myName = OnboardingUtils.getUserName(context).lowercase()
        var balance = 0.0
        list.forEach { expense ->
            val paidBy = expense.paidBy.lowercase()
            val isPaidByMe = paidBy == "you" || paidBy == myName
            
            if (expense.category == "Settlement") {
                if (isPaidByMe) {
                    balance -= expense.amount
                } else if (expense.merchant.contains("Payment from You", ignoreCase = true) || 
                           expense.merchant.contains("Payment from $myName", ignoreCase = true)) {
                    balance += expense.amount
                }
            } else {
                val share = expense.amount / memberCount
                if (isPaidByMe) {
                    balance += (expense.amount - share)
                } else {
                    balance -= share
                }
            }
        }
        balance
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    fun startSync(group: Group) {
        viewModelScope.launch {
            _isSyncing.value = true
            
            // Clean up case-sensitive duplicates in the member list string
            val members = group.getMemberList()
            val uniqueMembers = members.distinctBy { it.lowercase() }
            
            val activeGroup = if (uniqueMembers.size != members.size) {
                val updatedGroup = group.copy(members = uniqueMembers.joinToString(","))
                expenseDao.updateGroup(updatedGroup)
                updatedGroup
            } else {
                group
            }
            
            groupSyncManager.startSync(activeGroup)
            
            // Force push any unsynced expenses
            val unsynced = expenses.value.filter { !it.isSynced }
            unsynced.forEach { 
                groupSyncManager.pushExpense(it)
            }
            _isSyncing.value = false
        }
    }

    suspend fun deleteGroup() {
        val expensesInGroup = expenses.value
        expensesInGroup.forEach { expense ->
            expenseDao.update(expense.copy(groupId = null))
        }

        group.value?.let { groupToDelete ->
            groupSyncManager.stopSync(groupToDelete.remoteId)
            expenseDao.deleteGroup(groupToDelete)
        }
    }

    override fun onCleared() {
        group.value?.let { groupSyncManager.stopSync(it.remoteId) }
        super.onCleared()
    }
}
