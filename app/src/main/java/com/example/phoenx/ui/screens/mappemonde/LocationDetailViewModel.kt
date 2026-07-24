package com.example.phoenx.ui.screens.mappemonde

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val _recipients = MutableStateFlow<List<com.example.phoenx.data.local.RecipientEntity>>(emptyList())
    val recipients: StateFlow<List<com.example.phoenx.data.local.RecipientEntity>> = _recipients.asStateFlow()

    init {
        loadRecipients()
    }

    private fun loadRecipients() {
        viewModelScope.launch {
            offlineEntryDao.getAllRecipients().collect { list ->
                _recipients.value = list
            }
        }
    }

    fun loadLocationData(locationId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Charger le lieu depuis Firestore
                val doc = db.collection("users").document(userId)
                    .collection("locations").document(locationId).get().await()
                val location = doc.toObject(LocationMemory::class.java)?.copy(id = doc.id)
                
                if (location != null) {
                    // Collecter les souvenirs liés (Flux temps réel local)
                    offlineEntryDao.getEntriesForLocation(locationId).collectLatest { relatedEntries ->
                        // Fallback : si aucun via locationId, on cherche par nom ou pactId (legacy)
                        val finalEntries = if (relatedEntries.isEmpty()) {
                            val all = withContext(Dispatchers.IO) {
                                offlineEntryDao.getAllEntriesSync()
                            }
                            all.filter { it.locationName == location.placeName || it.pactId == location.id }
                        } else relatedEntries

                        _uiState.value = LocationDetailUiState.Success(location, finalEntries)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LocationDetailVM", "Error loading location data", e)
                _uiState.value = LocationDetailUiState.Error
            }
        }
    }

    fun updateLocation(locationId: String, newName: String, emoji: String, start: Long?, end: Long?) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .collection("locations").document(locationId)
                    .update(mapOf(
                        "placeName" to newName,
                        "emoji" to emoji,
                        "startDate" to start,
                        "endDate" to end,
                        "visitedAt" to (start ?: 0L)
                    )).await()
                loadLocationData(locationId)
            } catch (e: Exception) {
                android.util.Log.e("LocationDetailVM", "Error updating location", e)
            }
        }
    }

    fun updateEntrySummary(entryId: String, newSummary: String) {
        viewModelScope.launch {
            try {
                offlineEntryDao.updateEntrySummary(newSummary, entryId)
            } catch (e: Exception) {
                android.util.Log.e("LocationDetailVM", "Error updating entry", e)
            }
        }
    }

    fun updateEntryRecipients(entryId: String, newRecipientIds: List<String>) {
        viewModelScope.launch {
            try {
                offlineEntryDao.updateEntryRecipients(newRecipientIds.joinToString(","), entryId)
            } catch (e: Exception) {
                android.util.Log.e("LocationDetailVM", "Error updating recipients", e)
            }
        }
    }

    fun updateEntryVisibility(entryId: String, visibility: String) {
        viewModelScope.launch {
            try {
                offlineEntryDao.updateEntryVisibility(visibility, entryId)
            } catch (e: Exception) {
                android.util.Log.e("LocationDetailVM", "Error updating visibility", e)
            }
        }
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            try {
                offlineEntryDao.deleteEntry(entryId)
            } catch (e: Exception) {
                android.util.Log.e("LocationDetailVM", "Error deleting entry", e)
            }
        }
    }

    fun detachEntry(entryId: String) {
        viewModelScope.launch {
            try {
                offlineEntryDao.detachEntryFromLocation(entryId)
            } catch (e: Exception) {
                android.util.Log.e("LocationDetailVM", "Error detaching entry", e)
            }
        }
    }
}
