package com.example.phoenx.ui.screens.portraits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.PortraitEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PortraitViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<PortraitUiState>(PortraitUiState.Idle)
    val uiState: StateFlow<PortraitUiState> = _uiState

    fun savePortrait(recipientId: String, answers: List<String>) {
        _uiState.value = PortraitUiState.Loading
        viewModelScope.launch {
            try {
                val fullContent = answers.joinToString("\n\n")
                val tempKey = encryptionManager.deriveKeyFromPassword("temp_pass", "salt".toByteArray())
                val encrypted = encryptionManager.encryptText(fullContent, tempKey)

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
