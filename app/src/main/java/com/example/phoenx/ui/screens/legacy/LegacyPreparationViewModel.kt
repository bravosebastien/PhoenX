package com.example.phoenx.ui.screens.legacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.encryption.EncryptionManager
import com.example.phoenx.data.local.LegacyEntity
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.RecipientEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LegacyPreparationViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao,
    private val encryptionManager: EncryptionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LegacyPrepUiState())
    val uiState: StateFlow<LegacyPrepUiState> = _uiState

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            offlineEntryDao.getAllRecipients().collectLatest { recipients ->
                _uiState.value = _uiState.value.copy(recipients = recipients)
            }
        }
        viewModelScope.launch {
            offlineEntryDao.getAllEntries().collectLatest { entries ->
                _uiState.value = _uiState.value.copy(entries = entries)
            }
        }
    }

    fun saveLegacy(recipientId: String, selectedEntryIds: List<String>) {
        viewModelScope.launch {
            val legacy = LegacyEntity(
                recipientId = recipientId,
                entryIds = selectedEntryIds.joinToString(","),
                triggerType = "activation"
            )
            offlineEntryDao.insertLegacy(legacy)
        }
    }
}

data class LegacyPrepUiState(
    val recipients: List<RecipientEntity> = emptyList(),
    val entries: List<OfflineEntry> = emptyList()
)
