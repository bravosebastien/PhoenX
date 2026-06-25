package com.example.phoenx.ui.screens.recipient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.RecipientEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecipientViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecipientUiState>(RecipientUiState.Loading)
    val uiState: StateFlow<RecipientUiState> = _uiState

    init {
        loadRecipients()
    }

    private fun loadRecipients() {
        viewModelScope.launch {
            offlineEntryDao.getAllRecipients().collectLatest { recipients ->
                _uiState.value = RecipientUiState.Success(recipients)
            }
        }
    }

    fun addRecipient(name: String, email: String, relationship: String) {
        viewModelScope.launch {
            val recipient = RecipientEntity(
                name = name,
                email = email,
                relationship = relationship
            )
            offlineEntryDao.insertRecipient(recipient)
        }
    }

    fun deleteRecipient(recipient: RecipientEntity) {
        viewModelScope.launch {
            offlineEntryDao.deleteRecipient(recipient)
        }
    }
}

sealed class RecipientUiState {
    object Loading : RecipientUiState()
    data class Success(val recipients: List<RecipientEntity>) : RecipientUiState()
}
