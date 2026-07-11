package com.example.phoenx.ui.screens.portraits

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.PortraitEntity
import com.example.phoenx.data.local.RecipientEntity
import com.example.phoenx.data.sync.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PortraitViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<PortraitUiState>(PortraitUiState.Idle)
    val uiState: StateFlow<PortraitUiState> = _uiState

    private val _recipients = MutableStateFlow<List<RecipientEntity>>(emptyList())
    val recipients: StateFlow<List<RecipientEntity>> = _recipients

    private val _currentRecipientId = MutableStateFlow<String?>(null)
    
    val existingPortrait: StateFlow<String?> = _currentRecipientId
        .filterNotNull()
        .flatMapLatest { id -> offlineEntryDao.getPortraitEntryForRecipient(id) }
        .map { entry -> 
            entry?.let { encryptionManager.decryptText(it.encryptedPayload) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            offlineEntryDao.getAllRecipients().collectLatest { list ->
                _recipients.value = list
            }
        }
    }

    fun setRecipient(recipientId: String?) {
        _currentRecipientId.value = recipientId
    }

    fun savePortrait(recipientId: String, content: String) {
        _uiState.value = PortraitUiState.Loading
        viewModelScope.launch {
            try {
                if (content.isBlank()) {
                    _uiState.value = PortraitUiState.Error("Le portrait est vide. Écris au moins une pensée.")
                    return@launch
                }

                val encrypted = encryptionManager.encryptText(content)

                // On cherche s'il existe déjà un portrait pour ce destinataire
                val existing = offlineEntryDao.getPortraitEntryForRecipient(recipientId).first()
                
                val entryId = existing?.id ?: java.util.UUID.randomUUID().toString()
                
                val portraitEntry = OfflineEntry(
                    id = entryId,
                    encryptedPayload = encrypted,
                    entryType = "PORTRAIT",
                    ageAtCreation = existing?.ageAtCreation ?: "{\"years\":0,\"months\":0,\"days\":0}", // Idéalement calculer l'âge actuel
                    emotionalCategory = "Amour",
                    visibility = "specific",
                    recipientIds = recipientId,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    syncStatus = "pending",
                    aiSummary = "Portrait de proche"
                )
                
                offlineEntryDao.insertEntry(portraitEntry)
                
                // Déclenchement de la synchro
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(context).enqueue(syncRequest)

                _uiState.value = PortraitUiState.Success
            } catch (e: Exception) {
                _uiState.value = PortraitUiState.Error(e.message ?: "Erreur de sauvegarde")
            }
        }
    }
}

sealed class PortraitUiState {
    object Idle : PortraitUiState()
    object Loading : PortraitUiState()
    object Success : PortraitUiState()
    data class Error(val message: String) : PortraitUiState()
}
