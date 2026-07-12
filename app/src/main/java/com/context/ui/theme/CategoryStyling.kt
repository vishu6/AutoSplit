package com.context.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.toColorInt
import com.context.data.Category
import com.context.utils.ExpenseCategory

// Define your Category Colors (Pastel/Modern Fintech shades)
val ColorFoodSoft = Color(0xFFFFE0B2)       
val ColorTransportSoft = Color(0xFFBBDEFB)  
val ColorShoppingSoft = Color(0xFFE1BEE7)   
val ColorBillsSoft = Color(0xFFFFCDD2)      
val ColorGrocerySoft = Color(0xFFC8E6C9)    
val ColorEntertainmentSoft = Color(0xFFD1C4E9) 
val ColorHealthSoft = Color(0xFFB2EBF2)      
val ColorTravelSoft = Color(0xFFDCEDC8)    
val ColorEducationSoft = Color(0xFFFFF9C4)   
val ColorWorkSoft = Color(0xFFCFD8DC)      
val ColorGiftsSoft = Color(0xFFFFD180)     
val ColorFamilySoft = Color(0xFFFFCCBC)    
val ColorInvestmentSoft = Color(0xFFA5D6A7) 
val ColorOtherSoft = Color(0xFFE0E0E0)      

val ColorFoodBold = Color(0xFFBF360C)
val ColorTransportBold = Color(0xFF0D47A1)
val ColorShoppingBold = Color(0xFF4A148C)
val ColorBillsBold = Color(0xFFB71C1C)
val ColorGroceryBold = Color(0xFF1B5E20)
val ColorEntertainmentBold = Color(0xFF311B92)
val ColorHealthBold = Color(0xFF006064)
val ColorTravelBold = Color(0xFF33691E)
val ColorEducationBold = Color(0xFFF57F17)
val ColorWorkBold = Color(0xFF263238)
val ColorGiftsBold = Color(0xFFE65100)
val ColorFamilyBold = Color(0xFFBF360C)
val ColorInvestmentBold = Color(0xFF1B5E20)
val ColorOtherBold = Color(0xFF212121)

data class CategoryStyle(
    val color: Color,
    val icon: ImageVector,
    val boldColor: Color
)

object CategoryStyling {
    
    fun getStyle(
        categoryName: String, 
        customCategories: Map<String, Category>? = null,
        customColorHex: String? = null, 
        customIconName: String? = null
    ): CategoryStyle {
        // 1. SYSTEM LOOKUP FIRST
        val systemCategory = ExpenseCategory.entries.find { it.label == categoryName } 
        if (systemCategory != null) {
            return when (systemCategory) {
                ExpenseCategory.FOOD -> CategoryStyle(ColorFoodSoft, systemCategory.icon, ColorFoodBold)
                ExpenseCategory.TRANSPORT -> CategoryStyle(ColorTransportSoft, systemCategory.icon, ColorTransportBold)
                ExpenseCategory.SHOPPING -> CategoryStyle(ColorShoppingSoft, systemCategory.icon, ColorShoppingBold)
                ExpenseCategory.GROCERY -> CategoryStyle(ColorGrocerySoft, systemCategory.icon, ColorGroceryBold)
                ExpenseCategory.BILLS -> CategoryStyle(ColorBillsSoft, systemCategory.icon, ColorBillsBold)
                ExpenseCategory.ENTERTAINMENT -> CategoryStyle(ColorEntertainmentSoft, systemCategory.icon, ColorEntertainmentBold)
                ExpenseCategory.HEALTH -> CategoryStyle(ColorHealthSoft, systemCategory.icon, ColorHealthBold)
                ExpenseCategory.TRAVEL -> CategoryStyle(ColorTravelSoft, systemCategory.icon, ColorTravelBold)
                ExpenseCategory.EDUCATION -> CategoryStyle(ColorEducationSoft, systemCategory.icon, ColorEducationBold)
                ExpenseCategory.WORK -> CategoryStyle(ColorWorkSoft, systemCategory.icon, ColorWorkBold)
                ExpenseCategory.GIFTS -> CategoryStyle(ColorGiftsSoft, systemCategory.icon, ColorGiftsBold)
                ExpenseCategory.FAMILY -> CategoryStyle(ColorFamilySoft, systemCategory.icon, ColorFamilyBold)
                ExpenseCategory.INVESTMENT -> CategoryStyle(ColorInvestmentSoft, systemCategory.icon, ColorInvestmentBold)
                ExpenseCategory.OTHER -> CategoryStyle(ColorOtherSoft, systemCategory.icon, ColorOtherBold)
            }
        }

        // 2. Specific provided custom data
        if (customColorHex != null || customIconName != null) {
            return createCustomStyle(customColorHex, customIconName)
        }

        // 3. Map lookup (performance optimized for lists)
        val customCat = customCategories?.get(categoryName)
        if (customCat != null && !customCat.isSystem) {
            return createCustomStyle(customCat.colorHex, customCat.iconName)
        }

        // 4. Final Fallback
        return CategoryStyle(
            color = ColorOtherSoft,
            icon = Icons.Default.Category,
            boldColor = ColorOtherBold
        )
    }

    private fun createCustomStyle(colorHex: String?, iconName: String?): CategoryStyle {
        val baseColor = if (!colorHex.isNullOrBlank()) {
            try { Color(colorHex.toColorInt()) } catch (e: Exception) { Color(0xFF2962FF) }
        } else {
            Color(0xFF2962FF)
        }

        return CategoryStyle(
            // FIXED: Increased alpha to 30% for better visibility in charts
            color = baseColor.copy(alpha = 0.3f),
            icon = getIconByName(iconName),
            boldColor = baseColor
        )
    }

    private fun getIconByName(name: String?): ImageVector {
        return when (name) {
            "Favorite" -> Icons.Default.Favorite
            "Pets" -> Icons.Default.Pets
            "Brush" -> Icons.Default.Brush
            "Sports" -> Icons.Default.SportsBasketball
            "Music" -> Icons.Default.MusicNote
            "Home" -> Icons.Default.Home
            "Star" -> Icons.Default.Star
            "Coffee" -> Icons.Default.Coffee
            "Laptop" -> Icons.Default.Laptop
            "Camera" -> Icons.Default.PhotoCamera
            else -> Icons.Default.Category
        }
    }
}
