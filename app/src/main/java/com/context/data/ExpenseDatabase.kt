package com.context.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val merchant: String,
    val amount: Double,
    val timestamp: Long,
    val category: String = "General",
    val groupId: Int? = null,
    val paidBy: String = "You",
    val isAuto: Boolean = false 
)

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey(autoGenerate = true) val groupId: Int = 0,
    val name: String,
    val members: String = "You",
    val totalSpent: Double? = 0.0
) {
    // Helper to get list easily in code
    fun getMemberList(): List<String> {
        return members.split(",").filter { it.isNotBlank() }
    }
}

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: Expense)

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Int): Expense?

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>
    
    // CORRECTED: Ignore Settlements from total
    @Query("SELECT SUM(amount) FROM expenses WHERE category != 'Settlement'")
    fun getTotalSpent(): Flow<Double?>

    @Query("SELECT * FROM `groups`")
    fun getAllGroups(): Flow<List<Group>>

    @Query("SELECT * FROM `groups` WHERE groupId = :id")
    suspend fun getGroup(id: Int): Group?

    @Insert
    suspend fun insertGroup(group: Group): Long

    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getExpensesForGroup(groupId: Int): Flow<List<Expense>>
    
    // This query is no longer used directly by the UI, but let's keep it for potential future use
    @Query("SELECT SUM(amount) FROM expenses WHERE groupId = :groupId")
    fun getGroupTotal(groupId: Int): Flow<Double?>

    // CORRECTED: Ignore Settlements when updating the group's total
    @Query("UPDATE `groups` SET totalSpent = (SELECT SUM(amount) FROM expenses WHERE groupId = :groupId AND category != 'Settlement') WHERE groupId = :groupId")
    suspend fun recalculateGroupTotal(groupId: Int)
}


@Database(entities = [Expense::class, Group::class], version = 6)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile private var INSTANCE: ExpenseDatabase? = null

        fun getDatabase(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}