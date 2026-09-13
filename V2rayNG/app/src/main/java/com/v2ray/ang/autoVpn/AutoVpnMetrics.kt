package com.v2ray.ang.autoVpn

import java.util.concurrent.atomic.AtomicLong

/**
 * Debug counters for the numbers the task asks to observe: how often the tunnel is cycled,
 * how long start and stop take, and how many commands a race made obsolete.
 *
 * Values live in memory only; nothing is written to disk on the event path.
 */
object AutoVpnMetrics {
    val startCount = AtomicLong()
    val stopCount = AtomicLong()
    val startFailureCount = AtomicLong()
    val stopFailureCount = AtomicLong()
    val supersededCount = AtomicLong()
    val lastStartLatencyMs = AtomicLong(-1)
    val lastStopLatencyMs = AtomicLong(-1)
    val lastResolveLatencyMs = AtomicLong(-1)

    fun record(event: AutoVpnEvent) {
        when (event) {
            is AutoVpnEvent.VpnStarted -> {
                startCount.incrementAndGet()
                lastStartLatencyMs.set(event.latencyMs)
            }

            is AutoVpnEvent.VpnStopped -> {
                stopCount.incrementAndGet()
                lastStopLatencyMs.set(event.latencyMs)
            }

            is AutoVpnEvent.VpnStartFailed -> startFailureCount.incrementAndGet()
            is AutoVpnEvent.VpnStopFailed -> stopFailureCount.incrementAndGet()
            is AutoVpnEvent.CommandSuperseded -> supersededCount.incrementAndGet()
            else -> Unit
        }
    }

    fun summary(): String =
        "starts=${startCount.get()} stops=${stopCount.get()} startFailures=${startFailureCount.get()} " +
            "stopFailures=${stopFailureCount.get()} superseded=${supersededCount.get()} " +
            "lastStartMs=${lastStartLatencyMs.get()} lastStopMs=${lastStopLatencyMs.get()} " +
            "lastResolveMs=${lastResolveLatencyMs.get()}"

    fun reset() {
        listOf(startCount, stopCount, startFailureCount, stopFailureCount, supersededCount)
            .forEach { it.set(0) }
        listOf(lastStartLatencyMs, lastStopLatencyMs, lastResolveLatencyMs).forEach { it.set(-1) }
    }
}
