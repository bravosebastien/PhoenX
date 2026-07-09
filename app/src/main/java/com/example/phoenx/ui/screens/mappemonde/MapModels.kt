package com.example.phoenx.ui.screens.mappemonde

import com.example.phoenx.data.local.OfflineEntry
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

data class LocationMemory(
    val id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val placeName: String = "",
    val countryName: String = "",
    val emoji: String = "📍",
    val visitedAt: Long = 0L, // Date unique (legacy)
    val startDate: Long? = null, // Début de période (Signature 7.1)
    val endDate: Long? = null,   // Fin de période (Signature 7.1)
    val entryIds: List<String> = emptyList(),
    val coverPhotoUrl: String = "",
    val memoriesCount: Int = 0
)

data class LocationWithEntries(
    val location: LocationMemory,
    val entries: List<OfflineEntry>
) : ClusterItem {
    override fun getPosition(): LatLng = LatLng(location.latitude, location.longitude)
    override fun getTitle(): String = location.placeName
    override fun getSnippet(): String = "${entries.size} souvenirs"
    override fun getZIndex(): Float? = null
}

enum class MapMode { CREATOR, RECIPIENT }
