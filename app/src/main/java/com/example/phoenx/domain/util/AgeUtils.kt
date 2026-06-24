package com.example.phoenx.domain.util

import com.example.phoenx.domain.model.AgeSnapshot
import java.time.LocalDate
import java.time.Period
import java.util.Date
import java.time.ZoneId

object AgeUtils {
    fun calculateAge(birthDate: Date, targetDate: Date = Date()): AgeSnapshot {
        val birth = birthDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val target = targetDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val period = Period.between(birth, target)
        return AgeSnapshot(period.years, period.months, period.days)
    }
}
