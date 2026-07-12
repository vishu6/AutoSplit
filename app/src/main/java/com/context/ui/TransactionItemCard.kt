package com.context.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.context.data.Category
import com.context.ui.theme.CategoryStyle
import com.context.ui.theme.CategoryStyling
import com.context.ui.theme.ContextTheme

@Composable
fun TransactionItemCard(
    transaction: TransactionDetails,
    isPrivacyMode: Boolean = false,
    categoryMap: Map<String, Category> = emptyMap()
) {
    val isSettlement = transaction.category == "Settlement"

    val categoryStyle = if (isSettlement) {
        CategoryStyle(
            color = Color(0xFFE8F5E9), 
            icon = Icons.Default.CheckCircle,
            boldColor = Color(0xFF2E7D32)
        )
    } else {
        // FIXED: Now uses the categoryMap to resolve custom icons like "Pets"
        CategoryStyling.getStyle(transaction.category, customCategories = categoryMap)
    }

    val amountColor = if (isSettlement) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSettlement) categoryStyle.color else categoryStyle.color.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = categoryStyle.icon,
                    contentDescription = transaction.category,
                    tint = categoryStyle.boldColor,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    text = transaction.merchant, 
                    style = MaterialTheme.typography.bodyLarge, 
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = if (isSettlement) "Repayment" else transaction.dateTime, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val amountText = if (isPrivacyMode) "••••" else transaction.amount
                Text(
                    text = (if (isSettlement) "+ " else "") + "₹$amountText",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                if (transaction.source == TransactionSource.AUTO_DETECTED && !isSettlement) {
                    val label = if (transaction.autoSource != null) "via ${transaction.autoSource}" else "Auto-Detected"
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F7FA)
@Composable
fun TransactionItemCardPreview() {
    val foodExpense = TransactionDetails(
        merchant = "Zomato",
        dateTime = "Today, 10:30 AM",
        amount = "250",
        type = TransactionType.EXPENSE,
        category = "Food & Drink",
        source = TransactionSource.AUTO_DETECTED,
        isAuto = true,
        autoSource = "PhonePe"
    )
    
    ContextTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TransactionItemCard(transaction = foodExpense, isPrivacyMode = false)
            TransactionItemCard(transaction = foodExpense, isPrivacyMode = true)
        }
    }
}
