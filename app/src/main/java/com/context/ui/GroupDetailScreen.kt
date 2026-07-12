package com.context.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.context.components.DonutChart
import com.context.data.Expense
import com.context.data.Group
import com.context.ui.theme.CategoryStyling
import com.context.ui.theme.ElectricBlue
import com.context.utils.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    onBack: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onSettleUpClick: () -> Unit,
    onExpenseClick: (Int) -> Unit,
    viewModel: GroupDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val expenses by viewModel.expenses.collectAsState()
    val group by viewModel.group.collectAsState()
    val totalSpent by viewModel.groupTotal.collectAsState()
    val yourBalance by viewModel.yourBalance.collectAsState()
    val memberBalances by viewModel.memberBalances.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    
    val privacyMode by remember { SecurityUtils.getPrivacyModeFlow(context) }.collectAsState(initial = SecurityUtils.isPrivacyModeEnabled(context))

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var selectedChartCategory by remember { mutableStateOf<String?>(null) }

    val displayExpenses = remember(expenses, selectedChartCategory) {
        if (selectedChartCategory == null) expenses
        else expenses.filter { it.category == selectedChartCategory }
    }

    LaunchedEffect(group) {
        group?.let { viewModel.startSync(it) }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { Text("Delete Group?") },
            text = { Text("Are you sure you want to delete this group? All expenses will be moved to your personal account.") },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isDeleting) {
                            isDeleting = true
                            scope.launch {
                                try {
                                    viewModel.deleteGroup()
                                    onBack()
                                } catch (e: Exception) {
                                    isDeleting = false
                                }
                            }
                        }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Delete")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !isDeleting) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        Column {
                            Text(group?.name ?: if (isDeleting) "Deleting..." else "Group Details")
                            if (isSyncing) {
                                Text(
                                    "Syncing with members...", 
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (group != null) {
                            IconButton(onClick = {
                                ShareUtils.shareGroupSummary(context, group!!, expenses)
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share Summary")
                            }

                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                        }
                        
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Invite Member") },
                                onClick = { 
                                    showMenu = false
                                    group?.let { g ->
                                        val myName = OnboardingUtils.getUserName(context)
                                        val inviteLink = "https://autosplit-fdf12.web.app/join?id=${g.remoteId}&key=${Uri.encode(g.syncKey)}&name=${Uri.encode(g.name)}&user=${Uri.encode(myName)}&invitee=${Uri.encode(myName)}"
                                        
                                        val shareMessage = "Join my Cleave group '${g.name}' to track expenses together!\n\n" +
                                                          "Click to join: $inviteLink"
                                        
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, shareMessage)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, "Invite Member")
                                        context.startActivity(shareIntent)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Settle Up") },
                                onClick = { onSettleUpClick(); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Savings, contentDescription = "Settle Up") }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Group") },
                                onClick = { showDeleteDialog = true; showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete Group") }
                            )
                        }
                    }
                )
                
                AnimatedVisibility(
                    visible = isSyncing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
            }
        },
        floatingActionButton = {
            if (group != null) {
                FloatingActionButton(
                    onClick = onAddExpenseClick,
                    containerColor = ElectricBlue, // SOLID OPAQUE COLOR
                    contentColor = Color.White,    // CONTRAST ICON
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            }
        }
    ) { padding ->
        if (group == null && !isDeleting) {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                 CircularProgressIndicator()
             }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text(
                                    text = "Total Spending",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                val totalText = CurrencyMasker.formatAmount(totalSpent, privacyMode)
                                Text(
                                    text = totalText,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                val balancePrefix = if (yourBalance >= 0) "You are owed" else "You owe"
                                val balanceText = CurrencyMasker.formatSmallAmount(Math.abs(yourBalance), privacyMode)
                                Text(
                                    text = "$balancePrefix $balanceText",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (yourBalance >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                )
                            }

                            if (expenses.isNotEmpty()) {
                                DonutChart(
                                    expenses = expenses.filter { it.category != "Settlement" },
                                    modifier = Modifier.weight(1f),
                                    chartSize = 100.dp,
                                    strokeWidth = 14.dp,
                                    selectedCategory = selectedChartCategory,
                                    onCategoryClick = { selectedChartCategory = it }
                                )
                            }
                        }
                    }
                }

                if (selectedChartCategory != null) {
                    item {
                        val style = CategoryStyling.getStyle(selectedChartCategory!!)
                        InputChip(
                            selected = true,
                            onClick = { selectedChartCategory = null },
                            label = { Text("Showing $selectedChartCategory only") },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = style.color,
                                selectedLabelColor = style.boldColor
                            )
                        )
                    }
                }

                if (memberBalances.isNotEmpty() && selectedChartCategory == null) {
                    item {
                        Text(
                            text = "Member Balances",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    items(memberBalances) { balance ->
                        MemberBalanceItem(balance, group, context, privacyMode)
                    }
                }

                item {
                    Text(
                        text = if (selectedChartCategory != null) "$selectedChartCategory Transactions" else "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(displayExpenses) { expense ->
                    GroupTransactionItem(
                        expense = expense,
                        onClick = { onExpenseClick(expense.id) },
                        isPrivacyMode = privacyMode
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }
}

@Composable
fun MemberBalanceItem(
    balance: MemberBalance, 
    group: Group?, 
    context: android.content.Context,
    isPrivacyMode: Boolean = false
) {
    val isOwed = balance.amount > 0
    val isSettled = Math.abs(balance.amount) < 0.01
    
    val avatarColor = remember(balance.name) {
        val colors = listOf(Color(0xFFBBDEFB), Color(0xFFFFCDD2), Color(0xFFC8E6C9), Color(0xFFFFF9C4), Color(0xFFD1C4E9), Color(0xFFFFE0B2))
        colors[Math.abs(balance.name.hashCode()) % colors.size]
    }
    val textColor = remember(avatarColor) {
        if (avatarColor == Color(0xFFBBDEFB)) Color(0xFF0D47A1) else Color(0xFF212121)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = avatarColor,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = balance.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = textColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = balance.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (!balance.isSynced) {
                    Text(
                        text = "Invite pending",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            group?.let { g ->
                                val myName = OnboardingUtils.getUserName(context)
                                val inviteLink = "https://autosplit-fdf12.web.app/join?id=${g.remoteId}&key=${Uri.encode(g.syncKey)}&name=${Uri.encode(g.name)}&user=${Uri.encode(myName)}&invitee=${Uri.encode(balance.name)}"
                                
                                val message = "Hey ${balance.name}! Join my group '${g.name}' on Cleave to track expenses together.\n\n" +
                                              "Click to join: $inviteLink"

                                val uri = if (balance.phone != null) {
                                    Uri.parse("https://api.whatsapp.com/send?phone=${balance.phone}&text=${Uri.encode(message)}")
                                } else {
                                    null
                                }
                                
                                if (uri != null) {
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                } else {
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, message)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Resend Invite"))
                                }
                            }
                        }
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                if (isSettled) {
                    Text(text = "Settled Up", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        text = if (isOwed) "is owed" else "owes group",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    val amountText = CurrencyMasker.formatSmallAmount(Math.abs(balance.amount), isPrivacyMode)
                    Text(
                        text = amountText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isOwed) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                    )
                }
            }
        }
    }
}

@Composable
fun GroupTransactionItem(
    expense: Expense, 
    onClick: () -> Unit,
    isPrivacyMode: Boolean = false
) {
    val isSettlement = expense.category == "Settlement"
    val style = remember(expense.category) { CategoryStyling.getStyle(expense.category) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSettlement) Color(0xFFE8F5E9) else style.color,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSettlement) Icons.Default.Savings else style.icon,
                        contentDescription = null,
                        tint = if (isSettlement) Color(0xFF2E7D32) else style.boldColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = expense.merchant,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (expense.isSynced) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.CloudDone, 
                            contentDescription = "Synced", 
                            tint = Color(0xFF4CAF50), // Green for success
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Sync, 
                            contentDescription = "Pending Sync", 
                            tint = Color.Gray.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = if (isSettlement) "Recorded Payment" else "Paid by ${expense.paidBy}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val amountText = CurrencyMasker.formatSmallAmount(expense.amount, isPrivacyMode)
                Text(
                    text = (if (isSettlement) "+ " else "") + amountText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black,
                    color = if (isSettlement) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(expense.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}
