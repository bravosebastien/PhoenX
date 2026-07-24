package com.example.phoenx.ui.screens.mailbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.RecipientEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MailboxViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(MailboxUiState())
    val uiState: StateFlow<MailboxUiState> = _uiState

    init {
        loadScheduledItems()
    }

    private fun loadScheduledItems() {
        viewModelScope.launch {
            // Dans le cadre du MVP, on récupère les OfflineEntries qui ont un scheduledTimestamp
            offlineEntryDao.getAllEntries()
                .flowOn(Dispatchers.Default)
                .collectLatest { entries ->
                    val scheduled = entries.filter { it.scheduledTimestamp != null }
                        .sortedBy { it.scheduledTimestamp }
                    
                    _uiState.value = _uiState.value.copy(
                        scheduledItems = scheduled,
                        isLoading = false
                    )
                }
        }
        
        viewModelScope.launch {
            offlineEntryDao.getAllRecipients().collectLatest { recipients ->
                _uiState.value = _uiState.value.copy(recipients = recipients)
            }
        }
    }

    fun deleteItem(entry: OfflineEntry) {
        viewModelScope.launch {
            // En prod, on mettrait juste à jour le timestamp à null
            // Ici on simule une suppression de la programmation
        }
    }
}

data class MailboxUiState(
    val scheduledItems: List<OfflineEntry> = emptyList(),
    val recipients: List<RecipientEntity> = emptyList(),
    val isLoading: Boolean = true
)
