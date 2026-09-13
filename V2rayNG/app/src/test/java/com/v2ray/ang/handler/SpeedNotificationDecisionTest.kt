package com.v2ray.ang.handler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedNotificationDecisionTest {

    @Test
    fun startsWhenEnabledIdleInteractiveAndRunning() {
        assertTrue(decide())
    }

    @Test
    fun doesNotStartWhenDisabledWithoutQueryingState() {
        val probes = Probes()

        assertFalse(decide(speedEnabled = false, probes = probes))
        assertEquals(0, probes.interactiveCalls)
        assertEquals(0, probes.coreCalls)
    }

    @Test
    fun doesNotStartASecondLoop() {
        val probes = Probes()

        assertFalse(decide(loopActive = true, probes = probes))
        assertEquals(0, probes.interactiveCalls)
        assertEquals(0, probes.coreCalls)
    }

    @Test
    fun doesNotStartWhileScreenIsOffEvenAfterReload() {
        // A reload after a network handover calls start while the device is asleep.
        val probes = Probes(interactive = false)

        assertFalse(decide(probes = probes))
        assertEquals(1, probes.interactiveCalls)
        assertEquals(0, probes.coreCalls)
    }

    @Test
    fun doesNotStartWhenCoreIsStopped() {
        assertFalse(decide(probes = Probes(coreRunning = false)))
    }

    private class Probes(val interactive: Boolean = true, val coreRunning: Boolean = true) {
        var interactiveCalls = 0
        var coreCalls = 0
    }

    private fun decide(
        speedEnabled: Boolean = true,
        loopActive: Boolean = false,
        probes: Probes = Probes()
    ): Boolean = shouldStartSpeedNotification(
        speedEnabled = speedEnabled,
        loopActive = loopActive,
        isInteractive = { probes.interactiveCalls++; probes.interactive },
        isCoreRunning = { probes.coreCalls++; probes.coreRunning }
    )
}
