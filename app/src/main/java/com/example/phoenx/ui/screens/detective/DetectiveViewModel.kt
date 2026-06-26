package com.example.phoenx.ui.screens.detective

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetectiveViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetectiveUiState())
    val uiState: StateFlow<DetectiveUiState> = _uiState

    init {
        loadLockedEntries()
    }

    private fun loadLockedEntries() {
        viewModelScope.launch {
            offlineEntryDao.getAllEntries().collectLatest { entries ->
                val locked = entries.filter { it.enigmaQuestion != null }
                _uiState.value = _uiState.value.copy(
                    lockedEntries = locked,
                    isLoading = false
                )
            }
        }
    }

    fun attemptUnlock(entry: OfflineEntry, answer: String) {
        if (entry.enigmaAnswer?.lowercase() == answer.trim().lowercase()) {
            _uiState.value = _uiState.value.copy(unlockedEntryId = entry.id)
            // En prod, on déchiffrerait le contenu ici
        } else {
            _uiState.value = _uiState.value.copy(error = "Mauvaise réponse. Cherche encore...")
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class DetectiveUiState(
    val lockedEntries: List<OfflineEntry> = emptyList(),
    val unlockedEntryId: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)
