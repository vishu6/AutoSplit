package com.context.ui

import androidx.lifecycle.ViewModel
import com.context.data.Expense
import com.context.data.ExpenseDao
import com.context.data.Group
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A fake implementation of the ExpenseDao for use in Previews.
 */
class FakeExpenseDao : ExpenseDao {
    override fun getAllExpenses(): Flow<List<Expense>> = MutableStateFlow(emptyList())
    override suspend fun getExpenseById(id: Int): Expense? = null
    override fun getAllGroups(): Flow<List<Group>> = MutableStateFlow(emptyList())
    override suspend fun getGroup(id: Int): Group? = null
    override fun getExpensesForGroup(groupId: Int): Flow<List<Expense>> = MutableStateFlow(emptyList())
    override fun getTotalSpent(): Flow<Double?> = MutableStateFlow(0.0)
    override fun getGroupTotal(groupId: Int): Flow<Double?> = MutableStateFlow(0.0)
    override suspend fun insert(expense: Expense) {}
    override suspend fun insertAll(expenses: List<Expense>) {}
    override suspend fun insertGroup(group: Group): Long = 0L
    override suspend fun delete(expense: Expense) {}
    override suspend fun deleteGroup(group: Group) {}
    override suspend fun update(expense: Expense) {}
    override suspend fun recalculateGroupTotal(groupId: Int) {}
    override suspend fun checkDuplicate(amount: Double, timeThreshold: Long): Int = 0
    override suspend fun checkDuplicateStrict(merchant: String, amount: Double, startTime: Long, endTime: Long): Int = 0
}

/**
 * A factory to create a HomeViewModel for use in Previews.
 */
object FakeHomeViewModelFactory {
    fun create(): HomeViewModel {
        return HomeViewModel(FakeExpenseDao())
    }
}
