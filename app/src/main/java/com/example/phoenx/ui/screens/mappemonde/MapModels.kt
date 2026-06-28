package com.example.phoenx.ui.screens.mappemonde

data class LocationMemory(
    val id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val placeName: String = "",
    val countryName: String = "",
    val emoji: String = "📍",
    val visitedAt: Long = 0L,
    val entryIds: List<String> = emptyList(),
    val coverPhotoUrl: String = "",
    val memoriesCount: Int = 0
)

enum class MapMode { CREATOR, RECIPIENT }
