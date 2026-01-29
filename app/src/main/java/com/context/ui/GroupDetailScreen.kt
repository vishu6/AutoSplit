package com.context.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.context.data.Expense
import com.context.ui.theme.ContextTheme
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    onBack: () -> Unit, 
    onAddExpenseClick: () -> Unit, 
    onSettleUpClick: () -> Unit,
    onExpenseClick: (Int) -> Unit, // <-- RENAMED
    viewModel: GroupDetailViewModel = hiltViewModel()
) {
    val expenses by viewModel.expenses.collectAsState()
    val group by viewModel.group.collectAsState()
    val perHeadCost by viewModel.perHeadCost.collectAsState()
    val totalSpent by viewModel.groupTotal.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(group?.name ?: "Group Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = onSettleUpClick,
                    containerColor = Color(0xFFE8F5E9),
                    contentColor = Color(0xFF2E7D32)
                ) {
                    Icon(Icons.Default.AttachMoney, contentDescription = "Settle")
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                FloatingActionButton(onClick = onAddExpenseClick) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Text(
                text = "₹${String.format("%.2f", totalSpent)}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "₹${String.format("%.2f", perHeadCost)} / person",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            Text(
                text = "Split between ${group?.getMemberList()?.size ?: 1} members",
                style = MaterialTheme.typography.labelSmall
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(expenses) { expense ->
                    Box(modifier = Modifier.clickable { onExpenseClick(expense.id) }) { // <-- RENAMED
                        TransactionItemCard(transaction = expense.toTransactionDetails())
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GroupDetailScreenPreview() {
    ContextTheme {
        // This preview is limited as it can't use a Hilt ViewModel
    }
}
