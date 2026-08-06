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
    private val standaloneMediaDao: com.example.phoenx.data.local.StandaloneMediaDao, // v9.4.27
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val encryptionManager: com.example.phoenx.data.encryption.EncryptionManager,
    private val mediaManager: com.example.phoenx.data.media.MediaManager // v9.4.27
) : ViewModel() {

    private val _targetCreatorId = MutableStateFlow<String?>(null)
    val currentUid: String get() = auth.currentUser?.uid ?: ""

    private val _viewMode = MutableStateFlow(com.example.phoenx.ui.screens.recipient.MediaViewMode.DEFAULT)
    val viewMode: StateFlow<com.example.phoenx.ui.screens.recipient.MediaViewMode> = _viewMode.asStateFlow()

    private val _filterRecipientId = MutableStateFlow<String?>(null)
    val filterRecipientId: StateFlow<String?> = _filterRecipientId.asStateFlow()

    fun setViewMode(mode: com.example.phoenx.ui.screens.recipient.MediaViewMode) {
        _viewMode.value = mode
    }

    fun setFilterRecipient(uid: String?) {
        _filterRecipientId.value = uid
    }

    private val _heirKey = MutableStateFlow<ByteArray?>(null)
    val heirKey: StateFlow<ByteArray?> = _heirKey.asStateFlow()
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

    /**
     * Chiffre et uploade une photo de couverture pour un extrait (v9.4.27)
     */
    fun updateExcerptCover(id: String, imageUri: android.net.Uri) {
        val context = com.google.firebase.FirebaseApp.getInstance().applicationContext
        viewModelScope.launch {
            val file = try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val tempFile = java.io.File(context.cacheDir, "temp_cover_lit_${java.util.UUID.randomUUID()}.jpg")
                inputStream?.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }
                tempFile
            } catch(_: Exception) { null } ?: return@launch
            
            try {
                val storagePath = mediaManager.encryptAndUpload(currentUid, id, file)
                standaloneMediaDao.updateMediaCover(id, storagePath, file.absolutePath)
                standaloneMediaDao.updateSyncStatus(id, "pending")
                android.util.Log.d("LitLibraryVM", "Couverture manuscrit mise à jour : $id")
            } catch (e: Exception) {
                android.util.Log.e("LitLibraryVM", "Erreur upload couverture manuscrit", e)
            }
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
