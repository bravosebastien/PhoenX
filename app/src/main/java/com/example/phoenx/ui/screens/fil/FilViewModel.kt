package com.example.phoenx.ui.screens.fil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.domain.model.AgeSnapshot
import com.example.phoenx.domain.model.EntryType
import com.example.phoenx.domain.model.PhoenXEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class FilViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<FilUiState>(FilUiState())
    val uiState: StateFlow<FilUiState> = _uiState

    init {
        loadEntries()
    }

    private fun loadEntries() {
        viewModelScope.launch {
            // Simulation de chargement de données
            val mockEntries = listOf(
                PhoenXEntry(
                    id = "1",
                    ageAtCreation = AgeSnapshot(43, 4, 12),
                    encryptedContent = "L'importance de transmettre ce qui ne peut s'écrire...".toByteArray(),
                    type = EntryType.THOUGHT,
                    timestamp = Instant.now()
                ),
                PhoenXEntry(
                    id = "2",
                    ageAtCreation = AgeSnapshot(43, 0, 5),
                    encryptedContent = "Aujourd'hui, j'ai réalisé que...".toByteArray(),
                    type = EntryType.EMOTION,
                    timestamp = Instant.now()
                ),
                PhoenXEntry(
                    id = "3",
                    ageAtCreation = AgeSnapshot(40, 11, 28),
                    encryptedContent = "Une pensée de mes 40 ans.".toByteArray(),
                    type = EntryType.THOUGHT,
                    timestamp = Instant.now()
                )
            )
            _uiState.value = FilUiState(
                entries = mockEntries,
                totalCount = mockEntries.size,
                minAge = 40,
                maxAge = 43
            )
        }
    }
}

data class FilUiState(
    val entries: List<PhoenXEntry> = emptyList(),
    val totalCount: Int = 0,
    val minAge: Int = 0,
    val maxAge: Int = 0,
    val isLoading: Boolean = false
)
