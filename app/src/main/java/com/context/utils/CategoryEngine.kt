package com.context.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.vector.ImageVector

enum class ExpenseCategory(val label: String, val icon: ImageVector) {
    FOOD("Food & Drink", Icons.Default.Dining),
    TRANSPORT("Transport", Icons.Default.Commute),
    GROCERY("Groceries", Icons.Default.LocalGroceryStore),
    SHOPPING("Shopping", Icons.Default.ShoppingBag),
    BILLS("Bills & Utilities", Icons.Default.Receipt),
    GENERAL("General", Icons.Default.Receipt)
}

object CategoryEngine {

    // The "Brain" - Map keywords to Categories
    private val KEYWORD_MAP = mapOf(
        // Food
        "food" to ExpenseCategory.FOOD,
        "swiggy" to ExpenseCategory.FOOD,
        "zomato" to ExpenseCategory.FOOD,
        "dominos" to ExpenseCategory.FOOD,
        "starbucks" to ExpenseCategory.FOOD,
        "mcdonalds" to ExpenseCategory.FOOD,
        "restaurant" to ExpenseCategory.FOOD,
        "cafe" to ExpenseCategory.FOOD,
        "coffee" to ExpenseCategory.FOOD,
        
        // Transport
        "transport" to ExpenseCategory.TRANSPORT,
        "travel" to ExpenseCategory.TRANSPORT,
        "bus" to ExpenseCategory.TRANSPORT,
        "train" to ExpenseCategory.TRANSPORT,
        "flight" to ExpenseCategory.TRANSPORT,
        "uber" to ExpenseCategory.TRANSPORT,
        "ola" to ExpenseCategory.TRANSPORT,
        "rapido" to ExpenseCategory.TRANSPORT,
        "petrol" to ExpenseCategory.TRANSPORT,
        "fuel" to ExpenseCategory.TRANSPORT,
        "shell" to ExpenseCategory.TRANSPORT,
        
        // Shopping
        "shopping" to ExpenseCategory.SHOPPING,
        "amazon" to ExpenseCategory.SHOPPING,
        "flipkart" to ExpenseCategory.SHOPPING,
        "myntra" to ExpenseCategory.SHOPPING,
        "zara" to ExpenseCategory.SHOPPING,
        
        // Grocery
        "grocery" to ExpenseCategory.GROCERY,
        "bigbasket" to ExpenseCategory.GROCERY,
        "zepto" to ExpenseCategory.GROCERY,
        "blinkit" to ExpenseCategory.GROCERY,
        "dmart" to ExpenseCategory.GROCERY,
        
        // Bills
        "bill" to ExpenseCategory.BILLS,
        "bescom" to ExpenseCategory.BILLS,
        "jio" to ExpenseCategory.BILLS,
        "airtel" to ExpenseCategory.BILLS,
        "act" to ExpenseCategory.BILLS,
        "netflix" to ExpenseCategory.BILLS
    )

    fun predictCategory(merchantName: String): ExpenseCategory {
        val normalized = merchantName.lowercase().trim()
        
        for ((keyword, category) in KEYWORD_MAP) {
            if (normalized.contains(keyword)) {
                return category
            }
        }
        
        return ExpenseCategory.GENERAL
    }
}