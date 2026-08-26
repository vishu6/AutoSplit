package com.context.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.context.data.Expense
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
import kotlin.math.abs

data class MemberBalance(
    val name: String,
    val amount: Double,
    val upiId: String? = null,
    val phone: String? = null,
    val isSynced: Boolean = false,
    val isActive: Boolean = true
)

sealed class GroupUiEvent {
    data class ShowSnackbar(val message: String) : GroupUiEvent()
    data class ShowErrorDialog(val title: String, val message: String) : GroupUiEvent()
}

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val groupSyncManager: GroupSyncManager,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val groupId = savedStateHandle.get<Int>("groupId") ?: 0

    private val _uiEvents = MutableSharedFlow<GroupUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    val expenses = expenseDao.getExpensesForGroup(groupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val group: StateFlow<Group?> = expenseDao.getAllGroups()
        .map { groups -> groups.find { it.groupId == groupId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val groupTotal: StateFlow<Double> = expenses
        .map { list -> list.filter { it.category != "Settlement" }.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
        
    val perHeadCost: StateFlow<Double> = combine(group, groupTotal) { group, total ->
        val members = group?.getMemberList()?.map { it.lowercase() }?.distinct() ?: emptyList()
        if (members.isNotEmpty()) total / members.size else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val allBalances: StateFlow<List<MemberBalance>> = combine(
        expenses, 
        group, 
        expenseDao.getMembersForGroup(groupId)
    ) { list, groupData, syncedMembers ->
        val rawMembers = groupData?.getMemberList() ?: return@combine emptyList()
        
        // NORMALIZATION: Collapse names that are identical except for case/whitespace
        val memberMap = mutableMapOf<String, String>() // lowercase -> original case
        rawMembers.forEach { name ->
            val lower = name.trim().lowercase()
            if (!memberMap.containsKey(lower)) {
                memberMap[lower] = name.trim()
            }
        }
        
        val uniqueMembers = memberMap.keys.toList()
        val myNameLower = OnboardingUtils.getUserName(context).trim().lowercase()
        
        val balances = calculateAllBalances(list, uniqueMembers, myNameLower)

        uniqueMembers.map { lowerName ->
            val displayName = memberMap[lowerName]!!
            // Check if this member has a synced identity in the database
            val syncedInfo = syncedMembers.find { it.name.trim().equals(displayName, ignoreCase = true) }
            
            MemberBalance(
                name = displayName,
                amount = balances[lowerName] ?: 0.0,
                upiId = syncedInfo?.upiId,
                phone = syncedInfo?.phone,
                isSynced = syncedInfo != null,
                isActive = syncedInfo?.isActive ?: true
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memberBalances: StateFlow<List<MemberBalance>> = allBalances.map { list ->
        val myNameLower = OnboardingUtils.getUserName(context).trim().lowercase()
        list.filter { it.name.lowercase() != "you" && it.name.lowercase() != myNameLower && it.isActive }
            .sortedByDescending { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inactiveMemberBalances: StateFlow<List<MemberBalance>> = allBalances.map { list ->
        list.filter { !it.isActive }.sortedBy { it.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val yourBalance: StateFlow<Double> = combine(expenses, group) { list, groupData ->
        val uniqueMembers = groupData?.getMemberList()?.map { it.trim().lowercase() }?.distinct() ?: listOf("you")
        val myName = OnboardingUtils.getUserName(context).trim().lowercase()
        val balances = calculateAllBalances(list, uniqueMembers, myName)
        balances[myName] ?: 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private fun calculateAllBalances(expenses: List<Expense>, membersLower: List<String>, myNameLower: String): Map<String, Double> {
        val memberCount = membersLower.size
        if (memberCount <= 0) return emptyMap()

        val balances = mutableMapOf<String, Double>()
        membersLower.forEach { balances[it] = 0.0 }

        expenses.forEach { expense ->
            if (expense.category == "Settlement") {
                val rawPayer = expense.merchant.removePrefix("Payment from ").trim().lowercase()
                val rawPaidBy = expense.paidBy.trim().lowercase()
                
                val payer = if (rawPayer == "you") myNameLower else rawPayer
                val paidBy = if (rawPaidBy == "you") myNameLower else rawPaidBy
                
                if (balances.containsKey(payer)) {
                    balances[payer] = (balances[payer] ?: 0.0) + expense.amount
                    balances[paidBy] = (balances[paidBy] ?: 0.0) - expense.amount
                }
            } else {
                val share = expense.amount / memberCount
                val rawPaidBy = expense.paidBy.trim().lowercase()
                val paidBy = if (rawPaidBy == "you") myNameLower else rawPaidBy
                
                membersLower.forEach { memberLower ->
                    if (memberLower == paidBy) {
                        balances[memberLower] = (balances[memberLower] ?: 0.0) + (expense.amount - share)
                    } else {
                        balances[memberLower] = (balances[memberLower] ?: 0.0) - share
                    }
                }
            }
        }
        return balances
    }

    fun tryDeleteMember(memberName: String) {
        viewModelScope.launch {
            val groupData = group.value ?: return@launch
            val expenseList = expenses.value
            val membersLower = groupData.getMemberList().map { it.trim().lowercase() }.distinct()
            val myNameLower = OnboardingUtils.getUserName(context).trim().lowercase()
            
            val balances = calculateAllBalances(expenseList, membersLower, myNameLower)
            val netBalance = balances[memberName.trim().lowercase()] ?: 0.0

            if (abs(netBalance) < 0.02) {
                expenseDao.deactivateMember(groupId, memberName.trim())
                _uiEvents.emit(GroupUiEvent.ShowSnackbar("$memberName removed successfully"))
                
                // Sync status to other members
                groupData.let { groupSyncManager.broadcastIdentity(it) }
            } else {
                val balanceText = if (netBalance > 0) {
                    "is owed ₹${String.format("%.2f", netBalance)}"
                } else {
                    "owes ₹${String.format("%.2f", abs(netBalance))}"
                }
                _uiEvents.emit(GroupUiEvent.ShowErrorDialog(
                    "Cannot remove member",
                    "$memberName still $balanceText. Please settle up before removing them."
                ))
            }
        }
    }

    fun reactivateMember(memberName: String) {
        viewModelScope.launch {
            expenseDao.reactivateMember(groupId, memberName.trim())
            _uiEvents.emit(GroupUiEvent.ShowSnackbar("$memberName re-added to group"))
            
            // Sync status to other members
            group.value?.let { groupSyncManager.broadcastIdentity(it) }
        }
    }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    fun startSync(group: Group) {
        viewModelScope.launch {
            _isSyncing.value = true
            val members = group.getMemberList()
            val uniqueMembers = members.map { it.trim() }.distinctBy { it.lowercase() }
            
            val activeGroup = if (uniqueMembers.size != members.size) {
                val updatedGroup = group.copy(members = uniqueMembers.joinToString(","))
                expenseDao.updateGroup(updatedGroup)
                updatedGroup
            } else {
                group
            }
            
            groupSyncManager.startSync(activeGroup)
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
