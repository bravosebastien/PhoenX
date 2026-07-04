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
import com.example.phoenx.domain.util.AgeUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@HiltViewModel
class MappamondeViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val offlineEntryDao: OfflineEntryDao
) : ViewModel() {

    private val _allLocations = MutableStateFlow<List<LocationWithEntries>>(emptyList())

    private val _visibleLocations = MutableStateFlow<List<LocationWithEntries>>(emptyList())
    val visibleLocations: StateFlow<List<LocationWithEntries>> = _visibleLocations.asStateFlow()

    private val _trailPoints = MutableStateFlow<List<LatLng>>(emptyList())
    val trailPoints: StateFlow<List<LatLng>> = _trailPoints.asStateFlow()

    private val _lastAppearedLocation = MutableStateFlow<LocationMemory?>(null)
    val lastAppearedLocation: StateFlow<LocationMemory?> = _lastAppearedLocation.asStateFlow()

    val canShowTimeline: StateFlow<Boolean> = _allLocations.map { list ->
        val ages = list.flatMap { it.entries.map { e -> AgeUtils.parseAgeJson(e.ageAtCreation).years } }.distinct()
        ages.size >= 2
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _selectedLocation = MutableStateFlow<LocationMemory?>(null)
    val selectedLocation: StateFlow<LocationMemory?> = _selectedLocation.asStateFlow()

    private val _mapMode = MutableStateFlow(MapMode.CREATOR)
    val mode: StateFlow<MapMode> = _mapMode.asStateFlow()

    private val _isGlobeView = MutableStateFlow(true)
    val isGlobeView: StateFlow<Boolean> = _isGlobeView.asStateFlow()

    private val _timelineAge = MutableStateFlow(100) // Défault aujourd'hui
    val timelineAge: StateFlow<Int> = _timelineAge.asStateFlow()

    private val _currentAge = MutableStateFlow(0)
    val currentAge: StateFlow<Int> = _currentAge.asStateFlow()

    init {
        loadCurrentAge()
        loadLocations()
    }

    private fun loadCurrentAge() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(userId).get().await()
                val birthTimestamp = doc.getTimestamp("dateOfBirth")
                if (birthTimestamp != null) {
                    val age = AgeUtils.calculateAge(birthTimestamp.toDate())
                    _currentAge.value = age.years
                    _timelineAge.value = age.years
                }
            } catch (e: Exception) {}
        }
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
                val locations = snapshot.documents.mapNotNull { it.toObject(LocationMemory::class.java)?.copy(id = it.id) }
                
                // Charger les entrées pour chaque lieu pour la timeline
                val locationsWithEntries = coroutineScope {
                    locations.map { loc ->
                        async {
                            val entries = if (loc.entryIds.isNotEmpty()) {
                                offlineEntryDao.getEntriesByIds(loc.entryIds).first()
                            } else emptyList<OfflineEntry>()
                            LocationWithEntries(loc, entries)
                        }
                    }.awaitAll()
                }
                
                _allLocations.value = locationsWithEntries
                updateVisibleAndTrail(locationsWithEntries)
            } catch (e: Exception) {
                // Fallback ou erreur
            }
        }
    }

    private fun updateVisibleAndTrail(visible: List<LocationWithEntries>) {
        _visibleLocations.value = visible
        
        // Calcul du fil d'ariane (Polyline)
        // Visible uniquement si la timeline est active (moins de points que le total)
        if (visible.size < _allLocations.value.size && visible.size >= 2) {
            _trailPoints.value = visible
                .sortedBy { it.location.visitedAt }
                .map { LatLng(it.location.latitude, it.location.longitude) }
        } else {
            _trailPoints.value = emptyList()
        }
    }

    fun onTimelineSlide(maxAge: Int) {
        val previousLocations = _visibleLocations.value
        _timelineAge.value = maxAge
        
        val newVisible = _allLocations.value.filter { locationWithEntries ->
            if (locationWithEntries.entries.isEmpty()) true // Les lieux sans souvenirs restent
            else locationWithEntries.entries.any { entry ->
                val age = AgeUtils.parseAgeJson(entry.ageAtCreation)
                age.years <= maxAge
            }
        }

        // Détection de la Bulle d'Éveil
        val newlyAppeared = newVisible.filter { item -> !previousLocations.any { it.location.id == item.location.id } }
        newlyAppeared.lastOrNull()?.let { item ->
            _lastAppearedLocation.value = item.location
            viewModelScope.launch {
                kotlinx.coroutines.delay(2500)
                if (_lastAppearedLocation.value?.id == item.location.id) {
                    _lastAppearedLocation.value = null
                }
            }
        }

        updateVisibleAndTrail(newVisible)
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
