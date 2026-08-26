package com.context.ui

import android.content.Context
import com.context.data.Category
import com.context.data.Expense
import com.context.data.ExpenseDao
import com.context.data.Group
import com.context.data.GroupMember
import com.context.data.RecurringExpense
import com.context.data.RecurringExpenseDao
import com.context.sync.GroupSyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A fake implementation of the ExpenseDao and RecurringExpenseDao for use in Previews.
 */
class FakeExpenseDao : ExpenseDao, RecurringExpenseDao {
    override fun getAllExpenses(): Flow<List<Expense>> = MutableStateFlow(emptyList())
    override suspend fun getExpenseById(id: Int): Expense? = null
    override suspend fun getExpenseByRemoteId(remoteId: String): Expense? = null
    override fun getAllGroups(): Flow<List<Group>> = MutableStateFlow(emptyList())
    override suspend fun getGroup(id: Int): Group? = null
    override suspend fun getGroupByRemoteId(remoteId: String): Group? = null
    override fun getExpensesForGroup(groupId: Int): Flow<List<Expense>> = MutableStateFlow(emptyList())
    override fun getTotalSpent(): Flow<Double?> = MutableStateFlow(0.0)
    override fun getGroupTotal(groupId: Int): Flow<Double?> = MutableStateFlow(0.0)
    
    override suspend fun insert(expense: Expense): Long = 0L
    override suspend fun insertAll(expenses: List<Expense>) {}
    override suspend fun insertGroup(group: Group): Long = 0L
    override suspend fun updateGroup(group: Group) {}
    override suspend fun delete(expense: Expense) {}
    override suspend fun deleteGroup(group: Group) {}
    override suspend fun update(expense: Expense) {}
    override suspend fun recalculateGroupTotal(groupId: Int) {}
    override suspend fun checkDuplicate(amount: Double, timeThreshold: Long): Int = 0
    override suspend fun checkDuplicateStrict(merchant: String, amount: Double, startTime: Long, endTime: Long): Int = 0
    override suspend fun getExpensesSince(startTime: Long): List<Expense> = emptyList()
    override suspend fun getTransactionCount(): Int = 0
    override suspend fun getCategoryTotalForPeriod(category: String, start: Long, end: Long): Double? = 0.0
    override suspend fun getUnsyncedExpenses(groupId: Int): List<Expense> = emptyList()
    
    // Group Member Methods
    override suspend fun insertMember(member: GroupMember) {}
    override suspend fun insertMembers(members: List<GroupMember>) {}
    override fun getMembersForGroup(groupId: Int): Flow<List<GroupMember>> = MutableStateFlow(emptyList())
    override suspend fun getMemberUpi(groupId: Int, name: String): String? = null

    // Reconciliation Methods
    override suspend fun deleteMember(groupId: Int, oldName: String) {}
    override suspend fun updateExpensePayer(groupId: Int, oldName: String, newName: String) {}
    override suspend fun renameMemberInGroup(groupId: Int, oldName: String, newName: String) {}
    override suspend fun deactivateMember(groupId: Int, name: String) {}
    override suspend fun reactivateMember(groupId: Int, name: String) {}

    // Category Methods
    override suspend fun insertCategory(category: Category) {}
    override fun getAllCategories(): Flow<List<Category>> = MutableStateFlow(emptyList())
    override suspend fun deleteCategory(category: Category) {}

    // Recurring Expense Methods
    override suspend fun insert(recurringExpense: RecurringExpense): Long = 0L
    override suspend fun update(recurringExpense: RecurringExpense) {}
    override suspend fun delete(recurringExpense: RecurringExpense) {}
    override fun getAllActiveRecurringExpenses(): Flow<List<RecurringExpense>> = MutableStateFlow(emptyList())
    override fun getAllTrackedRecurringExpenses(): Flow<List<RecurringExpense>> = MutableStateFlow(emptyList())
    override suspend fun getRecurringExpenseByMerchant(merchant: String): RecurringExpense? = null
    override suspend fun getAllRecurringExpensesSync(): List<RecurringExpense> = emptyList()
    override suspend fun suppressRecurringExpense(id: Int) {}
}

/**
 * A factory to create a HomeViewModel for use in Previews.
 */
object FakeHomeViewModelFactory {
    fun create(context: Context): HomeViewModel {
        val fakeDao = FakeExpenseDao()
        val dummySyncManager = GroupSyncManager(fakeDao, context)
        return HomeViewModel(fakeDao, fakeDao, dummySyncManager, context)
    }
}
