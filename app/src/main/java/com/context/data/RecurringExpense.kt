package com.context.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_expenses")
data class RecurringExpense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val merchant: String,
    val averageAmount: Double,
    val frequencyDays: Int,      // 7, 30, 90, 365
    val lastPaidDate: Long,
    val nextExpectedDate: Long,
    val isAutoDetected: Boolean, // true = algo, false = manual
    val confidenceScore: Float,  // 0.0 to 1.0
    val isActive: Boolean = true,
    val category: String = "General",
    val isSuppressed: Boolean = false // If user said "No" to suggestion
)
