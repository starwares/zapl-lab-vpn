package com.v2ray.ang.autoVpn

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class VpnLifecycleControllerTest {

    /** Stands in for the daemon: records commands and flips the reported state itself. */
    private class FakeTunnel {
        val running = AtomicBoolean(false)
        val starts = AtomicInteger()
        val stops = AtomicInteger()

        fun start() {
            starts.incrementAndGet()
            running.set(true)
        }

        fun stop() {
            stops.incrementAndGet()
            running.set(false)
        }
    }

    private fun runTest(block: suspend CoroutineScope.(FakeTunnel, VpnLifecycleController, List<AutoVpnEvent>) -> Unit) =
        runBlocking {
            val tunnel = FakeTunnel()
            val events = Collections.synchronizedList(mutableListOf<AutoVpnEvent>())
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val controller = VpnLifecycleController(
                scope = scope,
                isTunnelRunning = { tunnel.running.get() },
                startTunnel = { tunnel.start() },
                stopTunnel = { tunnel.stop() },
                onEvent = { events.add(it) },
                awaitTunnel = { expected -> tunnel.running.get() == expected },
            )
            controller.start()
            try {
                block(tunnel, controller, events)
            } finally {
                controller.stop()
                scope.cancel()
            }
        }

    private suspend fun awaitEvent(events: List<AutoVpnEvent>, predicate: (AutoVpnEvent) -> Boolean) {
        withTimeout(2_000) {
            while (events.none(predicate)) delay(5)
        }
    }

    // AC-11: a repeated identical command must not create a second tunnel.
    @Test
    fun equivalentStartCommandsProduceOneStart() = runTest { tunnel, controller, events ->
        controller.request(VpnLifecycleController.Desired.ON)
        awaitEvent(events) { it is AutoVpnEvent.VpnStarted }
        controller.request(VpnLifecycleController.Desired.ON)
        delay(100)

        assertEquals(1, tunnel.starts.get())
        assertEquals(0, tunnel.stops.get())
        assertEquals(AutoVpnPolicyEngine.VpnState.ON, controller.currentState())
    }

    @Test
    fun stopIsSkippedWhenNothingIsRunning() = runTest { tunnel, controller, _ ->
        controller.request(VpnLifecycleController.Desired.OFF)
        delay(100)

        assertEquals(0, tunnel.stops.get())
        assertEquals(AutoVpnPolicyEngine.VpnState.OFF, controller.currentState())
    }

    // Section 15: YouTube -> Telegram -> YouTube -> launcher -> bank -> YouTube within seconds.
    @Test
    fun aBurstOfDesiredStatesEndsInTheLastOne() = runTest { tunnel, controller, events ->
        repeat(3) {
            controller.request(VpnLifecycleController.Desired.ON)
            controller.request(VpnLifecycleController.Desired.OFF)
        }
        controller.request(VpnLifecycleController.Desired.ON)
        awaitEvent(events) { it is AutoVpnEvent.VpnStarted }
        delay(150)

        assertTrue(tunnel.running.get())
        assertEquals(AutoVpnPolicyEngine.VpnState.ON, controller.currentState())
        assertTrue("superseded commands must be reported", events.any { it is AutoVpnEvent.CommandSuperseded })
        assertTrue("a burst must not start the tunnel once per event", tunnel.starts.get() <= 2)
    }

    @Test
    fun commandsAreSerializedSoAStopNeverOverlapsAStart() = runTest { tunnel, controller, events ->
        val inFlight = AtomicInteger()
        val overlapped = AtomicBoolean(false)
        val gate = CompletableDeferred<Unit>()
        val slowController = VpnLifecycleController(
            scope = this,
            isTunnelRunning = { tunnel.running.get() },
            startTunnel = { tunnel.start() },
            stopTunnel = { tunnel.stop() },
            awaitTunnel = { expected ->
                if (inFlight.incrementAndGet() > 1) overlapped.set(true)
                gate.await()
                inFlight.decrementAndGet()
                tunnel.running.get() == expected
            },
        )
        slowController.start()
        slowController.request(VpnLifecycleController.Desired.ON)
        slowController.request(VpnLifecycleController.Desired.OFF)
        delay(100)
        gate.complete(Unit)
        delay(200)
        slowController.stop()

        assertTrue("a second command must wait for the first to finish", !overlapped.get())
        assertEquals(0, controller.latestGeneration().toInt())
    }

    @Test
    fun generationGrowsWithEveryAcceptedCommand() = runTest { _, controller, _ ->
        assertEquals(1L, controller.request(VpnLifecycleController.Desired.ON))
        assertEquals(2L, controller.request(VpnLifecycleController.Desired.OFF))
        assertEquals(2L, controller.latestGeneration())
    }

    // A manual start or stop by the user must be picked up instead of a remembered value.
    @Test
    fun currentStateFollowsTheDaemon() = runTest { tunnel, controller, _ ->
        assertEquals(AutoVpnPolicyEngine.VpnState.OFF, controller.currentState())
        tunnel.running.set(true)
        assertEquals(AutoVpnPolicyEngine.VpnState.ON, controller.currentState())
        tunnel.running.set(false)
        assertEquals(AutoVpnPolicyEngine.VpnState.OFF, controller.currentState())
    }
}
