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
val ColorEntertainment = Color(0xFFD1C4E9) // Soft Lavender
val ColorHealth = Color(0xFFB2EBF2)      // Soft Cyan
val ColorTravel = Color(0xFFDCEDC8)    // Soft Light Green
val ColorEducation = Color(0xFFFFF9C4)   // Soft Yellow
val ColorWork = Color(0xFFCFD8DC)      // Soft Blue Grey
val ColorGifts = Color(0xFFFFD180)     // Soft Amber
val ColorFamily = Color(0xFFFFCCBC)    // Soft Deep Orange
val ColorOther = Color(0xFFE0E0E0)      // Grey 300

data class CategoryStyle(
    val color: Color,
    val icon: ImageVector
)

object CategoryStyling {
    
    fun getStyle(categoryName: String): CategoryStyle {
        // Find the enum match (or default to OTHER)
        val category = ExpenseCategory.values().find { it.label == categoryName } 
            ?: ExpenseCategory.OTHER

        return when (category) {
            ExpenseCategory.FOOD -> CategoryStyle(ColorFood, category.icon)
            ExpenseCategory.TRANSPORT -> CategoryStyle(ColorTransport, category.icon)
            ExpenseCategory.SHOPPING -> CategoryStyle(ColorShopping, category.icon)
            ExpenseCategory.GROCERY -> CategoryStyle(ColorGrocery, category.icon)
            ExpenseCategory.BILLS -> CategoryStyle(ColorBills, category.icon)
            ExpenseCategory.ENTERTAINMENT -> CategoryStyle(ColorEntertainment, category.icon)
            ExpenseCategory.HEALTH -> CategoryStyle(ColorHealth, category.icon)
            ExpenseCategory.TRAVEL -> CategoryStyle(ColorTravel, category.icon)
            ExpenseCategory.EDUCATION -> CategoryStyle(ColorEducation, category.icon)
            ExpenseCategory.WORK -> CategoryStyle(ColorWork, category.icon)
            ExpenseCategory.GIFTS -> CategoryStyle(ColorGifts, category.icon)
            ExpenseCategory.FAMILY -> CategoryStyle(ColorFamily, category.icon)
            ExpenseCategory.OTHER -> CategoryStyle(ColorOther, category.icon)
        }
    }
}
