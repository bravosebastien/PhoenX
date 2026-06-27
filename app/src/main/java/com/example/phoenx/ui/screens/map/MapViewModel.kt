package com.example.phoenx.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val offlineEntryDao: OfflineEntryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState

    init {
        loadMapEntries()
    }

    private fun loadMapEntries() {
        viewModelScope.launch {
            offlineEntryDao.getAllEntries().collectLatest { entries ->
                val mapEntries = entries.filter { it.latitude != null && it.longitude != null }
                _uiState.value = MapUiState.Success(mapEntries)
            }
        }
    }

    fun addMapEntry(latitude: Double, longitude: Double, name: String) {
        viewModelScope.launch {
            val entry = OfflineEntry(
                entryType = "LOCATION",
                encryptedPayload = "Lieu enregistré : $name".toByteArray(),
                ageAtCreation = "{ \"years\": 0, \"months\": 0, \"days\": 0 }", // Temporary
                emotionalCategory = "Voyage",
                visibility = "Privé",
                latitude = latitude,
                longitude = longitude,
                locationName = name,
                aiSummary = "Souvenir à $name"
            )
            offlineEntryDao.insertEntry(entry)
        }
    }
}

sealed class MapUiState {
    object Loading : MapUiState()
    data class Success(val entries: List<OfflineEntry>) : MapUiState()
}
