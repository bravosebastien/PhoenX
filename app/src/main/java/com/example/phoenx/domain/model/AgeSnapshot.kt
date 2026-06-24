package com.example.phoenx.domain.model

import java.time.LocalDate
import java.time.Period

/**
 * Représente l'âge exact du créateur au moment d'une saisie.
 * C'est l'ADN de Phoen-X : "Le Fil de Pensée par Âge".
 */
data class AgeSnapshot(
    val years: Int,
    val months: Int,
    val days: Int
) {
    override fun toString(): String {
        return "$years ans, $months mois et $days jours"
    }

    companion object {
        fun calculateFrom(birthDate: LocalDate, targetDate: LocalDate = LocalDate.now()): AgeSnapshot {
            val period = Period.between(birthDate, targetDate)
            return AgeSnapshot(period.years, period.months, period.days)
        }
    }
}
