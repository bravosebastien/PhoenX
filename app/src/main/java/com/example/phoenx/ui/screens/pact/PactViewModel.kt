package com.example.phoenx.ui.screens.pact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.example.phoenx.data.local.PactEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PactViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PactUiState())
    val uiState: StateFlow<PactUiState> = _uiState.asStateFlow()

    init {
        loadPacts()
    }

    private fun loadPacts() {
        offlineEntryDao.getAllPacts()
            .onEach { pacts ->
                _uiState.update { it.copy(pacts = pacts, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun invitePartner(name: String, email: String) {
        viewModelScope.launch {
            val pact = PactEntity(partnerName = name, partnerEmail = email)
            offlineEntryDao.insertPact(pact)
        }
    }

    fun getEntriesForPact(pactId: String): Flow<List<OfflineEntry>> {
        return offlineEntryDao.getEntriesForPact(pactId)
    }
}

data class PactUiState(
    val pacts: List<PactEntity> = emptyList(),
    val isLoading: Boolean = true
)
