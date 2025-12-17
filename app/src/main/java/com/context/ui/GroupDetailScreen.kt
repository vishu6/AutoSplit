package com.context.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.context.data.Expense
import com.context.ui.theme.ContextTheme
import com.context.utils.ShareUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    onBack: () -> Unit,
    onAddExpenseClick: () -> Unit, // <--- NEW PARAMETER
    viewModel: GroupDetailViewModel = hiltViewModel()
) {

    val expenses by viewModel.expenses.collectAsState()
    val group by viewModel.group.collectAsState()
    val total by viewModel.groupTotal.collectAsState()

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
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "₹${String.format("%.2f", total)}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Bar
            ActionBar(
                expenses = expenses, 
                groupName = group?.name ?: "",
                onAddExpenseClick = onAddExpenseClick // <--- Pass the callback
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Expense List
            Text("Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(expenses) { expense ->
                    // Using a simplified item for now
                    Row {
                        Text(expense.merchant, modifier = Modifier.weight(1f))
                        Text("₹${expense.amount}")
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionBar(
    expenses: List<Expense>, 
    groupName: String, 
    onAddExpenseClick: () -> Unit // <--- NEW PARAMETER
) {
    val context = LocalContext.current

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = onAddExpenseClick, // <--- CONNECT IT HERE
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Text("Add Expense", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = {
                ShareUtils.shareGroupSummary(context, groupName, expenses.sumOf { it.amount }, expenses)
            },
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Share Bill", fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GroupDetailScreenPreview() {
    ContextTheme {
        // Preview won't work with Hilt ViewModel. We can create a mock ViewModel for previews if needed.
        // GroupDetailScreen(onBack = {}, onAddExpenseClick = {})
    }
}
