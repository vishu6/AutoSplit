package com.context.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.context.utils.ExpenseCategory

// Define your Category Colors (Pastel/Modern Fintech shades)
val ColorFood = Color(0xFFFFE0B2)       // Soft Orange
val ColorTransport = Color(0xFFBBDEFB)  // Soft Blue
val ColorShopping = Color(0xFFE1BEE7)   // Soft Purple
val ColorBills = Color(0xFFFFCDD2)      // Soft Red
val ColorGrocery = Color(0xFFC8E6C9)    // Soft Green
val ColorGeneral = Color(0xFFE0E0E0)    // Grey 300 - A more visible grey

data class CategoryStyle(
    val color: Color,
    val icon: ImageVector
)

object CategoryStyling {
    
    fun getStyle(categoryName: String): CategoryStyle {
        // Find the enum match (or default to GENERAL)
        val category = ExpenseCategory.values().find { it.label == categoryName } 
            ?: ExpenseCategory.GENERAL

        return when (category) {
            ExpenseCategory.FOOD -> CategoryStyle(ColorFood, category.icon)
            ExpenseCategory.TRANSPORT -> CategoryStyle(ColorTransport, category.icon)
            ExpenseCategory.SHOPPING -> CategoryStyle(ColorShopping, category.icon)
            ExpenseCategory.GROCERY -> CategoryStyle(ColorGrocery, category.icon)
            ExpenseCategory.BILLS -> CategoryStyle(ColorBills, category.icon)
            ExpenseCategory.GENERAL -> CategoryStyle(ColorGeneral, category.icon)
        }
    }
}
