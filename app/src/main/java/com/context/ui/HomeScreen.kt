package com.context.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.context.components.DonutChart
import com.context.data.Expense
import com.context.data.Group
import com.context.ui.theme.CategoryStyling
import com.context.ui.theme.ContextTheme
import com.context.ui.theme.ElectricBlue
import com.context.ui.theme.LightBlue
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    onNavigateToGroup: (Int) -> Unit,
    onCreateGroupClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onExpenseClick: (Int) -> Unit, // <-- RENAMED
    onProfileClick: () -> Unit
) {
    val transactions by homeViewModel.allExpenses.collectAsState(initial = emptyList())
    val groups by homeViewModel.groups.collectAsState(initial = emptyList())
    val totalSpent by homeViewModel.totalSpent.collectAsState(initial = 0.0)

    val spendingExpenses = remember(transactions) {
        transactions.filter { it.category != "Settlement" }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onAddExpenseClick,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    HomeTopBar(name = "Vishwanath", onProfileClick = onProfileClick)
                    Spacer(modifier = Modifier.height(24.dp))
                    BalanceSummaryCard(amount = totalSpent?.toString() ?: "0.0")
                }
            }

            if (spendingExpenses.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Spend Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DonutChart(expenses = spendingExpenses, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(24.dp))
                        ChartLegend(expenses = spendingExpenses, modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "My Groups",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                GroupsList(
                    groups = groups,
                    expenses = transactions, 
                    onGroupClick = onNavigateToGroup,
                    onNewGroupClick = onCreateGroupClick
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (transactions.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                items(transactions) { expense ->
                    val details = expense.toTransactionDetails()
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable { onExpenseClick(expense.id) } // <-- RENAMED
                    ) {
                        TransactionItemCard(transaction = details)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

fun Expense.toTransactionDetails(): TransactionDetails {
    val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    return TransactionDetails(
        merchant = this.merchant,
        dateTime = sdf.format(this.timestamp),
        amount = this.amount.toString(),
        type = TransactionType.EXPENSE,
        category = this.category,
        source = if (this.isAuto) TransactionSource.AUTO_DETECTED else TransactionSource.MANUAL,
        isAuto = this.isAuto
    )
}

@Composable
private fun HomeTopBar(name: String, onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Good Morning, $name",
            style = MaterialTheme.typography.titleLarge
        )
        Image(
            imageVector = Icons.Default.Person,
            contentDescription = "Profile",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onProfileClick() }
                .padding(8.dp)
        )
    }
}

@Composable
private fun BalanceSummaryCard(amount: String) {
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
                .fillMaxWidth()
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
private fun ChartLegend(expenses: List<Expense>, modifier: Modifier = Modifier) {
    val categoryTotals = remember(expenses) {
        expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }
    val totalAmount = categoryTotals.values.sum()

    Column(modifier = modifier) {
        categoryTotals.forEach { (category, amount) ->
            val percentage = (amount / totalAmount * 100).toFloat()
            val style = CategoryStyling.getStyle(category)
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Box(modifier = Modifier.size(12.dp).background(style.color, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(category, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("${String.format("%.1f", percentage)}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GroupsList(groups: List<Group>, expenses: List<Expense>, onGroupClick: (Int) -> Unit, onNewGroupClick: () -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item {
            NewGroupCard(onClick = onNewGroupClick)
        }
        items(groups) { group ->
            val groupExpenses = expenses.filter { it.groupId == group.groupId }
            val groupTotal = groupExpenses
                .filter { it.category != "Settlement" } 
                .sumOf { it.amount }

            GroupCard(
                groupName = group.name,
                totalAmount = groupTotal, 
                onClick = { onGroupClick(group.groupId) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewGroupCard(onClick: () -> Unit) {
    val stroke = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
    Card(onClick = onClick, modifier = Modifier.height(140.dp).width(120.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()){
                drawRoundRect(color = Color.LightGray, style = stroke, cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Add, contentDescription = "New Group", tint = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Text("New Group", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupCard(groupName: String, totalAmount: Double, onClick: () -> Unit) {
    val pastelColors = listOf(
        Color(0xFFE3F2FD), // Pastel Blue
        Color(0xFFF3E5F5), // Pastel Purple
        Color(0xFFFFF0E5), // Pastel Orange
        Color(0xFFE8F5E9)  // Pastel Green
    )
    val cardColor = pastelColors[groupName.hashCode() % pastelColors.size]

    Card(
        modifier = Modifier
            .height(140.dp)
            .width(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = groupName,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "₹${totalAmount.toInt()}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(48.dp))
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

@Preview(showBackground = true, backgroundColor = 0xFFF5F7FA)
@Composable
fun HomeScreenWithGroupsPreview() {
    ContextTheme {
        HomeScreen(onNavigateToGroup = {}, onCreateGroupClick = {}, onAddExpenseClick = {}, onExpenseClick = {}, onProfileClick = {})
    }
}
