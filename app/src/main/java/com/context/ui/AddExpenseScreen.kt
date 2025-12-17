package com.context.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.context.data.Expense
import com.context.data.ExpenseDatabase
import com.context.data.Group
import com.context.utils.CategoryEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onBack: () -> Unit,
    onExpenseAdded: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { ExpenseDatabase.getDatabase(context) }

    // -- STATE --
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    // Group Selection State
    val groups by db.expenseDao().getAllGroups().collectAsState(initial = emptyList())
    var selectedGroup by remember { mutableStateOf<Group?>(null) } // null = Personal
    var isGroupDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Expense") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // 1. HUGE AMOUNT INPUT
            Text("Enter Amount", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                textStyle = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                ),
                prefix = { Text("₹", style = MaterialTheme.typography.displayMedium) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. DESCRIPTION INPUT
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("What is this for?") },
                placeholder = { Text("e.g. Dinner, Taxi") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. GROUP SELECTOR (The "Smart" Split)
            Text("Split with Group?", style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isGroupDropdownExpanded = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedGroup?.name ?: "Personal Expense (No Split)",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text("▼", fontSize = 12.sp)
                    }
                }

                DropdownMenu(
                    expanded = isGroupDropdownExpanded,
                    onDismissRequest = { isGroupDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    // Option 1: Personal
                    DropdownMenuItem(
                        text = { Text("Personal Expense (No Split)") },
                        onClick = {
                            selectedGroup = null
                            isGroupDropdownExpanded = false
                        }
                    )
                    Divider()
                    // Option 2: List of Groups
                    groups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.name) },
                            onClick = {
                                selectedGroup = group
                                isGroupDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f)) // Push button to bottom

            // 4. SAVE BUTTON
            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull()
                    if (amountVal != null && amountVal > 0) {
                        scope.launch {
                            val category = CategoryEngine.predictCategory(description)
                            
                            val newExpense = Expense(
                                merchant = description.ifBlank { category.label },
                                amount = amountVal,
                                timestamp = System.currentTimeMillis(),
                                category = category.label,
                                groupId = selectedGroup?.groupId,
                                paidBy = "You",
                                isAuto = false
                            )
                            
                            db.expenseDao().insert(newExpense)

                            selectedGroup?.groupId?.let {
                                db.expenseDao().recalculateGroupTotal(it)
                            }

                            Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                            onExpenseAdded()
                        }
                    } else {
                        Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Expense", fontSize = 18.sp)
            }
        }
    }
}