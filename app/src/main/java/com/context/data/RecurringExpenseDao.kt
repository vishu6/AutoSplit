package com.context.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurringExpense: RecurringExpense): Long

    @Update
    suspend fun update(recurringExpense: RecurringExpense)

    @Delete
    suspend fun delete(recurringExpense: RecurringExpense)

    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1 AND isSuppressed = 0")
    fun getAllActiveRecurringExpenses(): Flow<List<RecurringExpense>>

    @Query("SELECT * FROM recurring_expenses WHERE isSuppressed = 0")
    fun getAllTrackedRecurringExpenses(): Flow<List<RecurringExpense>>

    @Query("SELECT * FROM recurring_expenses WHERE merchant = :merchant LIMIT 1")
    suspend fun getRecurringExpenseByMerchant(merchant: String): RecurringExpense?

    @Query("SELECT * FROM recurring_expenses")
    suspend fun getAllRecurringExpensesSync(): List<RecurringExpense>

    @Query("UPDATE recurring_expenses SET isSuppressed = 1 WHERE id = :id")
    suspend fun suppressRecurringExpense(id: Int)
}
