package com.v2ray.ang.autoVpn

/**
 * Pure reducer that turns an observed situation into exactly one VPN action.
 *
 * It has no Android, service or storage dependency so every rule in the task's acceptance
 * criteria can be asserted by a JVM test. It never starts or stops anything itself.
 */
class AutoVpnPolicyEngine(
    private val idleTimeoutMs: Long = DEFAULT_IDLE_TIMEOUT_MS,
) {

    enum class VpnState { OFF, STARTING, ON, STOPPING, ERROR }

    enum class Action { START, STOP, KEEP, STOP_IMMEDIATELY }

    /**
     * @param policy policy of the primary app, or null when there is no user-facing app
     *   (launcher, locked screen, or a package the resolver could not attribute).
     * @param vpnNotNeededSinceMs first moment at which no `VPN` app was primary any more, or
     *   null while a `VPN` app is primary. The idle delay is measured from this moment, so
     *   repeated events inside the same non-VPN app do not postpone the stop.
     */
    data class Situation(
        val vpnState: VpnState,
        val policy: AppVpnPolicy?,
        val nowMs: Long,
        val vpnNotNeededSinceMs: Long?,
        val screenInteractive: Boolean = true,
    )

    fun decide(situation: Situation): Action = when {
        situation.policy == AppVpnPolicy.VPN_FORBIDDEN -> stopImmediatelyIfAnyTunnel(situation.vpnState)
        situation.policy == AppVpnPolicy.VPN -> startIfNoTunnel(situation)
        else -> stopWhenIdleExpired(situation)
    }

    /** Remaining idle delay, so the caller can re-evaluate exactly once at the deadline. */
    fun idleDelayLeftMs(situation: Situation): Long {
        val since = situation.vpnNotNeededSinceMs ?: return idleTimeoutMs
        return (since + idleTimeoutMs - situation.nowMs).coerceAtLeast(0L)
    }

    private fun stopImmediatelyIfAnyTunnel(state: VpnState): Action =
        if (state == VpnState.OFF || state == VpnState.STOPPING) Action.KEEP else Action.STOP_IMMEDIATELY

    private fun startIfNoTunnel(situation: Situation): Action = when {
        // No user is present, so nothing was really opened; do not create a tunnel.
        !situation.screenInteractive -> Action.KEEP
        situation.vpnState == VpnState.ON || situation.vpnState == VpnState.STARTING -> Action.KEEP
        else -> Action.START
    }

    private fun stopWhenIdleExpired(situation: Situation): Action = when {
        situation.vpnState == VpnState.OFF || situation.vpnState == VpnState.STOPPING -> Action.KEEP
        situation.vpnNotNeededSinceMs == null -> Action.KEEP
        situation.nowMs - situation.vpnNotNeededSinceMs >= idleTimeoutMs -> Action.STOP
        else -> Action.KEEP
    }

    companion object {
        const val DEFAULT_IDLE_TIMEOUT_MS = 10_000L

        /**
         * Policy the decision is made with.
         *
         * Android reports no usage event when focus moves between split-screen panes, so the
         * pane the user touches cannot be known. The fallback treats every visible pane as
         * possibly in use: a visible "always turn VPN off" app wins because it must never see
         * the tunnel, and otherwise a visible "through VPN" app keeps it. A picture-in-picture
         * window is paused and therefore never part of [visiblePolicies].
         */
        fun effectivePolicy(
            primaryPolicy: AppVpnPolicy?,
            visiblePolicies: Collection<AppVpnPolicy>,
            screenUsable: Boolean,
            keepWhileScreenOff: Boolean,
        ): AppVpnPolicy? = when {
            !screenUsable && !keepWhileScreenOff -> null
            !screenUsable -> primaryPolicy
            AppVpnPolicy.VPN_FORBIDDEN in visiblePolicies -> AppVpnPolicy.VPN_FORBIDDEN
            AppVpnPolicy.VPN in visiblePolicies -> AppVpnPolicy.VPN
            else -> primaryPolicy
        }

        /**
         * Moment from which the idle delay runs after the effective policy may have changed (a
         * new primary app or a pane opened or closed): `VPN` clears it, anything else keeps an
         * already running delay or starts it at [atMs].
         */
        fun notNeededSinceAfterPolicyChange(
            policy: AppVpnPolicy?,
            previousSinceMs: Long?,
            atMs: Long,
        ): Long? = if (policy == AppVpnPolicy.VPN) null else previousSinceMs ?: atMs

        /**
         * Moment from which the idle delay runs after the screen became usable or unusable.
         *
         * Turning the screen back on while a `VPN` app is primary must clear the delay started by
         * the previous screen-off; otherwise the next screen-off measures from that old moment
         * and stops the tunnel at once instead of after the delay.
         */
        fun notNeededSinceAfterScreenChange(
            usable: Boolean,
            keepWhileScreenOff: Boolean,
            policy: AppVpnPolicy?,
            previousSinceMs: Long?,
            nowMs: Long,
        ): Long? = when {
            usable -> if (policy == AppVpnPolicy.VPN) null else previousSinceMs
            keepWhileScreenOff -> previousSinceMs
            else -> previousSinceMs ?: nowMs
        }
    }
}
