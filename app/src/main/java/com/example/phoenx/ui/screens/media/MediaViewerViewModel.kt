package com.example.phoenx.ui.screens.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.media.MediaManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MediaViewerViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
    val mediaManager: MediaManager,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _entryId = MutableStateFlow<String?>(null)
    private val _creatorId = MutableStateFlow<String?>(null)

    val entry: StateFlow<OfflineEntry?> = _entryId
        .filterNotNull()
        .flatMapLatest { id -> offlineEntryDao.getEntryById(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _heirKey = MutableStateFlow<ByteArray?>(null)
    val heirKey: StateFlow<ByteArray?> = _heirKey.asStateFlow()

    fun loadMedia(entryId: String, creatorId: String?) {
        _entryId.value = entryId
        _creatorId.value = creatorId

        if (creatorId != null && creatorId != auth.currentUser?.uid) {
            viewModelScope.launch {
                try {
                    val keyDoc = db.collection("users").document(creatorId)
                        .collection("entry_keys").document("main").get().await()
                    val keyBase64 = keyDoc.getString("key")
                    if (keyBase64 != null) {
                        _heirKey.value = android.util.Base64.decode(keyBase64, android.util.Base64.NO_WRAP)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MediaViewerVM", "Erreur chargement clé héritage: ${e.message}")
                }
            }
        }
    }
}
