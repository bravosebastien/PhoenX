package com.example.phoenx.domain.model

import java.time.Instant

/**
 * Configuration de la transmission (Protocol Preuve de Vie)
 */
data class LegacyConfig(
    val creatorUid: String,
    val depositaryEmail: String,
    val inactivityThresholdWeeks: Int = 3,
    val lastProofOfLife: Instant = Instant.now(),
    val isLegacyTriggered: Boolean = false
)
