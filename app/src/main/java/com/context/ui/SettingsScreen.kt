package com.context.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Palette
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
import androidx.core.content.FileProvider
import com.context.app.R
import com.context.data.Expense
import com.context.data.ExpenseDatabase
import com.context.utils.OnboardingUtils
import com.context.utils.SecurityUtils
import com.context.utils.BiometricUtils
import com.context.utils.ThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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
    
    var isSecurityEnabled by remember { mutableStateOf(SecurityUtils.isSecurityEnabled(context)) }
    val canUseBiometrics = remember { BiometricUtils.canAuthenticate(context) }
    
    val currentThemeMode by ThemeUtils.getThemeModeFlow(context).collectAsState(initial = ThemeUtils.THEME_SYSTEM)
    var showThemeDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // IMPORT LAUNCHER
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                val (success, count) = importCsvFromUri(context, it, db)
                if (success) {
                    if (count > 0) {
                        snackbarHostState.showSnackbar("Successfully imported $count new transactions!")
                    } else {
                        snackbarHostState.showSnackbar("No new transactions found. All were already present.")
                    }
                } else {
                    snackbarHostState.showSnackbar("Error importing data. Check file format.")
                }
            }
        }
    }

    // EXPORT (SAVE TO DEVICE) LAUNCHER
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            scope.launch {
                val success = saveCsvToUri(context, it, allExpenses)
                if (success) {
                    snackbarHostState.showSnackbar("Backup saved to device!")
                } else {
                    snackbarHostState.showSnackbar("Failed to save backup.")
                }
            }
        }
    }

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
                .fillMaxSize()
                .verticalScroll(scrollState),
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
                        text = "Version 1.0.3 (Beta)",
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
                title = "App Theme",
                subtitle = when(currentThemeMode) {
                    ThemeUtils.THEME_LIGHT -> "Light Mode"
                    ThemeUtils.THEME_DARK -> "Dark Mode"
                    else -> "System Default"
                },
                icon = Icons.Default.Palette,
                onClick = { showThemeDialog = true }
            )

            if (canUseBiometrics) {
                SecurityToggleItem(
                    title = "App Lock",
                    subtitle = "Unlock with fingerprint or PIN",
                    icon = Icons.Default.Fingerprint,
                    isEnabled = isSecurityEnabled,
                    onToggle = { enabled ->
                        isSecurityEnabled = enabled
                        SecurityUtils.setSecurityEnabled(context, enabled)
                    }
                )
            }

            SettingsItem(
                title = "Save Backup to Device",
                subtitle = "Save your data as a .csv file on this phone",
                icon = Icons.Default.FileDownload,
                onClick = {
                    val date = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                    exportLauncher.launch("SplitMate_Backup_$date.csv")
                }
            )

            SettingsItem(
                title = "Share Backup (CSV)",
                subtitle = "Send your expense history to other apps",
                icon = Icons.Default.Share,
                onClick = {
                    scope.launch {
                        val csvFile = generateCsvFile(context, allExpenses)
                        if (csvFile != null) {
                            val uri = FileProvider.getUriForFile(context, "com.context.app.fileprovider", csvFile)
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_STREAM, uri)
                                type = "text/csv"
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export Backup"))
                        }
                    }
                }
            )

            SettingsItem(
                title = "Import Data (From CSV)",
                subtitle = "Restore expenses from a backup file",
                icon = Icons.Default.FileUpload,
                onClick = {
                    importLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "application/csv"))
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

            SettingsItem(
                title = "Privacy Policy",
                subtitle = "View our data handling practices",
                icon = Icons.Default.Info,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/split-mate-privacy-policy/home"))
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

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

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select Theme") },
            text = {
                Column {
                    ThemeOption("System Default", currentThemeMode == ThemeUtils.THEME_SYSTEM) {
                        ThemeUtils.saveThemeMode(context, ThemeUtils.THEME_SYSTEM)
                        showThemeDialog = false
                    }
                    ThemeOption("Light", currentThemeMode == ThemeUtils.THEME_LIGHT) {
                        ThemeUtils.saveThemeMode(context, ThemeUtils.THEME_LIGHT)
                        showThemeDialog = false
                    }
                    ThemeOption("Dark", currentThemeMode == ThemeUtils.THEME_DARK) {
                        ThemeUtils.saveThemeMode(context, ThemeUtils.THEME_DARK)
                        showThemeDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun ThemeOption(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

private suspend fun saveCsvToUri(context: Context, uri: Uri, expenses: List<Expense>): Boolean = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { fos ->
            fun String.escapeCsv(): String = "\"" + this.replace("\"", "\"\"") + "\""
            
            // Header: Date,Time,Merchant,Amount,Category,Group
            val header = "Date,Time,Merchant,Amount,Category,Group\n"
            fos.write(header.toByteArray())
            
            expenses.forEach { exp ->
                val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(exp.timestamp))
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(exp.timestamp))
                val line = "${date.escapeCsv()},${time.escapeCsv()},${exp.merchant.escapeCsv()},${exp.amount},${exp.category.escapeCsv()},${(exp.groupId ?: "Personal").toString().escapeCsv()}\n"
                fos.write(line.toByteArray())
            }
            true
        } ?: false
    } catch (e: Exception) {
        false
    }
}

private suspend fun generateCsvFile(context: Context, expenses: List<Expense>): File? = withContext(Dispatchers.IO) {
    try {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        
        val file = File(exportDir, "SplitMate_Backup.csv")
        val fos = FileOutputStream(file)
        
        fun String.escapeCsv(): String = "\"" + this.replace("\"", "\"\"") + "\""
        
        val header = "Date,Time,Merchant,Amount,Category,Group\n"
        fos.write(header.toByteArray())
        
        expenses.forEach { exp ->
            val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(exp.timestamp))
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(exp.timestamp))
            val line = "${date.escapeCsv()},${time.escapeCsv()},${exp.merchant.escapeCsv()},${exp.amount},${exp.category.escapeCsv()},${(exp.groupId ?: "Personal").toString().escapeCsv()}\n"
            fos.write(line.toByteArray())
        }
        
        fos.close()
        file
    } catch (e: Exception) {
        null
    }
}

