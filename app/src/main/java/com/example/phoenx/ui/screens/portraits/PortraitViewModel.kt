package com.example.phoenx.ui.screens.portraits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.PortraitEntity
import com.example.phoenx.data.local.RecipientEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PortraitViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<PortraitUiState>(PortraitUiState.Idle)
    val uiState: StateFlow<PortraitUiState> = _uiState

    private val _recipients = MutableStateFlow<List<RecipientEntity>>(emptyList())
    val recipients: StateFlow<List<RecipientEntity>> = _recipients

    init {
        viewModelScope.launch {
            offlineEntryDao.getAllRecipients().collectLatest { list ->
                _recipients.value = list
            }
        }
    }

    fun savePortrait(recipientId: String, answers: List<String>) {
        _uiState.value = PortraitUiState.Loading
        viewModelScope.launch {
            try {
                // Filtrer les réponses vides pour ne garder que la substance
                val filteredAnswers = answers.filter { it.isNotBlank() }
                if (filteredAnswers.isEmpty()) {
                    _uiState.value = PortraitUiState.Error("Le portrait est vide. Écris au moins une pensée.")
                    return@launch
                }

                val fullContent = filteredAnswers.joinToString("\n\n")
                val encrypted = encryptionManager.encryptText(fullContent)

                val portrait = PortraitEntity(
                    recipientId = recipientId,
                    encryptedContent = encrypted
                )
                offlineEntryDao.insertPortrait(portrait)
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
