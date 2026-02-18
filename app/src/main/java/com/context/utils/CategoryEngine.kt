package com.context.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

enum class ExpenseCategory(val label: String, val icon: ImageVector) {
    // Original Categories
    FOOD("Food & Drink", Icons.Default.Dining),
    TRANSPORT("Transport", Icons.Default.DirectionsBus),
    GROCERY("Groceries", Icons.Default.LocalGroceryStore),
    SHOPPING("Shopping", Icons.Default.ShoppingBag),
    BILLS("Bills & Utilities", Icons.Default.Receipt),

    // New Categories
    ENTERTAINMENT("Entertainment", Icons.Default.Movie),
    HEALTH("Health & Wellness", Icons.Default.Spa),
    TRAVEL("Travel", Icons.Default.Flight),
    EDUCATION("Education", Icons.Default.School),
    WORK("Work", Icons.Default.Work),
    GIFTS("Gifts & Donations", Icons.Default.CardGiftcard),
    FAMILY("Family & Personal", Icons.Default.Groups),
    OTHER("Other", Icons.Default.Lightbulb) // General fallback
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
        "bus" to ExpenseCategory.TRANSPORT,
        "train" to ExpenseCategory.TRANSPORT,
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
        "netflix" to ExpenseCategory.BILLS,
        
        // Travel
        "travel" to ExpenseCategory.TRAVEL,
        "flight" to ExpenseCategory.TRAVEL,
        "indigo" to ExpenseCategory.TRAVEL,
        "vistara" to ExpenseCategory.TRAVEL,
        "makemytrip" to ExpenseCategory.TRAVEL,

        // Entertainment
        "movie" to ExpenseCategory.ENTERTAINMENT,
        "bookmyshow" to ExpenseCategory.ENTERTAINMENT,
        "inox" to ExpenseCategory.ENTERTAINMENT,
        "pvr" to ExpenseCategory.ENTERTAINMENT,

        // Health
        "health" to ExpenseCategory.HEALTH,
        "apollo" to ExpenseCategory.HEALTH,
        "pharmacy" to ExpenseCategory.HEALTH,
    )

    fun predictCategory(merchantName: String): ExpenseCategory {
        val normalized = merchantName.lowercase().trim()
        
        for ((keyword, category) in KEYWORD_MAP) {
            if (normalized.contains(keyword)) {
                return category
            }
        }
        
        return ExpenseCategory.OTHER // Default to Other
    }
}