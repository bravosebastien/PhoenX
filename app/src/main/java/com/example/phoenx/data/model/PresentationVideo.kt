package com.example.phoenx.data.model

data class PresentationVideo(
    val id: String = "",
    val title: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String? = null,
    val order: Int = 0
)
