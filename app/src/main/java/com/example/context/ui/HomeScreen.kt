package com.example.context.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// --- THEME ---

val ElectricBlue = Color(0xFF2962FF)
val LightBlue = Color(0xFF5383FF)
val OffWhite = Color(0xFFF5F7FA)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    background = OffWhite,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
)

@Composable
fun ContextTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

// --- DATA ---

val mockTransactions = listOf(
    TransactionDetails(
        merchant = "Netflix",
        dateTime = "Today, 08:30 PM",
        amount = "899",
        type = TransactionType.EXPENSE,
        category = Category.OTHER,
        source = TransactionSource.AUTO_DETECTED
    ),
    TransactionDetails(
        merchant = "Starbucks",
        dateTime = "Yesterday, 10:15 AM",
        amount = "1,250",
        type = TransactionType.EXPENSE,
        category = Category.FOOD,
        source = TransactionSource.AUTO_DETECTED
    ),
    TransactionDetails(
        merchant = "Salary Credit",
        dateTime = "Jan 31, 09:00 AM",
        amount = "50,000",
        type = TransactionType.CREDIT,
        category = Category.OTHER,
        source = TransactionSource.MANUAL
    )
)

// --- COMPOSABLES ---

@Composable
fun HomeScreen(transactions: List<TransactionDetails>) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = { /* TODO */ },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            HomeTopBar(name = "Vish")
            Spacer(modifier = Modifier.height(24.dp))
            BalanceSummaryCard(amount = "15,400")
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (transactions.isEmpty()) {
                EmptyState()
            } else {
                TransactionList(transactions = transactions)
            }
        }
    }
}

@Composable
fun HomeTopBar(name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Good Morning, $name",
            style = MaterialTheme.typography.titleMedium
        )
        Image(
            imageVector = Icons.Default.Person,
            contentDescription = "Profile",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp)
        )
    }
}

@Composable
fun BalanceSummaryCard(amount: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(ElectricBlue, LightBlue)
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Total Spent This Month",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "₹$amount",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun TransactionList(transactions: List<TransactionDetails>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(transactions) { transaction ->
            TransactionItemCard(transaction)
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Empty",
            modifier = Modifier.size(64.dp),
            tint = Color.LightGray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Waiting for new expenses...",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}


// --- PREVIEWS ---

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    ContextTheme {
        HomeScreen(transactions = mockTransactions)
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenEmptyPreview() {
    ContextTheme {
        HomeScreen(transactions = emptyList())
    }
}
