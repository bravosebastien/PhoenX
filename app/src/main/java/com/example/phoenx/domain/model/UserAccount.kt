package com.example.phoenx.domain.model

import java.time.LocalDate

/**
 * Profil utilisateur Phoen-X
 */
data class UserAccount(
    val uid: String,
    val email: String,
    val birthDate: LocalDate,
    val createdAt: LocalDate = LocalDate.now()
)
