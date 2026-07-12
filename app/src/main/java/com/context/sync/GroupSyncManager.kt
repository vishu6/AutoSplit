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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
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

    @Keep
    data class JoinResult(
        val groupId: Int = 0,
        val groupName: String = "",
        val isNewJoin: Boolean = false
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
        val invitedAs: String? = null
    ) {
        constructor() : this("", null, null)
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
                    if (invitedAs != null) {
                        invitationHints[remoteId] = invitedAs
                    }
                    
                    joinGroup(remoteId, syncKey, name, inviterName) {
                        scope.launch(Dispatchers.Main) {
                            onComplete(name)
                        }
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

    fun joinGroup(remoteId: String, syncKey: String, name: String, inviterName: String? = null, onComplete: (() -> Unit)? = null) {
        scope.launch {
            val existing = expenseDao.getGroupByRemoteId(remoteId)
            val isNewJoin = existing == null
            val myName = OnboardingUtils.getUserName(context)
            
            val groupId = if (isNewJoin) {
                val otherMember = inviterName ?: "Friend"
                val newGroup = Group(
                    name = name,
                    members = "$myName,$otherMember", 
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
                groupFromDb?.let { startSync(it) }
                id
            } else {
                val id = existing!!.groupId
                expenseDao.getGroup(id)?.let { startSync(it) }
                id
            }
            
            _joinEvents.send(JoinResult(groupId, name, isNewJoin))
            withContext(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }
    }

    fun broadcastIdentity(group: Group) {
        if (!group.isSyncEnabled || group.syncKey == null) return

        scope.launch {
            val userName = OnboardingUtils.getUserName(context)
            val invitedAsHint = invitationHints[group.remoteId]
            
            val identity = IdentityPayload(
                name = userName,
                upiId = OnboardingUtils.getUpiId(context),
                invitedAs = invitedAsHint
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
        if (expense.groupId == null) return
        
        scope.launch {
            val group = expenseDao.getGroup(expense.groupId)
            if (group?.isSyncEnabled == true && group.syncKey != null) {
                val myName = OnboardingUtils.getUserName(context)
                
                val displayPayer = if (expense.paidBy == "You") myName else expense.paidBy
                val displayMerchant = if (expense.category == "Settlement") {
                    expense.merchant.replace("You", myName)
                } else {
                    expense.merchant
                }

                val dataMap = mapOf(
                    "merchant" to displayMerchant,
                    "amount" to expense.amount.toString(),
                    "timestamp" to expense.timestamp.toString(),
                    "category" to expense.category,
                    "paidBy" to displayPayer
                )
                
                val json = gson.toJson(dataMap)
                val encrypted = GroupCryptoUtils.encrypt(json, group.syncKey)
                
                val remoteId = if (expense.remoteId.isBlank()) UUID.randomUUID().toString() else expense.remoteId
                val payload = EncryptedPayload(
                    remoteId = remoteId,
                    encryptedData = encrypted,
                    sender = myName
                )

                database.child("groups").child(group.remoteId).child("expenses")
                    .child(payload.remoteId)
                    .setValue(payload)
                    .addOnSuccessListener {
                        scope.launch {
                            expenseDao.update(expense.copy(isSynced = true, remoteId = payload.remoteId))
                        }
                    }
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
                    expenseDao.renameMemberInGroup(group.groupId, nameToReconcile, identity.name)
                    val members = group.getMemberList().toMutableList()
                    members.remove(nameToReconcile)
                    if (!members.contains(identity.name)) {
                        members.add(identity.name)
                    }
                    val updatedGroup = group.copy(members = members.joinToString(","))
                    expenseDao.updateGroup(updatedGroup)
                }
                
                val member = GroupMember(
                    groupId = group.groupId,
                    name = identity.name,
                    upiId = identity.upiId,
                    lastSynced = System.currentTimeMillis()
                )
                expenseDao.insertMember(member)

                val currentMembers = group.getMemberList().toMutableList()
                currentMembers.removeAll { it.equals("Friend", ignoreCase = true) }
                
                if (!currentMembers.any { it.equals(identity.name, ignoreCase = true) }) {
                    currentMembers.add(identity.name)
                    val updatedGroup = group.copy(members = currentMembers.joinToString(","))
                    expenseDao.updateGroup(updatedGroup)
                }
            }
        } catch (e: Exception) {
            Log.e("Sync", "Identity Decryption Failed: ${e.message}")
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
