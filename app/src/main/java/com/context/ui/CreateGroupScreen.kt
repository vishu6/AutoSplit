package com.context.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.context.data.ExpenseDatabase
import com.context.data.Group
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    onGroupCreated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { ExpenseDatabase.getDatabase(context) }

    var groupName by remember { mutableStateOf("") }
    
    // Member State
    var newMemberName by remember { mutableStateOf("") }
    // Start with "You" by default
    val members = remember { mutableStateListOf("You") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Group") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            
            // 1. GROUP NAME
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name") },
                placeholder = { Text("e.g. Goa Trip") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. ADD MEMBERS SECTION
            Text("Add Members", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newMemberName,
                    onValueChange = { newMemberName = it },
                    label = { Text("Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newMemberName.isNotBlank()) {
                            members.add(newMemberName.trim())
                            newMemberName = ""
                        }
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. MEMBER CHIPS (Horizontal Scroll)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(members) { member ->
                    InputChip(
                        selected = true,
                        onClick = { 
                            if (member != "You") members.remove(member) 
                        },
                        label = { Text(member) },
                        trailingIcon = {
                            if (member != "You") {
                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 4. CREATE BUTTON
            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        scope.launch {
                            // Join the list into a single string: "You,Rahul,Priya"
                            val membersString = members.joinToString(",")
                            
                            db.expenseDao().insertGroup(
                                Group(name = groupName, members = membersString)
                            )
                            onGroupCreated()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Create Group with ${members.size} Members")
            }
        }
    }
}