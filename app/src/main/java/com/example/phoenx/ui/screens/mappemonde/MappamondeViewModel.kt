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
import java.util.Calendar
import java.util.Date

@HiltViewModel
class MappamondeViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val offlineEntryDao: OfflineEntryDao
) : ViewModel() {

    private val _allLocations = MutableStateFlow<List<LocationWithEntries>>(emptyList())
    val allLocations: StateFlow<List<LocationWithEntries>> = _allLocations.asStateFlow()

    private var birthDate: java.util.Date? = null

    private val _visibleLocations = MutableStateFlow<List<LocationWithEntries>>(emptyList())
    val visibleLocations: StateFlow<List<LocationWithEntries>> = _visibleLocations.asStateFlow()

    private val _trailPoints = MutableStateFlow<List<LatLng>>(emptyList())
    val trailPoints: StateFlow<List<LatLng>> = _trailPoints.asStateFlow()

    private val _lastAppearedLocation = MutableStateFlow<LocationMemory?>(null)
    val lastAppearedLocation: StateFlow<LocationMemory?> = _lastAppearedLocation.asStateFlow()

    val canShowTimeline: StateFlow<Boolean> = _allLocations.map { list ->
        // On autorise la timeline dès qu'il y a des lieux avec des dates
        list.size >= 2
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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _focusLocation = MutableStateFlow<LocationMemory?>(null)
    val focusLocation: StateFlow<LocationMemory?> = _focusLocation.asStateFlow()

    fun initialize(targetCreatorId: String? = null, focusEntryId: String? = null) {
        val cleanTargetId = targetCreatorId?.takeIf { it.isNotBlank() && !it.startsWith("{") && it != "null" }
        loadCurrentAge(cleanTargetId)
        loadLocations(cleanTargetId, focusEntryId)
    }

    private fun loadCurrentAge(targetCreatorId: String? = null) {
        val userId = targetCreatorId ?: auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(userId).get().await()
                val birthTimestamp = doc.getTimestamp("dateOfBirth")
                if (birthTimestamp != null) {
                    val date = birthTimestamp.toDate()
                    birthDate = date
                    val age = AgeUtils.calculateAge(date)
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

    private fun loadLocations(targetCreatorId: String? = null, focusEntryId: String? = null) {
        val userId = targetCreatorId ?: auth.currentUser?.uid ?: return
        android.util.Log.d("PHOENX_MAP_TRACE", "loadLocations: target=$targetCreatorId, current=${auth.currentUser?.uid}, finalUid=$userId")

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = db.collection("users").document(userId).collection("locations").get().await()
                android.util.Log.d("PHOENX_MAP_TRACE", "Firestore: ${snapshot.size()} locations chargées.")

                val locations = snapshot.documents.mapNotNull { it.toObject(LocationMemory::class.java)?.copy(id = it.id) }
                android.util.Log.d("PHOENX_MAP_TRACE", "MappamondeVM: ${locations.size} lieux trouvés. IDs: ${locations.map { it.id }}")

                // Charger les entrées pour chaque lieu pour la timeline
                val locationsWithEntries = coroutineScope {
                    locations.map { loc ->
                        async {
                            val entries = if (loc.entryIds.isNotEmpty()) {
                                android.util.Log.d("PHOENX_MAP_TRACE", "MappamondeVM: Recherche entries pour lieu ${loc.id} (placeName=${loc.placeName}). IDs attendus: ${loc.entryIds}")
                                // v9.4.27 : Lecture hybride pour les entrées rattachées au lieu
                                if (targetCreatorId != null && targetCreatorId != auth.currentUser?.uid) {
                                    // Mode Héritier
                                    emptyList<OfflineEntry>()
                                } else {
                                    val list = offlineEntryDao.getEntriesByIds(loc.entryIds).first()
                                    android.util.Log.d("PHOENX_MAP_TRACE", "   -> ${list.size} entries trouvées dans Room pour ce lieu.")
                                    list
                                }
                            } else {
                                android.util.Log.d("PHOENX_MAP_TRACE", "MappamondeVM: Lieu ${loc.id} sans entryIds rattachés.")
                                emptyList<OfflineEntry>()
                            }
                            LocationWithEntries(loc, entries)
                        }
                    }.awaitAll()
                }
                
                _allLocations.value = locationsWithEntries
                updateVisibleAndTrail(locationsWithEntries)

                // Gestion du FOCUS (v9.4.27)
                if (focusEntryId != null) {
                    val target = locationsWithEntries.find { it.location.entryIds.contains(focusEntryId) }
                    if (target != null) {
                        _focusLocation.value = target.location
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MappamondeVM", "Error loading locations", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updateVisibleAndTrail(visible: List<LocationWithEntries>) {
        _visibleLocations.value = visible
        
        // Calcul du fil d'or (Polyline)
        // On relie les points visibles triés par date
        if (visible.size >= 2) {
            _trailPoints.value = visible
                .map { it.location }
                .filter { it.visitedAt > 0 }
                .sortedBy { it.visitedAt }
                .map { LatLng(it.latitude, it.longitude) }
        } else {
            _trailPoints.value = emptyList()
        }
    }

    fun onTimelineSlide(maxAge: Int) {
        _timelineAge.value = maxAge
        
        if (maxAge >= _currentAge.value) {
            updateVisibleAndTrail(_allLocations.value)
            return
        }
        
        val birth = birthDate
        if (birth == null) {
            updateVisibleAndTrail(_allLocations.value)
            return
        }
        
        // Calculer date cutoff
        val calendar = Calendar.getInstance()
        calendar.time = birth
        calendar.add(Calendar.YEAR, maxAge)
        val cutoffMillis = calendar.timeInMillis

        val newVisible = _allLocations.value.filter { item ->
            val loc = item.location
            val refDate = loc.startDate ?: loc.visitedAt
            refDate <= 0L || refDate <= cutoffMillis
        }

        updateVisibleAndTrail(newVisible)
    }

    /**
     * Épingle un nouveau lieu et retourne son ID Firestore.
     */
    suspend fun pinLocation(
        latLng: LatLng,
        placeName: String,
        countryName: String,
        emoji: String,
        visitedAt: Long
    ): String? {
        val userId = auth.currentUser?.uid ?: return null
        return try {
            val newLoc = LocationMemory(
                latitude = latLng.latitude,
                longitude = latLng.longitude,
                placeName = placeName,
                countryName = countryName,
                emoji = emoji,
                visitedAt = visitedAt
            )
            val ref = db.collection("users").document(userId).collection("locations").add(newLoc).await()
            loadLocations()
            ref.id
        } catch (e: Exception) {
            android.util.Log.e("MappamondeVM", "Error pinning location", e)
            null
        }
    }

    fun updateLocationName(locationId: String, newName: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .collection("locations").document(locationId)
                    .update("placeName", newName).await()
                loadLocations()
            } catch (e: Exception) {
                android.util.Log.e("MappamondeVM", "Error updating name", e)
            }
        }
    }

    fun updateLocationDate(locationId: String, dateMillis: Long) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .collection("locations").document(locationId)
                    .update("visitedAt", dateMillis).await()
                loadLocations()
            } catch (e: Exception) {
                android.util.Log.e("MappamondeVM", "Error updating date", e)
            }
        }
    }

    fun updateLocationPeriod(locationId: String, start: Long?, end: Long?) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .collection("locations").document(locationId)
                    .update(mapOf(
                        "startDate" to start,
                        "endDate" to end,
                        "visitedAt" to (start ?: 0L) // On garde visitedAt synchro avec le début
                    )).await()
                loadLocations()
            } catch (e: Exception) {
                android.util.Log.e("MappamondeVM", "Error updating period", e)
            }
        }
    }

    fun removeLocation(locationId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // 1. Détacher tous les souvenirs de ce lieu en local
                offlineEntryDao.detachAllEntriesFromLocation(locationId)
                
                // 2. Supprimer le lieu de Firestore
                db.collection("users").document(userId).collection("locations").document(locationId).delete().await()

                loadLocations()
                _selectedLocation.value = null
            } catch (e: Exception) {
                android.util.Log.e("MappamondeVM", "Error deleting location", e)
            }
        }
    }

    fun selectLocation(location: LocationMemory?) {
        _selectedLocation.value = location
    }
}
