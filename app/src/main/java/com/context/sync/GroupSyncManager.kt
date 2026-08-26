package com.context.sync

import com.context.data.Expense
import com.context.data.ExpenseDao
import com.context.data.Group
import com.context.data.GroupMember
import com.context.utils.GroupCryptoUtils
import com.context.utils.OnboardingUtils
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.Keep
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupSyncManager @Inject constructor(
    private val expenseDao: ExpenseDao,
    @ApplicationContext private val context: Context
) {
    private val database = FirebaseDatabase.getInstance().reference
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val activeListeners = mutableMapOf<String, ValueEventListener>()
    private val identityListeners = mutableMapOf<String, ValueEventListener>()

    private val _joinEvents = Channel<JoinResult>(Channel.BUFFERED)
    val joinEvents = _joinEvents.receiveAsFlow()

    private val invitationHints = mutableMapOf<String, String>()
    private val pendingPushes = mutableSetOf<Int>()

    @Keep
    data class JoinResult(
        val groupId: Int = 0,
        val groupName: String = "",
        val isNewJoin: Boolean = false,
        val candidateName: String? = null,
        val remoteId: String? = null,
        val syncKey: String? = null,
        val inviterName: String? = null
    )

    @Keep
    data class EncryptedPayload(
        val remoteId: String = "",
        val encryptedData: String = "",
        val sender: String = ""
    ) {
        constructor() : this("", "", "")
    }

    @Keep
    data class IdentityPayload(
        val name: String = "",
        val upiId: String? = null,
        val invitedAs: String? = null,
        val isActive: Boolean = true
    ) {
        constructor() : this("", null, null, true)
    }

    fun joinByUrl(url: String, onComplete: (String) -> Unit, onError: (String) -> Unit) {
        scope.launch {
            try {
                val uri = Uri.parse(url.trim())
                val remoteId = uri.getQueryParameter("id")
                val syncKey = uri.getQueryParameter("key")
                val name = uri.getQueryParameter("name")
                val inviterName = uri.getQueryParameter("user")
                val invitedAs = uri.getQueryParameter("invitee")

                if (remoteId != null && syncKey != null && name != null) {
                    val existing = expenseDao.getGroupByRemoteId(remoteId)
                    val myName = OnboardingUtils.getUserName(context)

                    if (existing != null && existing.getMemberList().any { it.equals(myName, ignoreCase = true) }) {
                        _joinEvents.send(JoinResult(existing.groupId, name, false))
                        withContext(Dispatchers.Main) { onComplete(name) }
                        return@launch
                    }

                    var finalCandidate = invitedAs
                    try {
                        val groupSnapshot = database.child("groups").child(remoteId).child("metadata").get().await()
                        val encryptedMeta = groupSnapshot.getValue(String::class.java)
                        if (encryptedMeta != null) {
                            val decryptedJson = GroupCryptoUtils.decrypt(encryptedMeta, syncKey)
                            val remoteMembers = decryptedJson.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            
                            if (finalCandidate == null) {
                                finalCandidate = remoteMembers.find { 
                                    it.lowercase() != "you" && 
                                    it.lowercase() != inviterName?.lowercase() &&
                                    it.lowercase() != myName.lowercase()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Sync", "Metadata fetch failed: ${e.message}")
                    }

                    _joinEvents.send(JoinResult(
                        groupId = existing?.groupId ?: 0,
                        groupName = name,
                        isNewJoin = existing == null,
                        candidateName = finalCandidate,
                        remoteId = remoteId,
                        syncKey = syncKey,
                        inviterName = inviterName
                    ))
                    
                    withContext(Dispatchers.Main) {
                        onComplete(name)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError("Invalid invite link format.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Could not parse link.")
                }
            }
        }
    }

    fun confirmJoin(result: JoinResult, shouldMerge: Boolean) {
        scope.launch {
            val remoteId = result.remoteId ?: return@launch
            val syncKey = result.syncKey ?: return@launch
            val groupName = result.groupName
            val inviterName = result.inviterName
            val candidateName = result.candidateName

            val existing = expenseDao.getGroupByRemoteId(remoteId)
            val isNewJoin = existing == null
            val myName = OnboardingUtils.getUserName(context)
            
            var remoteMemberList: List<String>? = null
            try {
                val groupSnapshot = database.child("groups").child(remoteId).child("metadata").get().await()
                val encryptedMeta = groupSnapshot.getValue(String::class.java)
                if (encryptedMeta != null) {
                    remoteMemberList = GroupCryptoUtils.decrypt(encryptedMeta, syncKey).split(",").map { it.trim() }.filter { it.isNotBlank() }
                }
            } catch (e: Exception) {}

            val groupId = if (isNewJoin) {
                val membersSet = mutableSetOf<String>()
                membersSet.add(myName)
                if (inviterName != null) membersSet.add(inviterName)
                remoteMemberList?.forEach { membersSet.add(it) }

                if (shouldMerge && candidateName != null) {
                    membersSet.removeIf { it.equals(candidateName, ignoreCase = true) }
                    invitationHints[remoteId] = candidateName
                }

                val newGroup = Group(
                    name = groupName,
                    members = membersSet.joinToString(","),
                    remoteId = remoteId,
                    syncKey = syncKey,
                    isSyncEnabled = true
                )
                val id = expenseDao.insertGroup(newGroup).toInt()
                
                expenseDao.insertMember(GroupMember(
                    groupId = id,
                    name = myName,
                    upiId = OnboardingUtils.getUpiId(context),
                    lastSynced = System.currentTimeMillis()
                ))

                val groupFromDb = expenseDao.getGroup(id)
                groupFromDb?.let { 
                    pushGroupMetadata(it)
                    startSync(it) 
                }
                id
            } else {
                val id = existing!!.groupId
                
                if (shouldMerge && candidateName != null) {
                    expenseDao.renameMemberInGroup(id, candidateName, myName)
                    val members = existing.getMemberList().toMutableList()
                    members.removeIf { it.equals(candidateName, ignoreCase = true) }
                    if (!members.any { it.equals(myName, ignoreCase = true) }) members.add(myName)
                    
                    val updatedGroup = existing.copy(members = members.joinToString(","))
                    expenseDao.updateGroup(updatedGroup)
                    pushGroupMetadata(updatedGroup)
                    invitationHints[remoteId] = candidateName
                } else {
                    val members = existing.getMemberList().toMutableList()
                    if (!members.any { it.equals(myName, ignoreCase = true) }) {
                        members.add(myName)
                        val updatedGroup = existing.copy(members = members.joinToString(","))
                        expenseDao.updateGroup(updatedGroup)
                        pushGroupMetadata(updatedGroup)
                    }
                }
                
                expenseDao.getGroup(id)?.let { startSync(it) }
                id
            }
            
            _joinEvents.send(JoinResult(groupId, groupName, isNewJoin))
        }
    }

    fun pushGroupMetadata(group: Group) {
        if (!group.isSyncEnabled || group.syncKey == null) return
        scope.launch {
            try {
                val encrypted = GroupCryptoUtils.encrypt(group.members, group.syncKey)
                database.child("groups").child(group.remoteId).child("metadata").setValue(encrypted)
            } catch (e: Exception) {
                Log.e("Sync", "Push Group Metadata failed: ${e.message}")
            }
        }
    }

    fun broadcastIdentity(group: Group) {
        if (!group.isSyncEnabled || group.syncKey == null) return

        scope.launch {
            val userName = OnboardingUtils.getUserName(context)
            val invitedAsHint = invitationHints[group.remoteId]
            
            val localMembers = expenseDao.getMembersForGroup(group.groupId).first()
            val myStatus = localMembers.find { it.name.equals(userName, ignoreCase = true) }?.isActive ?: true
            
            val identity = IdentityPayload(
                name = userName,
                upiId = OnboardingUtils.getUpiId(context),
                invitedAs = invitedAsHint,
                isActive = myStatus
            )
            
            val json = gson.toJson(identity)
            val encrypted = GroupCryptoUtils.encrypt(json, group.syncKey)
            
            val payload = EncryptedPayload(
                remoteId = "me",
                encryptedData = encrypted,
                sender = userName
            )

            val userHash = Math.abs(userName.hashCode()).toString()
            database.child("groups").child(group.remoteId).child("identities")
                .child(userHash)
                .setValue(payload)
        }
    }

    fun startSync(group: Group) {
        if (!group.isSyncEnabled || group.syncKey == null) return
        
        broadcastIdentity(group)
        pushGroupMetadata(group)

        scope.launch {
            val unsynced = expenseDao.getUnsyncedExpenses(group.groupId)
            unsynced.forEach { pushExpense(it) }
        }

        if (!activeListeners.containsKey(group.remoteId)) {
            val expenseListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    scope.launch {
                        snapshot.children.forEach { child ->
                            val payload = child.getValue(EncryptedPayload::class.java)
                            payload?.let { processRemoteExpense(it, group) }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            database.child("groups").child(group.remoteId).child("expenses")
                .addValueEventListener(expenseListener)
            activeListeners[group.remoteId] = expenseListener
        }

        database.child("groups").child(group.remoteId).child("metadata")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val encrypted = snapshot.getValue(String::class.java) ?: return
                    scope.launch {
                        try {
                            val remoteMembersCsv = GroupCryptoUtils.decrypt(encrypted, group.syncKey!!)
                            val localGroup = expenseDao.getGroup(group.groupId) ?: return@launch
                            
                            val localList = localGroup.getMemberList()
                            val remoteList = remoteMembersCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            
                            // Get all current identities to find "Reconciled/Legacy" names
                            val syncedMembers = expenseDao.getMembersForGroup(group.groupId).first()
                            val activeRealNames = syncedMembers.map { it.name.lowercase() }

                            val mergedSet = localList.toMutableSet()
                            var changed = false
                            
                            remoteList.forEach { remoteName ->
                                val normalizedRemote = remoteName.lowercase()
                                // Skip if name was explicitly reconciled (invitation hint) 
                                // OR if we already have a real identity for this person and this is a ghost name
                                val isReconciled = invitationHints[group.remoteId]?.lowercase() == normalizedRemote
                                
                                if (!isReconciled && !mergedSet.any { it.lowercase() == normalizedRemote }) {
                                    mergedSet.add(remoteName)
                                    changed = true
                                }
                            }
                            
                            if (changed) {
                                val updated = localGroup.copy(members = mergedSet.joinToString(","))
                                expenseDao.updateGroup(updated)
                            }
                        } catch (e: Exception) {}
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        if (!identityListeners.containsKey(group.remoteId)) {
            val idListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    scope.launch {
                        snapshot.children.forEach { child ->
                            val payload = child.getValue(EncryptedPayload::class.java)
                            payload?.let { processRemoteIdentity(it, group) }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            database.child("groups").child(group.remoteId).child("identities")
                .addValueEventListener(idListener)
            identityListeners[group.remoteId] = idListener
        }
    }

    fun pushExpense(expense: Expense) {
        if (expense.groupId == null || expense.isSynced) return
        
        synchronized(pendingPushes) {
            if (pendingPushes.contains(expense.id)) return
            pendingPushes.add(expense.id)
        }

        scope.launch {
            try {
                val group = expenseDao.getGroup(expense.groupId)
                if (group?.isSyncEnabled == true && group.syncKey != null) {
                    val myName = OnboardingUtils.getUserName(context)
                    
                    val remoteId = if (expense.remoteId.isBlank() || expense.remoteId.contains("-")) UUID.randomUUID().toString() else expense.remoteId
                    val lockedExpense = expense.copy(isSynced = true, remoteId = remoteId)
                    expenseDao.update(lockedExpense)

                    val displayPayer = if (lockedExpense.paidBy == "You") myName else lockedExpense.paidBy
                    val displayMerchant = if (lockedExpense.category == "Settlement") {
                        lockedExpense.merchant.replace("You", myName)
                    } else {
                        lockedExpense.merchant
                    }

                    val dataMap = mapOf(
                        "merchant" to displayMerchant,
                        "amount" to lockedExpense.amount.toString(),
                        "timestamp" to lockedExpense.timestamp.toString(),
                        "category" to lockedExpense.category,
                        "paidBy" to displayPayer
                    )
                    
                    val json = gson.toJson(dataMap)
                    val encrypted = GroupCryptoUtils.encrypt(json, group.syncKey)
                    
                    val payload = EncryptedPayload(
                        remoteId = remoteId,
                        encryptedData = encrypted,
                        sender = myName
                    )

                    database.child("groups").child(group.remoteId).child("expenses")
                        .child(payload.remoteId)
                        .setValue(payload)
                        .addOnSuccessListener {
                            synchronized(pendingPushes) { pendingPushes.remove(expense.id) }
                        }
                        .addOnFailureListener {
                            scope.launch {
                                expenseDao.update(expense.copy(isSynced = false))
                                synchronized(pendingPushes) { pendingPushes.remove(expense.id) }
                            }
                        }
                } else {
                    synchronized(pendingPushes) { pendingPushes.remove(expense.id) }
                }
            } catch (e: Exception) {
                synchronized(pendingPushes) { pendingPushes.remove(expense.id) }
            }
        }
    }

    fun deleteRemoteExpense(expense: Expense, groupId: Int? = null) {
        val targetGroupId = groupId ?: expense.groupId ?: return
        if (expense.remoteId.isBlank()) return

        scope.launch {
            val group = expenseDao.getGroup(targetGroupId)
            if (group?.isSyncEnabled == true) {
                database.child("groups").child(group.remoteId).child("expenses")
                    .child(expense.remoteId)
                    .removeValue()
            }
        }
    }

    private suspend fun processRemoteExpense(payload: EncryptedPayload, group: Group) {
        val existing = expenseDao.getExpenseByRemoteId(payload.remoteId)
        if (existing != null) return 

        try {
            val decryptedJson = GroupCryptoUtils.decrypt(payload.encryptedData, group.syncKey!!)
            val dataMap = gson.fromJson(decryptedJson, Map::class.java)
            
            val myName = OnboardingUtils.getUserName(context)
            val remotePayer = dataMap["paidBy"] as? String ?: "Unknown"
            val remoteMerchant = dataMap["merchant"] as? String ?: "Unknown"
            val category = dataMap["category"] as? String ?: "General"
            
            val localPayer = if (remotePayer.equals(myName, ignoreCase = true)) "You" else remotePayer
            val localMerchant = if (category == "Settlement") {
                remoteMerchant.replace(myName, "You", ignoreCase = true)
            } else {
                remoteMerchant
            }

            val amount = (dataMap["amount"] as? String)?.toDouble() ?: 0.0
            val timestamp = (dataMap["timestamp"] as? String)?.toLong() ?: System.currentTimeMillis()

            val remoteExpense = Expense(
                merchant = localMerchant,
                amount = amount,
                timestamp = timestamp,
                category = category,
                groupId = group.groupId,
                paidBy = localPayer,
                remoteId = payload.remoteId,
                isSynced = true
            )

            expenseDao.insert(remoteExpense)
            expenseDao.recalculateGroupTotal(group.groupId)
        } catch (e: Exception) {
            Log.e("Sync", "Remote Decryption Failed: ${e.message}")
        }
    }

    private suspend fun processRemoteIdentity(payload: EncryptedPayload, group: Group) {
        try {
            val decryptedJson = GroupCryptoUtils.decrypt(payload.encryptedData, group.syncKey!!)
            val identity = gson.fromJson(decryptedJson, IdentityPayload::class.java)
            val myName = OnboardingUtils.getUserName(context)
            
            if (identity.name.isNotBlank() && !identity.name.equals(myName, ignoreCase = true)) {
                val nameToReconcile = identity.invitedAs
                if (nameToReconcile != null && !nameToReconcile.equals(identity.name, ignoreCase = true)) {
                    // GLOBAL RECONCILIATION: When we see someone has claimed a name, 
                    // remove that name from our group member list to maintain parity.
                    expenseDao.renameMemberInGroup(group.groupId, nameToReconcile, identity.name)
                    val members = group.getMemberList().toMutableList()
                    members.removeIf { it.equals(nameToReconcile, ignoreCase = true) }
                    if (!members.any { it.equals(identity.name, ignoreCase = true) }) {
                        members.add(identity.name)
                    }
                    val updatedGroup = group.copy(members = members.joinToString(","))
                    expenseDao.updateGroup(updatedGroup)
                    pushGroupMetadata(updatedGroup) // authoritative update
                    invitationHints[group.remoteId] = nameToReconcile // track legacy name
                }
                
                val member = GroupMember(
                    groupId = group.groupId,
                    name = identity.name,
                    upiId = identity.upiId,
                    lastSynced = System.currentTimeMillis(),
                    isActive = identity.isActive
                )
                expenseDao.insertMember(member)

                val currentMembers = group.getMemberList().toMutableList()
                currentMembers.removeAll { it.equals("Friend", ignoreCase = true) }
                
                if (!currentMembers.any { it.equals(identity.name, ignoreCase = true) }) {
                    currentMembers.add(identity.name)
                    val updatedGroup = group.copy(members = currentMembers.joinToString(","))
                    expenseDao.updateGroup(updatedGroup)
                    pushGroupMetadata(updatedGroup)
                }
            }
        } catch (e: Exception) {
            Log.e("Sync", "Identity Failed: ${e.message}")
        }
    }

    fun stopSync(groupRemoteId: String) {
        activeListeners[groupRemoteId]?.let {
            database.child("groups").child(groupRemoteId).child("expenses").removeEventListener(it)
            activeListeners.remove(groupRemoteId)
        }
        identityListeners[groupRemoteId]?.let {
            database.child("groups").child(groupRemoteId).child("identities").removeEventListener(it)
            identityListeners.remove(groupRemoteId)
        }
    }
}
