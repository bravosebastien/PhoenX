package com.example.phoenx.ui.screens.mappemonde

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.local.OfflineEntryDao
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class MappamondeViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val offlineEntryDao: OfflineEntryDao
) : ViewModel() {

    private val _locations = MutableStateFlow<List<LocationMemory>>(emptyList())
    val locations: StateFlow<List<LocationMemory>> = _locations.asStateFlow()

    private val _selectedLocation = MutableStateFlow<LocationMemory?>(null)
    val selectedLocation: StateFlow<LocationMemory?> = _selectedLocation.asStateFlow()

    private val _mapMode = MutableStateFlow(MapMode.CREATOR)
    val mode: StateFlow<MapMode> = _mapMode.asStateFlow()

    // Choix entre vue classique et globe
    private val _isGlobeView = MutableStateFlow(true)
    val isGlobeView: StateFlow<Boolean> = _isGlobeView.asStateFlow()

    init {
        loadLocations()
    }

    fun setMode(mode: MapMode) {
        _mapMode.value = mode
    }

    fun toggleMapView() {
        _isGlobeView.value = !_isGlobeView.value
    }

    fun loadLocations() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(userId).collection("locations").get().await()
                val list = snapshot.documents.mapNotNull { it.toObject(LocationMemory::class.java)?.copy(id = it.id) }
                _locations.value = list
            } catch (e: Exception) {
                // Fallback ou erreur
            }
        }
    }

    fun pinLocation(
        latLng: LatLng,
        placeName: String,
        countryName: String,
        emoji: String,
        visitedAt: Long
    ) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val newLoc = LocationMemory(
                latitude = latLng.latitude,
                longitude = latLng.longitude,
                placeName = placeName,
                countryName = countryName,
                emoji = emoji,
                visitedAt = visitedAt
            )
            db.collection("users").document(userId).collection("locations").add(newLoc).await()
            loadLocations()
        }
    }

    fun removeLocation(locationId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            db.collection("users").document(userId).collection("locations").document(locationId).delete().await()
            loadLocations()
            _selectedLocation.value = null
        }
    }

    fun selectLocation(location: LocationMemory?) {
        _selectedLocation.value = location
    }
}
