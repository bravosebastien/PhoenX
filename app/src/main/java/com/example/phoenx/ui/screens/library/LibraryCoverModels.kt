package com.example.phoenx.ui.screens.library

data class LibraryCover(
    val compartmentId: String = "",
    val mediaType: String = "none", // "photo" | "video" | "none"
    val mediaUrl: String = "",
    val uploadedAt: Long = 0L,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)
