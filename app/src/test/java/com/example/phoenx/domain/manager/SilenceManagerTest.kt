package com.example.phoenx.domain.manager

import com.example.phoenx.domain.models.SilenceConfig
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class SilenceManagerTest {

    @Test
    fun testBeforeDeadlineStatusIsOk() {
        val config = SilenceConfig(
            rhythmDays = 30,
            lastCheckInAt = Timestamp.now(),
            missedCycles = 0
        )
        val status = SilenceManager.checkSilenceStatus(config)
        assertEquals(SilenceStatus.OK, status)
    }

    @Test
    fun testAfterDeadlineStatusIsCheckInDue() {
        val thirtyOneDaysAgo = Date(System.currentTimeMillis() - (31L * 24 * 3600 * 1000))
        val config = SilenceConfig(
            rhythmDays = 30,
            lastCheckInAt = Timestamp(thirtyOneDaysAgo),
            missedCycles = 0
        )
        val status = SilenceManager.checkSilenceStatus(config)
        assertEquals(SilenceStatus.CHECK_IN_DUE, status)
    }

    @Test
    fun testTwoMissedCyclesStatusIsBlocked() {
        val config = SilenceConfig(
            rhythmDays = 30,
            lastCheckInAt = Timestamp.now(),
            missedCycles = 2
        )
        val status = SilenceManager.checkSilenceStatus(config)
        assertEquals(SilenceStatus.BLOCKED, status)
    }

    @Test
    fun testThreeMissedCyclesStatusIsNotifyDepositary() {
        val config = SilenceConfig(
            rhythmDays = 30,
            lastCheckInAt = Timestamp.now(),
            missedCycles = 3
        )
        val status = SilenceManager.checkSilenceStatus(config)
        assertEquals(SilenceStatus.NOTIFY_DEPOSITARY, status)
    }
}
