package com.v2ray.ang.autoVpn

/**
 * Structured events emitted by the Auto VPN pipeline.
 *
 * They carry package names and generations only; configuration, credentials and server data
 * must never reach this channel.
 */
sealed interface AutoVpnEvent {
    data class ForegroundCandidateChanged(val packageName: String) : AutoVpnEvent
    data class PrimaryForegroundResolved(val packageName: String?) : AutoVpnEvent
    data class PolicyResolved(val packageName: String?, val policy: AppVpnPolicy?) : AutoVpnEvent
    data class VpnStartRequested(val generation: Long) : AutoVpnEvent
    data class VpnStarted(val generation: Long, val latencyMs: Long) : AutoVpnEvent
    data class VpnStartFailed(val generation: Long, val latencyMs: Long) : AutoVpnEvent
    data class VpnStopRequested(val generation: Long, val immediate: Boolean) : AutoVpnEvent
    data class VpnStopped(val generation: Long, val latencyMs: Long) : AutoVpnEvent
    data class VpnStopFailed(val generation: Long, val latencyMs: Long) : AutoVpnEvent
    data class CommandSuperseded(val generation: Long) : AutoVpnEvent
    data class ScreenStateChanged(val interactive: Boolean, val locked: Boolean) : AutoVpnEvent
    data class VisibleAppsChanged(val packageNames: Set<String>) : AutoVpnEvent
}
