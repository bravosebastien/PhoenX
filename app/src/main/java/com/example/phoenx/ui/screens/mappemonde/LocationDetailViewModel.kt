package com.example.phoenx.ui.screens.mappemonde

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class LocationDetailUiState {
    object Loading : LocationDetailUiState()
    data class Success(val location: LocationMemory, val entries: List<OfflineEntry>) : LocationDetailUiState()
    object Error : LocationDetailUiState()
}

@HiltViewModel
class LocationDetailViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val offlineEntryDao: OfflineEntryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<LocationDetailUiState>(LocationDetailUiState.Loading)
    val uiState: StateFlow<LocationDetailUiState> = _uiState.asStateFlow()

    fun loadLocationData(locationId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(userId).collection("locations").document(locationId).get().await()
                val location = doc.toObject(LocationMemory::class.java)?.copy(id = doc.id)
                
                if (location != null) {
                    offlineEntryDao.getAllEntries().collectLatest { allEntries ->
                        val relatedEntries = allEntries.filter { it.latitude == location.latitude && it.longitude == location.longitude }
                        _uiState.value = LocationDetailUiState.Success(location, relatedEntries)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = LocationDetailUiState.Error
            }
        }
    }
}
