package com.context.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.context.ui.theme.ContextTheme
import com.context.utils.HapticUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AllTransactionsScreen(
    homeViewModel: HomeViewModel,
    onBack: () -> Unit,
    onExpenseClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val filteredExpenses by homeViewModel.filteredExpenses.collectAsState()
    val selectedRange by homeViewModel.selectedTimeRange.collectAsState()
    val currentCalendar by homeViewModel.currentCalendar.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Transactions") },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticUtils.playTick(context)
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TimeRangeFilter(
                selectedRange = selectedRange,
                onRangeSelected = { 
                    HapticUtils.playTick(context)
                    homeViewModel.onTimeRangeSelected(it) 
                },
                calendar = currentCalendar,
                onNext = { 
                    HapticUtils.playTick(context)
                    homeViewModel.onNextPeriod() 
                },
                onPrevious = { 
                    HapticUtils.playTick(context)
                    homeViewModel.onPreviousPeriod() 
                }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(filteredExpenses) { expense ->
                    val details = expense.toTransactionDetails()
                    Column(
                        modifier = Modifier.combinedClickable(
                            onClick = { onExpenseClick(expense.id) },
                            onLongClick = {
                                HapticUtils.playHeavyClick(context)
                                onExpenseClick(expense.id)
                            }
                        )
                    ) {
                        TransactionItemCard(transaction = details)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F7FA)
@Composable
fun AllTransactionsScreenPreview() {
    ContextTheme {
        AllTransactionsScreen(
            homeViewModel = FakeHomeViewModelFactory.create(),
            onBack = {}, 
            onExpenseClick = {}
        )
    }
}
