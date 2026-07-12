package com.example.phoenx.domain.model

import java.time.LocalDate

/**
 * Profil utilisateur Phoen-X unifié (v7.2)
 */
data class UserAccount(
    val uid: String,
    val email: String,
    val displayName: String = "",
    val birthDate: LocalDate? = null,
    val isCreator: Boolean = false,
    val myRoles: Map<String, UserRole> = emptyMap(),
    val createdAt: LocalDate = LocalDate.now()
)
