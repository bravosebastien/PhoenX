package com.example.phoenx.data.model

data class PresentationVideo(
    val id: String = "",
    val title: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String? = null,
    val slotIndex: Int = 0 // 1 to 6
)
