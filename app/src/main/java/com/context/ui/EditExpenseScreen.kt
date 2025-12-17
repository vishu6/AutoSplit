package com.context.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.context.data.Expense
import com.context.data.ExpenseDatabase
import com.context.data.Group
import com.context.utils.CategoryEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseScreen(
    expenseId: Int,
    onBack: () -> Unit,
    onExpenseUpdated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { ExpenseDatabase.getDatabase(context) }

    // State
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var existingExpense by remember { mutableStateOf<Expense?>(null) }
    
    // Group Selector State
    val groups by db.expenseDao().getAllGroups().collectAsState(initial = emptyList())
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var isGroupDropdownExpanded by remember { mutableStateOf(false) }

    // LOAD DATA
    LaunchedEffect(expenseId) {
        val exp = db.expenseDao().getExpenseById(expenseId)
        if (exp != null) {
            existingExpense = exp
            amount = exp.amount.toString().replace(".0", "") // Clean format
            description = exp.merchant
        }
    }

    // Sync selected group once groups are loaded
    LaunchedEffect(groups, existingExpense) {
        if (existingExpense?.groupId != null && groups.isNotEmpty()) {
            selectedGroup = groups.find { it.groupId == existingExpense?.groupId }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Expense") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // DELETE BUTTON
                    IconButton(onClick = {
                        scope.launch {
                            existingExpense?.let { expenseToDelete ->
                                val groupId = expenseToDelete.groupId
                                db.expenseDao().delete(expenseToDelete)
                                // After deleting, recalculate the total for the affected group
                                groupId?.let { db.expenseDao().recalculateGroupTotal(it) }
                            }
                            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                            onExpenseUpdated()
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            
            // AMOUNT
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                textStyle = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("₹") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // DESCRIPTION
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // GROUP SELECTOR
            Box {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().clickable { isGroupDropdownExpanded = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedGroup?.name ?: "Personal Expense")
                        Spacer(modifier = Modifier.weight(1f))
                        Text("▼")
                    }
                }
                DropdownMenu(
                    expanded = isGroupDropdownExpanded,
                    onDismissRequest = { isGroupDropdownExpanded = false }
                ) {
                    DropdownMenuItem(text = { Text("Personal") }, onClick = { selectedGroup = null; isGroupDropdownExpanded = false })
                    groups.forEach { group ->
                        DropdownMenuItem(text = { Text(group.name) }, onClick = { selectedGroup = group; isGroupDropdownExpanded = false })
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // UPDATE BUTTON
            Button(
                onClick = {
                    scope.launch {
                        val amtVal = amount.toDoubleOrNull() ?: 0.0
                        existingExpense?.let { oldExp ->
                            val oldGroupId = oldExp.groupId
                            val updatedExp = oldExp.copy(
                                amount = amtVal,
                                merchant = description,
                                groupId = selectedGroup?.groupId,
                                category = CategoryEngine.predictCategory(description).label
                            )
                            db.expenseDao().update(updatedExp)
                            
                            // Recalculate for the new group
                            updatedExp.groupId?.let { db.expenseDao().recalculateGroupTotal(it) }
                            // If the group changed, also recalculate for the old group
                            if (oldGroupId != updatedExp.groupId) {
                                oldGroupId?.let { db.expenseDao().recalculateGroupTotal(it) }
                            }

                            onExpenseUpdated()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Update Expense")
            }
        }
    }
}