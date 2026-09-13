package com.v2ray.ang.autoVpn

import com.v2ray.ang.autoVpn.AutoVpnPolicyEngine.Action
import com.v2ray.ang.autoVpn.AutoVpnPolicyEngine.Situation
import com.v2ray.ang.autoVpn.AutoVpnPolicyEngine.VpnState
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoVpnPolicyEngineTest {

    private val engine = AutoVpnPolicyEngine(idleTimeoutMs = 10_000L)

    private fun situation(
        state: VpnState,
        policy: AppVpnPolicy?,
        nowMs: Long = 0L,
        notNeededSince: Long? = null,
        screenInteractive: Boolean = true,
    ) = Situation(state, policy, nowMs, notNeededSince, screenInteractive)

    // AC-1: the tunnel is off and the user opens an app that must go through it.
    @Test
    fun startsWhenVpnAppBecomesPrimary() {
        assertEquals(Action.START, engine.decide(situation(VpnState.OFF, AppVpnPolicy.VPN)))
        assertEquals(Action.START, engine.decide(situation(VpnState.ERROR, AppVpnPolicy.VPN)))
        assertEquals(Action.START, engine.decide(situation(VpnState.STOPPING, AppVpnPolicy.VPN)))
    }

    // AC-8: returning to the same app must not restart an already running tunnel.
    @Test
    fun keepsRunningTunnelForVpnApp() {
        assertEquals(Action.KEEP, engine.decide(situation(VpnState.ON, AppVpnPolicy.VPN)))
        assertEquals(Action.KEEP, engine.decide(situation(VpnState.STARTING, AppVpnPolicy.VPN)))
    }

    // AC-2 and AC-6: a forbidden app must stop the tunnel with no delay, including mid-start.
    @Test
    fun forbiddenAppStopsImmediatelyFromEveryLiveState() {
        listOf(VpnState.ON, VpnState.STARTING, VpnState.ERROR).forEach { state ->
            assertEquals(
                "state=$state",
                Action.STOP_IMMEDIATELY,
                engine.decide(situation(state, AppVpnPolicy.VPN_FORBIDDEN)),
            )
        }
    }

    @Test
    fun forbiddenAppKeepsQuietWhenNothingIsRunning() {
        assertEquals(Action.KEEP, engine.decide(situation(VpnState.OFF, AppVpnPolicy.VPN_FORBIDDEN)))
        assertEquals(Action.KEEP, engine.decide(situation(VpnState.STOPPING, AppVpnPolicy.VPN_FORBIDDEN)))
    }

    @Test
    fun directAppWaitsForTheIdleDelay() {
        assertEquals(
            Action.KEEP,
            engine.decide(situation(VpnState.ON, AppVpnPolicy.DIRECT, nowMs = 9_999, notNeededSince = 0)),
        )
        assertEquals(
            Action.STOP,
            engine.decide(situation(VpnState.ON, AppVpnPolicy.DIRECT, nowMs = 10_000, notNeededSince = 0)),
        )
    }

    // Section 18: the launcher is NO_ACTIVE_APP and follows the same idle delay.
    @Test
    fun noActiveAppUsesTheIdleDelay() {
        assertEquals(
            Action.KEEP,
            engine.decide(situation(VpnState.ON, policy = null, nowMs = 5_000, notNeededSince = 0)),
        )
        assertEquals(
            Action.STOP,
            engine.decide(situation(VpnState.ON, policy = null, nowMs = 10_001, notNeededSince = 0)),
        )
    }

    @Test
    fun zeroTimeoutStopsAsSoonAsTheVpnAppIsLeft() {
        val immediate = AutoVpnPolicyEngine(idleTimeoutMs = 0L)
        assertEquals(
            Action.STOP,
            immediate.decide(situation(VpnState.ON, AppVpnPolicy.DIRECT, nowMs = 0, notNeededSince = 0)),
        )
    }

    // Section 23: with the screen off nothing was really opened, so no tunnel is created.
    @Test
    fun doesNotStartWhileTheScreenIsNotUsable() {
        assertEquals(
            Action.KEEP,
            engine.decide(situation(VpnState.OFF, AppVpnPolicy.VPN, screenInteractive = false)),
        )
    }

    @Test
    fun keepsTunnelWhileTheIdleClockHasNotStarted() {
        assertEquals(
            Action.KEEP,
            engine.decide(situation(VpnState.ON, AppVpnPolicy.DIRECT, nowMs = 60_000, notNeededSince = null)),
        )
    }

    // Found on the emulator: VPN app -> screen off -> on -> off stopped the tunnel at once.
    @Test
    fun screenOnClearsTheDelayStartedByThePreviousScreenOff() {
        var since: Long? = null
        since = AutoVpnPolicyEngine.notNeededSinceAfterScreenChange(false, false, AppVpnPolicy.VPN, since, 1_000)
        assertEquals(1_000L, since)
        since = AutoVpnPolicyEngine.notNeededSinceAfterScreenChange(true, false, AppVpnPolicy.VPN, since, 12_000)
        assertEquals(null, since)
        since = AutoVpnPolicyEngine.notNeededSinceAfterScreenChange(false, false, AppVpnPolicy.VPN, since, 40_000)
        assertEquals(40_000L, since)

        assertEquals(
            Action.KEEP,
            engine.decide(situation(VpnState.ON, policy = null, nowMs = 40_001, notNeededSince = since, screenInteractive = false)),
        )
    }

    @Test
    fun screenOffKeepsTheTunnelWhenTheOptionIsOn() {
        assertEquals(
            null,
            AutoVpnPolicyEngine.notNeededSinceAfterScreenChange(false, true, AppVpnPolicy.VPN, null, 5_000),
        )
    }

    @Test
    fun screenOnDoesNotResetTheDelayOfANonVpnApp() {
        assertEquals(
            3_000L,
            AutoVpnPolicyEngine.notNeededSinceAfterScreenChange(true, false, AppVpnPolicy.DIRECT, 3_000, 9_000),
        )
    }

    @Test
    fun primaryChangeStartsOrKeepsTheDelay() {
        assertEquals(null, AutoVpnPolicyEngine.notNeededSinceAfterPolicyChange(AppVpnPolicy.VPN, 100, 500))
        assertEquals(500L, AutoVpnPolicyEngine.notNeededSinceAfterPolicyChange(AppVpnPolicy.DIRECT, null, 500))
        assertEquals(100L, AutoVpnPolicyEngine.notNeededSinceAfterPolicyChange(null, 100, 500))
    }

    // Section 21: focus between split panes is invisible, so a visible forbidden app wins.
    @Test
    fun visibleForbiddenAppOverridesAVpnPrimaryInSplitScreen() {
        val policy = AutoVpnPolicyEngine.effectivePolicy(
            primaryPolicy = AppVpnPolicy.VPN,
            visiblePolicies = listOf(AppVpnPolicy.VPN, AppVpnPolicy.VPN_FORBIDDEN),
            screenUsable = true,
            keepWhileScreenOff = false,
        )

        assertEquals(AppVpnPolicy.VPN_FORBIDDEN, policy)
        assertEquals(Action.STOP_IMMEDIATELY, engine.decide(situation(VpnState.ON, policy)))
    }

    @Test
    fun visibleDirectAppDoesNotOverrideTheVpnPrimary() {
        assertEquals(
            AppVpnPolicy.VPN,
            AutoVpnPolicyEngine.effectivePolicy(AppVpnPolicy.VPN, listOf(AppVpnPolicy.DIRECT), true, false),
        )
    }

    // Observed on the emulator: Chrome + Clock in split, Clock resumed last, the tunnel dropped
    // while Chrome was still on half of the screen.
    @Test
    fun visibleVpnPaneKeepsTheTunnelWhenAnotherPaneResumedLast() {
        assertEquals(
            AppVpnPolicy.VPN,
            AutoVpnPolicyEngine.effectivePolicy(
                primaryPolicy = AppVpnPolicy.DIRECT,
                visiblePolicies = listOf(AppVpnPolicy.VPN, AppVpnPolicy.DIRECT),
                screenUsable = true,
                keepWhileScreenOff = false,
            ),
        )
    }

    @Test
    fun closingTheVpnPaneStartsTheIdleDelayAtThatMoment() {
        var since = AutoVpnPolicyEngine.notNeededSinceAfterPolicyChange(AppVpnPolicy.VPN, null, 1_000)
        assertEquals(null, since)
        since = AutoVpnPolicyEngine.notNeededSinceAfterPolicyChange(AppVpnPolicy.DIRECT, since, 60_000)
        assertEquals(60_000L, since)
        assertEquals(
            Action.KEEP,
            engine.decide(situation(VpnState.ON, AppVpnPolicy.DIRECT, nowMs = 60_001, notNeededSince = since)),
        )
    }

    @Test
    fun screenOffWithoutTheOptionMeansNoActiveApp() {
        assertEquals(
            null,
            AutoVpnPolicyEngine.effectivePolicy(AppVpnPolicy.VPN, listOf(AppVpnPolicy.VPN), false, false),
        )
        assertEquals(
            AppVpnPolicy.VPN,
            AutoVpnPolicyEngine.effectivePolicy(AppVpnPolicy.VPN, listOf(AppVpnPolicy.VPN), false, true),
        )
    }

    @Test
    fun reportsRemainingIdleDelay() {
        val situation = situation(VpnState.ON, AppVpnPolicy.DIRECT, nowMs = 4_000, notNeededSince = 0)
        assertEquals(6_000L, engine.idleDelayLeftMs(situation))
        assertEquals(0L, engine.idleDelayLeftMs(situation.copy(nowMs = 20_000)))
    }
}
