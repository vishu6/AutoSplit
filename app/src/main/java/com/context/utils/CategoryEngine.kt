package com.context.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    INVESTMENT("Investment", Icons.Default.AccountBalanceWallet),
    OTHER("Other", Icons.Default.Lightbulb) // General fallback
}

object CategoryEngine {

    // The "Brain" - Map keywords to Categories
    private val KEYWORD_MAP = mapOf(
        // Food & Drink
        "food" to ExpenseCategory.FOOD,
        "dinner" to ExpenseCategory.FOOD,
        "lunch" to ExpenseCategory.FOOD,
        "breakfast" to ExpenseCategory.FOOD,
        "swiggy" to ExpenseCategory.FOOD,
        "zomato" to ExpenseCategory.FOOD,
        "dominos" to ExpenseCategory.FOOD,
        "pizza" to ExpenseCategory.FOOD,
        "burger" to ExpenseCategory.FOOD,
        "kfc" to ExpenseCategory.FOOD,
        "subway" to ExpenseCategory.FOOD,
        "mcdonald" to ExpenseCategory.FOOD,
        "starbucks" to ExpenseCategory.FOOD,
        "coffee" to ExpenseCategory.FOOD,
        "tea" to ExpenseCategory.FOOD,
        "cafe" to ExpenseCategory.FOOD,
        "restaurant" to ExpenseCategory.FOOD,
        "bakery" to ExpenseCategory.FOOD,
        "sweets" to ExpenseCategory.FOOD,
        "snacks" to ExpenseCategory.FOOD,
        "biryani" to ExpenseCategory.FOOD,
        "pub" to ExpenseCategory.FOOD,
        "bar" to ExpenseCategory.FOOD,
        "club" to ExpenseCategory.FOOD,
        "wine" to ExpenseCategory.FOOD,
        "alcohol" to ExpenseCategory.FOOD,
        "beer" to ExpenseCategory.FOOD,
        "drinks" to ExpenseCategory.FOOD,
        
        // Transport
        "transport" to ExpenseCategory.TRANSPORT,
        "bus" to ExpenseCategory.TRANSPORT,
        "train" to ExpenseCategory.TRANSPORT,
        "uber" to ExpenseCategory.TRANSPORT,
        "ola" to ExpenseCategory.TRANSPORT,
        "rapido" to ExpenseCategory.TRANSPORT,
        "taxi" to ExpenseCategory.TRANSPORT,
        "cab" to ExpenseCategory.TRANSPORT,
        "petrol" to ExpenseCategory.TRANSPORT,
        "fuel" to ExpenseCategory.TRANSPORT,
        "diesel" to ExpenseCategory.TRANSPORT,
        "shell" to ExpenseCategory.TRANSPORT,
        "parking" to ExpenseCategory.TRANSPORT,
        "toll" to ExpenseCategory.TRANSPORT,
        "metro" to ExpenseCategory.TRANSPORT,
        "auto" to ExpenseCategory.TRANSPORT,
        "redbus" to ExpenseCategory.TRANSPORT,
        
        // Shopping
        "shopping" to ExpenseCategory.SHOPPING,
        "amazon" to ExpenseCategory.SHOPPING,
        "flipkart" to ExpenseCategory.SHOPPING,
        "myntra" to ExpenseCategory.SHOPPING,
        "zara" to ExpenseCategory.SHOPPING,
        "h&m" to ExpenseCategory.SHOPPING,
        "levi" to ExpenseCategory.SHOPPING,
        "nike" to ExpenseCategory.SHOPPING,
        "adidas" to ExpenseCategory.SHOPPING,
        "clothes" to ExpenseCategory.SHOPPING,
        "shoes" to ExpenseCategory.SHOPPING,
        "lifestyle" to ExpenseCategory.SHOPPING,
        "mall" to ExpenseCategory.SHOPPING,
        "electronics" to ExpenseCategory.SHOPPING,
        "apple" to ExpenseCategory.SHOPPING,
        "samsung" to ExpenseCategory.SHOPPING,
        "gadget" to ExpenseCategory.SHOPPING,
        "mobile" to ExpenseCategory.SHOPPING, // Can be bill too, but often phone purchase
        
        // Grocery
        "grocery" to ExpenseCategory.GROCERY,
        "groceries" to ExpenseCategory.GROCERY,
        "bigbasket" to ExpenseCategory.GROCERY,
        "zepto" to ExpenseCategory.GROCERY,
        "blinkit" to ExpenseCategory.GROCERY,
        "dmart" to ExpenseCategory.GROCERY,
        "milk" to ExpenseCategory.GROCERY,
        "vegetable" to ExpenseCategory.GROCERY,
        "fruit" to ExpenseCategory.GROCERY,
        "supermarket" to ExpenseCategory.GROCERY,
        "hypermarket" to ExpenseCategory.GROCERY,
        "kirana" to ExpenseCategory.GROCERY,
        "reliance fresh" to ExpenseCategory.GROCERY,
        "more" to ExpenseCategory.GROCERY,
        "spar" to ExpenseCategory.GROCERY,
        "big bazaar" to ExpenseCategory.GROCERY,
        
        // Bills & Utilities
        "bill" to ExpenseCategory.BILLS,
        "utility" to ExpenseCategory.BILLS,
        "electricity" to ExpenseCategory.BILLS,
        "bescom" to ExpenseCategory.BILLS,
        "water" to ExpenseCategory.BILLS,
        "gas" to ExpenseCategory.BILLS,
        "cylinder" to ExpenseCategory.BILLS,
        "indane" to ExpenseCategory.BILLS,
        "hp gas" to ExpenseCategory.BILLS,
        "jio" to ExpenseCategory.BILLS,
        "airtel" to ExpenseCategory.BILLS,
        "vi " to ExpenseCategory.BILLS, // Space to avoid "visit"
        "act" to ExpenseCategory.BILLS,
        "broadband" to ExpenseCategory.BILLS,
        "wifi" to ExpenseCategory.BILLS,
        "recharge" to ExpenseCategory.BILLS,
        "netflix" to ExpenseCategory.BILLS,
        "amazon prime" to ExpenseCategory.BILLS,
        "hotstar" to ExpenseCategory.BILLS,
        "spotify" to ExpenseCategory.BILLS,
        "youtube premium" to ExpenseCategory.BILLS,
        "insurance" to ExpenseCategory.BILLS,
        "rent" to ExpenseCategory.BILLS,
        "maintenance" to ExpenseCategory.BILLS,
        
        // Travel
        "travel" to ExpenseCategory.TRAVEL,
        "flight" to ExpenseCategory.TRAVEL,
        "indigo" to ExpenseCategory.TRAVEL,
        "vistara" to ExpenseCategory.TRAVEL,
        "air india" to ExpenseCategory.TRAVEL,
        "makemytrip" to ExpenseCategory.TRAVEL,
        "mmt" to ExpenseCategory.TRAVEL,
        "goibibo" to ExpenseCategory.TRAVEL,
        "booking" to ExpenseCategory.TRAVEL,
        "agoda" to ExpenseCategory.TRAVEL,
        "hotel" to ExpenseCategory.TRAVEL,
        "stay" to ExpenseCategory.TRAVEL,
        "resort" to ExpenseCategory.TRAVEL,
        "airbnb" to ExpenseCategory.TRAVEL,
        "hostel" to ExpenseCategory.TRAVEL,
        "sightseeing" to ExpenseCategory.TRAVEL,
        "vacation" to ExpenseCategory.TRAVEL,
        "trip" to ExpenseCategory.TRAVEL,

        // Entertainment
        "movie" to ExpenseCategory.ENTERTAINMENT,
        "cinema" to ExpenseCategory.ENTERTAINMENT,
        "bookmyshow" to ExpenseCategory.ENTERTAINMENT,
        "inox" to ExpenseCategory.ENTERTAINMENT,
        "pvr" to ExpenseCategory.ENTERTAINMENT,
        "ticket" to ExpenseCategory.ENTERTAINMENT,
        "event" to ExpenseCategory.ENTERTAINMENT,
        "concert" to ExpenseCategory.ENTERTAINMENT,
        "park" to ExpenseCategory.ENTERTAINMENT,
        "zoo" to ExpenseCategory.ENTERTAINMENT,
        "museum" to ExpenseCategory.ENTERTAINMENT,
        "gaming" to ExpenseCategory.ENTERTAINMENT,
        "steam" to ExpenseCategory.ENTERTAINMENT,
        "playstation" to ExpenseCategory.ENTERTAINMENT,
        "xbox" to ExpenseCategory.ENTERTAINMENT,

        // Health & Wellness
        "health" to ExpenseCategory.HEALTH,
        "wellness" to ExpenseCategory.HEALTH,
        "doctor" to ExpenseCategory.HEALTH,
        "clinic" to ExpenseCategory.HEALTH,
        "hospital" to ExpenseCategory.HEALTH,
        "medicine" to ExpenseCategory.HEALTH,
        "medical" to ExpenseCategory.HEALTH,
        "pharmacy" to ExpenseCategory.HEALTH,
        "apollo" to ExpenseCategory.HEALTH,
        "pharmeasy" to ExpenseCategory.HEALTH,
        "lab" to ExpenseCategory.HEALTH,
        "dental" to ExpenseCategory.HEALTH,
        "fitness" to ExpenseCategory.HEALTH,
        "gym" to ExpenseCategory.HEALTH,
        "yoga" to ExpenseCategory.HEALTH,
        "spa" to ExpenseCategory.HEALTH,
        "salon" to ExpenseCategory.HEALTH,
        "parlour" to ExpenseCategory.HEALTH,

        // Education
        "education" to ExpenseCategory.EDUCATION,
        "school" to ExpenseCategory.EDUCATION,
        "college" to ExpenseCategory.EDUCATION,
        "university" to ExpenseCategory.EDUCATION,
        "fee" to ExpenseCategory.EDUCATION,
        "tuition" to ExpenseCategory.EDUCATION,
        "course" to ExpenseCategory.EDUCATION,
        "udemy" to ExpenseCategory.EDUCATION,
        "coursera" to ExpenseCategory.EDUCATION,
        "book" to ExpenseCategory.EDUCATION,
        "stationery" to ExpenseCategory.EDUCATION,
        "library" to ExpenseCategory.EDUCATION,

        // Work
        "work" to ExpenseCategory.WORK,
        "office" to ExpenseCategory.WORK,
        "printing" to ExpenseCategory.WORK,
        "software" to ExpenseCategory.WORK,
        "saas" to ExpenseCategory.WORK,
        "subscription" to ExpenseCategory.WORK,
        "laptop" to ExpenseCategory.WORK,
        "coworking" to ExpenseCategory.WORK,

        // Gifts & Donations
        "gift" to ExpenseCategory.GIFTS,
        "donation" to ExpenseCategory.GIFTS,
        "charity" to ExpenseCategory.GIFTS,
        "temple" to ExpenseCategory.GIFTS,
        "church" to ExpenseCategory.GIFTS,
        "mosque" to ExpenseCategory.GIFTS,
        "birthday" to ExpenseCategory.GIFTS,
        "anniversary" to ExpenseCategory.GIFTS,
        "wedding" to ExpenseCategory.GIFTS,

        // Family & Personal
        "family" to ExpenseCategory.FAMILY,
        "personal" to ExpenseCategory.FAMILY,
        "home" to ExpenseCategory.FAMILY,
        "child" to ExpenseCategory.FAMILY,
        "parent" to ExpenseCategory.FAMILY,

        // Investment
        "zerodha" to ExpenseCategory.INVESTMENT,
        "groww" to ExpenseCategory.INVESTMENT,
        "upstox" to ExpenseCategory.INVESTMENT,
        "indmoney" to ExpenseCategory.INVESTMENT,
        "stocks" to ExpenseCategory.INVESTMENT,
        "mutual fund" to ExpenseCategory.INVESTMENT,
        "sip" to ExpenseCategory.INVESTMENT,
        "etf" to ExpenseCategory.INVESTMENT,
        "crypto" to ExpenseCategory.INVESTMENT,
        "bitcoin" to ExpenseCategory.INVESTMENT,
        "coinbase" to ExpenseCategory.INVESTMENT,
        "wazirx" to ExpenseCategory.INVESTMENT,
        "gold" to ExpenseCategory.INVESTMENT,
        "silver" to ExpenseCategory.INVESTMENT,
        "investment" to ExpenseCategory.INVESTMENT,
        "equity" to ExpenseCategory.INVESTMENT,
        "dividend" to ExpenseCategory.INVESTMENT,
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
