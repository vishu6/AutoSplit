package com.context.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "monthly_budgets")
data class MonthlyBudget(
    @PrimaryKey val monthKey: String, // format: "YYYY-MM"
    val totalLimit: Double
)

@Entity(tableName = "monthly_category_limits", primaryKeys = ["monthKey", "category"])
data class MonthlyCategoryLimit(
    val monthKey: String,
    val category: String,
    val limit: Double
)

@Dao
interface BudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE monthKey = :monthKey")
    suspend fun getMonthlyBudget(monthKey: String): MonthlyBudget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyBudget(budget: MonthlyBudget)

    @Query("SELECT * FROM monthly_category_limits WHERE monthKey = :monthKey")
    suspend fun getCategoryLimitsForMonth(monthKey: String): List<MonthlyCategoryLimit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryLimits(limits: List<MonthlyCategoryLimit>)

    @Query("DELETE FROM monthly_category_limits WHERE monthKey = :monthKey")
    suspend fun deleteCategoryLimitsForMonth(monthKey: String)
}
