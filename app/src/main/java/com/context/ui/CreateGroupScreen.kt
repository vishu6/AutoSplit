package com.context.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.context.data.ExpenseDatabase
import com.context.data.Group
import com.context.data.GroupMember
import com.context.utils.HapticUtils
import com.context.utils.GroupCryptoUtils
import com.context.utils.OnboardingUtils
import kotlinx.coroutines.launch
import java.util.UUID

data class Member(val name: String, val phone: String? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    onGroupCreated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { ExpenseDatabase.getDatabase(context) }
    val myName = remember { OnboardingUtils.getUserName(context) }

    var groupName by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }
    var newMemberName by remember { mutableStateOf("") }
    
    val members = remember { mutableStateListOf(Member(myName)) }
    
    var showSuccessSheet by remember { mutableStateOf(false) }
    var createdGroup by remember { mutableStateOf<Group?>(null) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        contactUri?.let { uri ->
            var name: String? = null
            var phone: String? = null
            
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                    name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                    
                    val hasPhoneNumber = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER))
                    if (hasPhoneNumber > 0) {
                        context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(id),
                            null
                        )?.use { phoneCursor ->
                            if (phoneCursor.moveToFirst()) {
                                phone = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                            }
                        }
                    }
                }
            }
            
            if (name != null) {
                val newMember = Member(name!!, phone)
                if (!members.any { it.name.equals(name, ignoreCase = true) }) {
                    members.add(newMember)
                    HapticUtils.playTick(context)
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) contactPickerLauncher.launch(null)
        else Toast.makeText(context, "Permission needed for contacts", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Group", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name") },
                placeholder = { Text("e.g. Goa Trip 2024") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text("Who's in the group?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                onClick = {
                    when (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)) {
                        PackageManager.PERMISSION_GRANTED -> contactPickerLauncher.launch(null)
                        else -> permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PersonAdd, null, tint = Color.White)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Add from Contacts", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Pick friends from your phonebook", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (members.size > 0) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(members) { member ->
                        InputChip(
                            selected = true,
                            onClick = { if (member.name != myName) members.remove(member) },
                            label = { Text(if (member.name == myName) "You" else member.name) },
                            trailingIcon = {
                                if (member.name != myName) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!showManualInput) {
                TextButton(onClick = { showManualInput = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add member manually")
                }
            }

            AnimatedVisibility(visible = showManualInput) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newMemberName,
                        onValueChange = { newMemberName = it },
                        label = { Text("Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        if (newMemberName.isNotBlank()) {
                            if (!members.any { it.name.equals(newMemberName.trim(), ignoreCase = true) }) {
                                members.add(Member(newMemberName.trim()))
                                newMemberName = ""
                                HapticUtils.playTick(context)
                            } else {
                                Toast.makeText(context, "Member already added", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (groupName.isNotBlank() && members.size > 1) {
                        scope.launch {
                            HapticUtils.playDoubleTick(context)
                            val syncKey = GroupCryptoUtils.generateSecretKey()
                            val remoteId = UUID.randomUUID().toString()
                            
                            val uniqueNames = members.map { it.name }.distinctBy { it.lowercase() }
                            val membersString = uniqueNames.joinToString(",")
                            
                            val group = Group(
                                name = groupName.trim(), 
                                members = membersString,
                                remoteId = remoteId,
                                syncKey = syncKey,
                                isSyncEnabled = true
                            )
                            val groupId = db.expenseDao().insertGroup(group).toInt()
                            
                            val groupMembers = members.filter { m -> uniqueNames.contains(m.name) }.map { 
                                GroupMember(groupId = groupId, name = it.name, phone = it.phone)
                            }
                            db.expenseDao().insertMembers(groupMembers)
                            
                            createdGroup = db.expenseDao().getGroupByRemoteId(remoteId)
                            showSuccessSheet = true
                        }
                    } else if (members.size <= 1) {
                        Toast.makeText(context, "Add at least one more member", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = groupName.isNotBlank()
            ) {
                Icon(Icons.Default.GroupAdd, null)
                Spacer(Modifier.width(12.dp))
                Text("Create Group with ${members.size} Members", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showSuccessSheet && createdGroup != null) {
        ModalBottomSheet(
            onDismissRequest = { 
                showSuccessSheet = false
                onGroupCreated() 
            },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Group Created! 🎉", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text("Invite members to start syncing expenses.", color = Color.Gray, textAlign = TextAlign.Center)

                Spacer(Modifier.height(24.dp))

                val invitees = members.filter { it.name != myName }

                if (invitees.isNotEmpty()) {
                    Text("SEND INVITES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    
                    invitees.forEach { member ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(member.name, fontWeight = FontWeight.Bold)
                                    if (member.phone != null) {
                                        Text(member.phone, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                                Button(
                                    onClick = {
                                        HapticUtils.playTick(context)
                                        val g = createdGroup!!
                                        // BRANDED LINK: Using cleaveapp.in
                                        val inviteLink = "https://cleaveapp.in/join?id=${g.remoteId}&key=${Uri.encode(g.syncKey)}&name=${Uri.encode(g.name)}&user=${Uri.encode(myName)}&invitee=${Uri.encode(member.name)}"
                                        
                                        val message = "Hey ${member.name}! Join my group '${g.name}' on Cleave to track expenses together.\n\n" +
                                                      "Click to join: $inviteLink"
                                        
                                        if (member.phone != null) {
                                            val uri = Uri.parse("https://api.whatsapp.com/send?phone=${member.phone}&text=${Uri.encode(message)}")
                                            val intent = Intent(Intent.ACTION_VIEW, uri)
                                            context.startActivity(intent)
                                        } else {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, message)
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Invite ${member.name}"))
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Invite")
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                
                TextButton(onClick = { 
                    showSuccessSheet = false
                    onGroupCreated() 
                }) {
                    Text("Done")
                }
            }
        }
    }
}