private suspend fun importCsvFromUri(context: Context, uri: Uri, db: ExpenseDatabase): Pair<Boolean, Int> = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val reader = inputStream?.bufferedReader()
        val lines = reader?.readLines() ?: emptyList()
        
        if (lines.isEmpty()) return@withContext Pair(false, 0)
        
        // IDENTIFY FORMAT BY HEADER
        val header = lines.firstOrNull()?.lowercase() ?: ""
        val isNewFormat = header.contains("time")
        
        val newExpenses = mutableListOf<Expense>()
        val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val dateFormatOnly = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        var importedCount = 0
        
        // Skip header
        lines.drop(1).forEach { line ->
            val parts = line.split(",").map { it.trim().removeSurrounding("\"") }
            
            if (isNewFormat && parts.size >= 4) {
                // Header: Date,Time,Merchant,Amount,Category,Group
                val dateStr = parts[0]
                val timeStr = parts[1]
                val merchant = parts[2]
                val amount = parts[3].toDoubleOrNull() ?: 0.0
                val category = if (parts.size > 4) parts[4] else "Other"
                
                val timestamp = try { 
                    dateTimeFormat.parse("$dateStr $timeStr")?.time ?: dateFormatOnly.parse(dateStr)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) { 
                    try { dateFormatOnly.parse(dateStr)?.time ?: System.currentTimeMillis() } catch(e: Exception) { System.currentTimeMillis() }
                }
                
                // DE-DUPLICATION CHECK
                val isDuplicate = db.expenseDao().checkDuplicateStrict(merchant, amount, timestamp - 1000, timestamp + 1000) > 0
                
                if (!isDuplicate) {
                    newExpenses.add(
                        Expense(
                            merchant = merchant,
                            amount = amount,
                            timestamp = timestamp,
                            category = category,
                            groupId = null,
                            isAuto = false
                        )
                    )
                    importedCount++
                }
            } else if (!isNewFormat && parts.size >= 3) {
                // Old Header: Date,Merchant,Amount,Category,Group
                val dateStr = parts[0]
                val merchant = parts[1]
                val amount = parts[2].toDoubleOrNull() ?: 0.0
                val category = if (parts.size > 3) parts[3] else "Other"
                
                val timestamp = try { dateFormatOnly.parse(dateStr)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
                
                // For old format, we use a wider window (5 mins) since we don't have exact time
                val isDuplicate = db.expenseDao().checkDuplicateStrict(merchant, amount, timestamp - 300000, timestamp + 300000) > 0
                if (!isDuplicate) {
                    newExpenses.add(
                        Expense(
                            merchant = merchant,
                            amount = amount,
                            timestamp = timestamp,
                            category = category,
                            groupId = null,
                            isAuto = false
                        )
                    )
                    importedCount++
                }
            }
        }
        
        if (newExpenses.isNotEmpty()) {
            db.expenseDao().insertAll(newExpenses)
        }
        Pair(true, importedCount)
    } catch (e: Exception) {
        Pair(false, 0)
    }
}

@Composable
fun SecurityToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
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
        trailingContent = {
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        },
        modifier = Modifier.clickable { onToggle(!isEnabled) }
    )
    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
