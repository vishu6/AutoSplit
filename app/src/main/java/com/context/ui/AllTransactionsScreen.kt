package com.context.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.context.components.ModernSearchBar
import com.context.components.ModernTimeRangeFilter
import com.context.ui.theme.ContextTheme
import com.context.utils.HapticUtils
import com.context.utils.SecurityUtils
import com.context.utils.TimeRange
import java.util.*

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
    val searchQuery by homeViewModel.searchQuery.collectAsState()
    val isSearchActive by homeViewModel.isSearchActive.collectAsState()
    
    val privacyMode by remember { SecurityUtils.getPrivacyModeFlow(context) }.collectAsState(initial = SecurityUtils.isPrivacyModeEnabled(context))

    BackHandler(enabled = isSearchActive) {
        homeViewModel.setSearchActive(false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (!isSearchActive) {
                        Text("All Transactions", fontWeight = FontWeight.Bold)
                    } else {
                        ModernSearchBar(
                            query = searchQuery,
                            onQueryChange = { homeViewModel.onSearchQueryChanged(it) },
                            onClearSearch = { homeViewModel.setSearchActive(false) },
                            placeholder = "Search in ${selectedRange.name.lowercase()}..."
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticUtils.playTick(context)
                        if (isSearchActive) homeViewModel.setSearchActive(false) else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = {
                            HapticUtils.playTick(context)
                            homeViewModel.setSearchActive(true)
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
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
            ModernTimeRangeFilter(
                selectedRange = selectedRange,
                onRangeSelected = { range ->
                    HapticUtils.playTick(context)
                    homeViewModel.onTimeRangeSelected(range)
                },
                calendar = currentCalendar,
                onNext = {
                    HapticUtils.playTick(context)
                    homeViewModel.onNextPeriod()
                },
                onPrevious = {
                    HapticUtils.playTick(context)
                    homeViewModel.onPreviousPeriod()
                },
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (filteredExpenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isEmpty()) "No transactions found" else "No matches for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
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
                            TransactionItemCard(
                                transaction = details,
                                isPrivacyMode = privacyMode
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F7FA)
@Composable
fun AllTransactionsScreenPreview() {
    val context = LocalContext.current
    ContextTheme {
        AllTransactionsScreen(
            homeViewModel = FakeHomeViewModelFactory.create(context),
            onBack = {}, 
            onExpenseClick = {}
        )
    }
}
