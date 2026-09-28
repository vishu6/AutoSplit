package com.context.ui

import android.content.Context
import androidx.sqlite.db.SupportSQLiteQuery
import com.context.data.*
import com.context.sync.GroupSyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A fake implementation of the ExpenseDao, RecurringExpenseDao, and BudgetDao for use in Previews.
 */
class FakeExpenseDao : ExpenseDao, RecurringExpenseDao, BudgetDao {
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
    override suspend fun update(expense: Expense) {}
    override suspend fun delete(expense: Expense) {}
    
    override suspend fun insertGroup(group: Group): Long = 0L
    override suspend fun updateGroup(group: Group) {}
    override suspend fun deleteGroup(group: Group) {}

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

    // Merchant Custom Rules Methods
    override suspend fun insertMerchantRule(rule: MerchantCategoryRule) {}
    override suspend fun getMerchantRule(merchantKey: String): MerchantCategoryRule? = null

    // Raw Query Methods (For Ask Cleave AI) - Using fully qualified parameter type to resolve compiler errors
    override suspend fun executeRawQuery(query: SupportSQLiteQuery): List<Long> = emptyList()
    override suspend fun executeRawQueryForDouble(query: SupportSQLiteQuery): Double? = 0.0
    override suspend fun executeRawQueryForExpenses(query: SupportSQLiteQuery): List<Expense> = emptyList()

    // Recurring Expense Methods
    override suspend fun insert(recurringExpense: RecurringExpense): Long = 0L
    override suspend fun update(recurringExpense: RecurringExpense) {}
    override suspend fun delete(recurringExpense: RecurringExpense) {}
    override fun getAllActiveRecurringExpenses(): Flow<List<RecurringExpense>> = MutableStateFlow(emptyList())
    override fun getAllTrackedRecurringExpenses(): Flow<List<RecurringExpense>> = MutableStateFlow(emptyList())
    override suspend fun getRecurringExpenseByMerchant(merchant: String): RecurringExpense? = null
    override suspend fun getAllRecurringExpensesSync(): List<RecurringExpense> = emptyList()
    override suspend fun suppressRecurringExpense(id: Int) {}

    // BudgetDao Methods
    override suspend fun getMonthlyBudget(monthKey: String): MonthlyBudget? = null
    override suspend fun insertMonthlyBudget(budget: MonthlyBudget) {}
    override suspend fun getCategoryLimitsForMonth(monthKey: String): List<MonthlyCategoryLimit> = emptyList()
    override suspend fun insertCategoryLimits(limits: List<MonthlyCategoryLimit>) {}
    override suspend fun deleteCategoryLimitsForMonth(monthKey: String) {}
}

/**
 * A factory to create a HomeViewModel for use in Previews.
 */
object FakeHomeViewModelFactory {
    fun create(context: Context): HomeViewModel {
        val fakeDao = FakeExpenseDao()
        val dummySyncManager = GroupSyncManager(fakeDao, context)
        val askCleaveRepository = AskCleaveRepository(fakeDao)
        val budgetRepository = BudgetRepository(fakeDao, context)
        return HomeViewModel(
            expenseDao = fakeDao, 
            recurringExpenseDao = fakeDao, 
            askCleaveRepository = askCleaveRepository,
            budgetRepository = budgetRepository,
            groupSyncManager = dummySyncManager,
            context = context
        )
    }
}
