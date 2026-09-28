package com.context.data

import android.content.Context
import com.context.utils.BudgetUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    @ApplicationContext private val context: Context
) {
    /**
     * The Waterfall with Auto-Inheritance: Specific Month Override -> Previous Month Custom Budget -> Global SharedPref Default
     */
    suspend fun getMonthlyBudget(monthKey: String): Double = withContext(Dispatchers.IO) {
        val override = budgetDao.getMonthlyBudget(monthKey)
        if (override != null) {
            override.totalLimit
        } else {
            // Industry Standard: Auto-inherit from previous month's custom budget if it exists
            val prevMonthKey = getPreviousMonthKey(monthKey)
            val prevOverride = budgetDao.getMonthlyBudget(prevMonthKey)
            if (prevOverride != null) {
                budgetDao.insertMonthlyBudget(MonthlyBudget(monthKey, prevOverride.totalLimit))
                prevOverride.totalLimit
            } else {
                BudgetUtils.getMonthlyBudget(context)
            }
        }
    }

    suspend fun getCategoryLimits(monthKey: String): Map<String, Double> = withContext(Dispatchers.IO) {
        val overrides = budgetDao.getCategoryLimitsForMonth(monthKey)
        if (overrides.isNotEmpty()) {
            overrides.associate { it.category to it.limit }
        } else {
            // Industry Standard: Auto-inherit from previous month's custom category limits if they exist
            val prevMonthKey = getPreviousMonthKey(monthKey)
            val prevOverrides = budgetDao.getCategoryLimitsForMonth(prevMonthKey)
            if (prevOverrides.isNotEmpty()) {
                val newLimits = prevOverrides.map { it.copy(monthKey = monthKey) }
                budgetDao.insertCategoryLimits(newLimits)
                prevOverrides.associate { it.category to it.limit }
            } else {
                BudgetUtils.getCategoryLimits(context)
            }
        }
    }

    suspend fun setMonthlyBudget(monthKey: String, amount: Double) = withContext(Dispatchers.IO) {
        budgetDao.insertMonthlyBudget(MonthlyBudget(monthKey, amount))
    }

    suspend fun setCategoryLimits(monthKey: String, limits: Map<String, Double>) = withContext(Dispatchers.IO) {
        // Industry Standard: Clear out previous targets first to avoid "ghost limits"
        budgetDao.deleteCategoryLimitsForMonth(monthKey)
        val entities = limits.map { (cat, limit) -> 
            MonthlyCategoryLimit(monthKey, cat, limit)
        }
        budgetDao.insertCategoryLimits(entities)
    }

    /**
     * Copies from Global Default if no Room record exists for the source month.
     */
    suspend fun copyBudgetFromMonth(fromMonth: String, toMonth: String) = withContext(Dispatchers.IO) {
        val existingSourceBudget = budgetDao.getMonthlyBudget(fromMonth)
        val sourceTotal = existingSourceBudget?.totalLimit ?: BudgetUtils.getMonthlyBudget(context)
        
        // Save to target month in Room
        budgetDao.insertMonthlyBudget(MonthlyBudget(toMonth, sourceTotal))

        val existingCatLimits = budgetDao.getCategoryLimitsForMonth(fromMonth)
        if (existingCatLimits.isNotEmpty()) {
            val newLimits = existingCatLimits.map { it.copy(monthKey = toMonth) }
            budgetDao.insertCategoryLimits(newLimits)
        } else {
            // Fallback to global category limits if source month has no overrides
            val globalLimits = BudgetUtils.getCategoryLimits(context)
            if (globalLimits.isNotEmpty()) {
                val newLimits = globalLimits.map { (cat, limit) -> 
                    MonthlyCategoryLimit(toMonth, cat, limit)
                }
                budgetDao.insertCategoryLimits(newLimits)
            }
        }
    }

    /**
     * Checks if a custom override exists for this month
     */
    suspend fun hasCustomBudget(monthKey: String): Boolean = withContext(Dispatchers.IO) {
        budgetDao.getMonthlyBudget(monthKey) != null
    }

    /**
     * Helper to compute the previous month key given "YYYY-MM"
     */
    private fun getPreviousMonthKey(monthKey: String): String {
        return try {
            val parts = monthKey.split("-")
            var year = parts[0].toInt()
            var month = parts[1].toInt()
            month -= 1
            if (month == 0) {
                month = 12
                year -= 1
            }
            String.format(Locale.US, "%04d-%02d", year, month)
        } catch (e: Exception) {
            ""
        }
    }
}
