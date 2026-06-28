package com.example.phoenx.domain.models

import com.google.firebase.Timestamp

data class SilenceConfig(
    val rhythmDays: Int = 30, // 14, 30 ou 60
    val lastCheckInAt: Timestamp = Timestamp.now(),
    val missedCycles: Int = 0,
    val lastSilenceStatus: String = "present" // "present" | "traversing" | "no_response"
)
