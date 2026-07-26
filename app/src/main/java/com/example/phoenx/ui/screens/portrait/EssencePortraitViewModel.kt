package com.example.phoenx.ui.screens.portrait

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.ai.AIManager
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.toFirestoreMap
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EssencePortraitViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val aiManager: AIManager,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<PortraitUiState>(PortraitUiState.Idle)
    val uiState: StateFlow<PortraitUiState> = _uiState

    fun generatePortrait() {
        val userId = auth.currentUser?.uid ?: return
        _uiState.value = PortraitUiState.Loading
        viewModelScope.launch {
            try {
                val summaries = offlineEntryDao.getAllAiSummaries()
                if (summaries.isEmpty()) {
                    _uiState.value = PortraitUiState.Empty
                    return@launch
                }

                // v9.1 : Récupération du profil enrichi du Créateur
                val richProfile = offlineEntryDao.getCreatorProfileSync(userId)
                val authorProfileMap = richProfile?.toFirestoreMap()
                
                val portraitText = aiManager.generateEssencePortrait(summaries, authorProfileMap)
                _uiState.value = PortraitUiState.Success(portraitText)
            } catch (e: Exception) {
                _uiState.value = PortraitUiState.Error(e.message ?: "Erreur de génération")
            }
        }
    }
}

sealed class PortraitUiState {
    object Idle : PortraitUiState()
    object Loading : PortraitUiState()
    object Empty : PortraitUiState()
    data class Success(val content: String) : PortraitUiState()
    data class Error(val message: String) : PortraitUiState()
}
