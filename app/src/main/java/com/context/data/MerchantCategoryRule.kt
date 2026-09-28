package com.context.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_category_rules")
data class MerchantCategoryRule(
    @PrimaryKey val merchantKey: String, // Normalized merchant name (lowercase, trimmed)
    val category: String,                // Category name
    val updatedAt: Long = System.currentTimeMillis()
)
