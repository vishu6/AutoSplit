package com.context.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.context.utils.BudgetUtils
import com.context.utils.CategoryUtils
import com.context.utils.HapticUtils
import com.context.utils.LocalToaster

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSetupScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val toaster = LocalToaster.current
    val scrollState = rememberScrollState()

    var monthlyBudget by remember { mutableStateOf(BudgetUtils.getMonthlyBudget(context).let { if (it > 0) it.toString() else "" }) }
    val categories = remember { CategoryUtils.categories }
    val initialCategoryLimits = remember { BudgetUtils.getCategoryLimits(context) }
    val categoryLimits = remember { 
        mutableStateMapOf<String, String>().apply {
            categories.forEach { cat ->
                this[cat] = initialCategoryLimits[cat]?.let { if (it > 0) it.toString() else "" } ?: ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monthly Budget", fontWeight = FontWeight.Bold) },
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
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "How much do you want to spend this month?",
                style = MaterialTheme.typography.titleLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = monthlyBudget,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) monthlyBudget = it },
                textStyle = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("₹", style = MaterialTheme.typography.displayMedium) },
                placeholder = { Text("0", style = MaterialTheme.typography.displayMedium.copy(color = Color.LightGray)) },
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                "Category Limits (optional)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            categories.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(category, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    OutlinedTextField(
                        value = categoryLimits[category] ?: "",
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) categoryLimits[category] = it },
                        placeholder = { Text("₹ 0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(120.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    val budgetVal = monthlyBudget.toDoubleOrNull() ?: 0.0
                    BudgetUtils.setMonthlyBudget(context, budgetVal)
                    
                    val limitsMap = categoryLimits.mapValues { it.value.toDoubleOrNull() ?: 0.0 }
                    BudgetUtils.setCategoryLimits(context, limitsMap)
                    
                    HapticUtils.playDoubleTick(context)
                    toaster.show("Budget saved successfully!")
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Budget", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "You can change this anytime in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
