package com.example.phoenx.ui.screens.library

import com.google.firebase.Timestamp

data class LibraryCover(
    val compartmentId: String = "",
    val mediaType: String = "none", // "photo" | "video" | "none"
    val mediaUrl: String = "",
    val uploadedAt: Long = 0L
)
