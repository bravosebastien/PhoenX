package com.example.phoenx.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.model.StandaloneMedia
import com.example.phoenx.data.repository.StandaloneMediaRepository
import com.example.phoenx.data.local.OfflineEntryDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Blob
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject

@HiltViewModel
class LiteraryLibraryViewModel @Inject constructor(
    private val repository: StandaloneMediaRepository,
    private val offlineEntryDao: OfflineEntryDao,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val encryptionManager: com.example.phoenx.data.encryption.EncryptionManager
) : ViewModel() {

    private val _targetCreatorId = MutableStateFlow<String?>(null)
    val currentUid: String get() = auth.currentUser?.uid ?: ""

    private val _heirKey = MutableStateFlow<ByteArray?>(null)
    private val _isProtocolActivated = MutableStateFlow(false)

    // Liste des extraits (Combinaison local/distant selon le mode)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val excerpts: StateFlow<List<StandaloneMedia>> = _targetCreatorId.flatMapLatest { targetId ->
        if (targetId == null || targetId == currentUid) {
            // MODE CRÉATEUR : Local (Room)
            repository.getMediaByType("TEXT_EXCERPT")
        } else {
            // MODE HÉRITIER : Firestore
            combine(
                callbackFlow {
                    val listener = db.collection("users").document(targetId)
                        .collection("standaloneMedia")
                        .whereEqualTo("type", "TEXT_EXCERPT")
                        .addSnapshotListener { snapshot, _ ->
                            trySend(snapshot?.documents ?: emptyList())
                        }
                    awaitClose { listener.remove() }
                },
                _heirKey,
                _isProtocolActivated
            ) { documents, key, activated ->
                documents.mapNotNull { doc ->
                    val contentBlob = doc.get("content") as? Blob
                    val decrypted = if (contentBlob != null) {
                        if (activated) {
                            try {
                                encryptionManager.decryptText(contentBlob.toBytes(), key)
                            } catch (e: Exception) { "Erreur déchiffrement" }
                        } else "Contenu scellé"
                    } else doc.getString("content") ?: ""

                    StandaloneMedia(
                        id = doc.id,
                        type = "TEXT_EXCERPT",
                        title = doc.getString("title") ?: "",
                        userComment = doc.getString("userComment"), // v9.4.27
                        content = decrypted,
                        recipientIds = (doc.get("recipientIds") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList(),
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recipients = offlineEntryDao.getAllRecipients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTargetCreator(creatorId: String?) {
        _targetCreatorId.value = creatorId
        if (creatorId != null && creatorId != currentUid) {
            viewModelScope.launch {
                try {
                    // Check protocol status
                    val result = functions.getHttpsCallable("getCreatorProtocolStatus")
                        .call(mapOf("creatorId" to creatorId)).await()
                    val data = result.data as? Map<*, *>
                    _isProtocolActivated.value = data?.get("isActivated") as? Boolean ?: false

                    if (_isProtocolActivated.value) {
                        val keyDoc = db.collection("users").document(creatorId)
                            .collection("entry_keys").document("main").get().await()
                        val keyBase64 = keyDoc.getString("key")
                        if (keyBase64 != null) {
                            _heirKey.value = android.util.Base64.decode(keyBase64 as String, android.util.Base64.NO_WRAP)
                        }
                    }
                } catch (e: Exception) {
                    _isProtocolActivated.value = false
                }
            }
        }
    }

    fun addExcerpt(title: String, content: String, recipientIds: List<String>, userComment: String? = null, existingId: String? = null) {
        viewModelScope.launch {
            val media = StandaloneMedia(
                id = existingId ?: java.util.UUID.randomUUID().toString(),
                type = "TEXT_EXCERPT",
                title = title,
                userComment = userComment,
                content = content,
                recipientIds = recipientIds
            )
            repository.saveMedia(media)
        }
    }

    fun deleteExcerpt(media: StandaloneMedia) {
        viewModelScope.launch {
            try {
                db.collection("users").document(currentUid)
                    .collection("standaloneMedia").document(media.id).delete().await()
                
                repository.deleteMedia(media.id)
            } catch (e: Exception) { }
        }
    }
}
