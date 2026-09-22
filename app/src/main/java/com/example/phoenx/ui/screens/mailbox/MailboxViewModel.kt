package com.example.phoenx.ui.screens.mailbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.RecipientEntity
import com.example.phoenx.data.sync.toOfflineEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MailboxViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val encryptionManager: EncryptionManager,
    private val offlineEntryDao: OfflineEntryDao
) : ViewModel() {

    private val _targetCreatorId = MutableStateFlow<String?>(null)
    private val _heirKey = MutableStateFlow<ByteArray?>(null)

    private val _uiState = MutableStateFlow(MailboxUiState())
    val uiState: StateFlow<MailboxUiState> = _uiState.asStateFlow()

    init {
        observeScheduledItems()
    }

    fun setTargetCreator(creatorId: String?) {
        val cleanId = creatorId?.takeIf { it.isNotBlank() && !it.startsWith("{") && it != "null" }
        if (_targetCreatorId.value != cleanId) {
            _targetCreatorId.value = cleanId
        }
    }

    private fun observeScheduledItems() {
        val currentUid = auth.currentUser?.uid ?: ""
        viewModelScope.launch {
            _targetCreatorId.flatMapLatest { targetId ->
                if (targetId == null || targetId == currentUid) {
                    // MODE CRÉATEUR : Room locale
                    offlineEntryDao.getAllEntries().map { entries ->
                        entries.filter { it.scheduledTimestamp != null }
                    }
                } else {
                    // MODE DESTINATAIRE : Firestore (Capsules/Lettres destinées au proche)
                    try {
                        val keyDoc = db.collection("users").document(targetId)
                            .collection("entry_keys").document("main").get().await()
                        val keyBase64 = keyDoc.getString("key")
                        if (keyBase64 != null) {
                            _heirKey.value = android.util.Base64.decode(keyBase64, android.util.Base64.NO_WRAP)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MailboxVM", "Erreur clé miroir: ${e.message}")
                    }

                    val publicFlow = callbackFlow {
                        val listener = db.collection("users").document(targetId)
                            .collection("entries")
                            .whereEqualTo("visibility", "EVERYONE")
                            .addSnapshotListener { snapshot, error ->
                                if (error != null) return@addSnapshotListener
                                val items = snapshot?.documents
                                    ?.mapNotNull { it.toOfflineEntry(encryptionManager, _heirKey.value) }
                                    ?.filter { it.scheduledTimestamp != null }
                                    ?: emptyList()
                                trySend(items)
                            }
                        awaitClose { listener.remove() }
                    }

                    val privateFlow = callbackFlow {
                        val listener = db.collection("users").document(targetId)
                            .collection("entries")
                            .whereArrayContains("recipientIds", currentUid)
                            .addSnapshotListener { snapshot, error ->
                                if (error != null) return@addSnapshotListener
                                val items = snapshot?.documents
                                    ?.mapNotNull { it.toOfflineEntry(encryptionManager, _heirKey.value) }
                                    ?.filter { it.scheduledTimestamp != null }
                                    ?: emptyList()
                                trySend(items)
                            }
                        awaitClose { listener.remove() }
                    }

                    combine(publicFlow, privateFlow) { pub, priv ->
                        (pub + priv).distinctBy { it.id }
                    }
                }
            }.collect { scheduled ->
                _uiState.update { 
                    it.copy(
                        scheduledItems = scheduled.sortedBy { item -> item.scheduledTimestamp },
                        isLoading = false
                    ) 
                }
            }
        }

        viewModelScope.launch {
            offlineEntryDao.getAllRecipients().collect { recipients ->
                _uiState.update { it.copy(recipients = recipients) }
            }
        }
    }

    fun deleteItem(entry: OfflineEntry) {
        viewModelScope.launch {
            offlineEntryDao.deleteEntry(entry.id)
        }
    }
}

data class MailboxUiState(
    val scheduledItems: List<OfflineEntry> = emptyList(),
    val recipients: List<RecipientEntity> = emptyList(),
    val isLoading: Boolean = true
)
