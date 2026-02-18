package com.context.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.context.app.R
import com.context.data.ExpenseDatabase
import com.context.utils.OnboardingUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { ExpenseDatabase.getDatabase(context) }
    val allExpenses by db.expenseDao().getAllExpenses().collectAsState(initial = emptyList())

    var userName by remember { mutableStateOf("") }
    val originalUserName = remember { OnboardingUtils.getUserName(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        userName = originalUserName
    }

    val isNameChanged = userName != originalUserName

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
//                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            painter = painterResource(id = R.drawable.ic_launcher_round),
                            contentDescription = "App Logo",
                            modifier = Modifier.fillMaxSize(0.9f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Split Mate",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version 1.0 (Beta)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            Text(
                "Display Name",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top=8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("What should we call you?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "This name is only stored on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val finalName = if (userName.isNotBlank()) userName.trim() else "Mate"
                        OnboardingUtils.saveUserName(context, finalName)
                        scope.launch {
                            snackbarHostState.showSnackbar("Name updated!")
                        }
                    },
                    enabled = isNameChanged
                ) {
                    Text("Save")
                }
            }

            Spacer(Modifier.height(16.dp))
            
            Divider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            SettingsItem(
                title = "Backup Data (Export CSV)",
                subtitle = "Share your expense history",
                icon = Icons.Default.Share,
                onClick = {
                    fun String.escapeCsv(): String = "\"" + this.replace("\"", "\"\"") + "\""

                    val csvHeader = listOf("Date", "Merchant", "Amount", "Category", "Group").joinToString(",") { it.escapeCsv() }
                    val csvBody = allExpenses.joinToString("\n") { exp ->
                        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(exp.timestamp))
                        val merchant = exp.merchant
                        val category = exp.category
                        val group = (exp.groupId ?: "Personal").toString()
                        
                        listOf(date, merchant, exp.amount.toString(), category, group).joinToString(",") { it.escapeCsv() }
                    }
                    
                    val csvContent = "$csvHeader\n$csvBody"

                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, csvContent)
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TITLE, "SplitMate_Backup.csv")
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Export Backup"))
                }
            )

            SettingsItem(
                title = "Report a Bug",
                subtitle = "Found an issue? Let us know.",
                icon = Icons.Default.BugReport,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("caresplitmate@gmail.com"))
                        putExtra(Intent.EXTRA_SUBJECT, "Split Mate Bug Report")
                    }
                    try { context.startActivity(intent) } catch (e: Exception) {}
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Made with ❤️ in India",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🇮🇳 Atmanirbhar Bharat Initiative",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { 
            Icon(
                imageVector = icon, 
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            ) 
        },
        modifier = Modifier.clickable { onClick() }
    )
    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
