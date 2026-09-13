package com.v2ray.ang.autoVpn

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.helper.MessageHelper
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Wires the Android signals to the policy engine and to the single lifecycle controller.
 *
 * It runs inside the daemon process, which the service guide names as the only authority for
 * whether the native core is running, so the tunnel state is read from [CoreServiceManager]
 * instead of being remembered. The coordinator itself never calls a service: starts go through
 * [LauncherManager] and stops through [MessageHelper], exactly like the manual UI path.
 */
class AutoVpnCoordinator(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Signals arrive from two threads: the polling coroutine and the screen broadcast receiver
     * on the main thread. Every read and write of the decision state happens under this lock so
     * the engine always sees one consistent situation.
     */
    private val lock = Any()
    private lateinit var resolver: PrimaryForegroundAppResolver
    private var monitor: ForegroundAppMonitor? = null
    private var idleJob: Job? = null

    private var engine = AutoVpnPolicyEngine()
    private var idleTimeoutMs = AutoVpnPolicyEngine.DEFAULT_IDLE_TIMEOUT_MS
    private var keepWhileScreenOff = false

    private var primary: String? = null
    private var visiblePackages: Set<String> = emptySet()
    private var vpnNotNeededSinceMs: Long? = System.currentTimeMillis()
    private var screenUsable = true

    private val lifecycle = VpnLifecycleController(
        scope = scope,
        isTunnelRunning = { CoreServiceManager.isRunning() },
        startTunnel = { LauncherManager.startService(context, notifyUser = false) },
        stopTunnel = { MessageHelper.sendMsg2Service(context, AppConfig.MSG_AUTO_VPN_STOP, "") },
        onEvent = { publish(it) },
    )

    fun start(): Boolean = synchronized(lock) {
        when {
            !MmkvManager.decodeSettingsBool(AppConfig.PREF_AUTO_VPN_ENABLED, false) -> {
                LogUtil.i(AppConfig.TAG, "AutoVpn: disabled in settings")
                false
            }

            !hasUsageAccess(context) -> {
                LogUtil.w(AppConfig.TAG, "AutoVpn: usage access is not granted, monitor not started")
                false
            }

            else -> {
                reloadSettings()
                resolver = AndroidForegroundFilters.newResolver(context)
                lifecycle.start()
                monitor = ForegroundAppMonitor(context, scope, ::onSignal).also { it.start() }
                publishStatus()
                LogUtil.i(AppConfig.TAG, "AutoVpn: monitor started, idleTimeoutMs=$idleTimeoutMs")
                true
            }
        }
    }

    fun stop() = synchronized(lock) {
        monitor?.stop()
        monitor = null
        idleJob?.cancel()
        idleJob = null
        lifecycle.stop()
        scope.cancel()
        AutoVpnStatusStore.clear()
        LogUtil.i(AppConfig.TAG, "AutoVpn: monitor stopped, ${AutoVpnMetrics.summary()}")
    }

    /** Applies a settings or policy change without restarting the monitor. */
    fun reload() = synchronized(lock) {
        AutoVpnPolicyRepository.refresh()
        reloadSettings()
        evaluate(System.currentTimeMillis())
    }

    private fun reloadSettings() {
        idleTimeoutMs = MmkvManager.decodeSettingsLong(
            AppConfig.PREF_AUTO_VPN_IDLE_TIMEOUT_MS,
            AutoVpnPolicyEngine.DEFAULT_IDLE_TIMEOUT_MS,
        )
        keepWhileScreenOff = MmkvManager.decodeSettingsBool(AppConfig.PREF_AUTO_VPN_KEEP_SCREEN_OFF, false)
        engine = AutoVpnPolicyEngine(idleTimeoutMs)
    }

    private fun onSignal(signal: ForegroundAppMonitor.Signal) = synchronized(lock) {
        when (signal) {
            is ForegroundAppMonitor.Signal.Resumed -> {
                publish(AutoVpnEvent.ForegroundCandidateChanged(signal.packageName))
                apply(resolver.onResumed(signal.packageName, signal.className, signal.atMs), signal.atMs)
            }

            is ForegroundAppMonitor.Signal.Tick -> apply(resolver.onTick(signal.atMs), signal.atMs)

            is ForegroundAppMonitor.Signal.ScreenState -> onScreenState(signal)

            is ForegroundAppMonitor.Signal.VisibleApps -> onVisibleApps(signal)
        }
    }

    private fun onScreenState(signal: ForegroundAppMonitor.Signal.ScreenState) {
        val usable = signal.interactive && !signal.locked
        if (usable == screenUsable) return
        screenUsable = usable
        // An unlock is the cheapest moment to notice a newly installed keyboard or launcher.
        if (usable) AndroidForegroundFilters.refresh(context, resolver)
        publish(AutoVpnEvent.ScreenStateChanged(signal.interactive, signal.locked))
        val now = System.currentTimeMillis()
        vpnNotNeededSinceMs = AutoVpnPolicyEngine.notNeededSinceAfterScreenChange(
            usable = usable,
            keepWhileScreenOff = keepWhileScreenOff,
            policy = awakePolicy(),
            previousSinceMs = vpnNotNeededSinceMs,
            nowMs = now,
        )
        evaluate(now)
    }

    private fun onVisibleApps(signal: ForegroundAppMonitor.Signal.VisibleApps) {
        val userFacing = resolver.userFacing(signal.packageNames)
        if (userFacing == visiblePackages) return
        visiblePackages = userFacing
        publish(AutoVpnEvent.VisibleAppsChanged(userFacing))
        val now = System.currentTimeMillis()
        vpnNotNeededSinceMs = AutoVpnPolicyEngine.notNeededSinceAfterPolicyChange(
            awakePolicy(), vpnNotNeededSinceMs, now,
        )
        evaluate(now)
    }

    private fun apply(resolution: PrimaryForegroundAppResolver.Resolution, atMs: Long) {
        // A tick that resolved nothing must stay free: the idle deadline is handled by its own
        // timer, so there is nothing to recompute while the user stays inside one app.
        if (!resolution.changed) return

        val now = System.currentTimeMillis()
        AutoVpnMetrics.lastResolveLatencyMs.set((now - atMs).coerceAtLeast(0L))
        primary = resolution.primary
        publish(AutoVpnEvent.PrimaryForegroundResolved(resolution.primary))
        val policy = policyOf(resolution.primary)
        publish(AutoVpnEvent.PolicyResolved(resolution.primary, policy))
        vpnNotNeededSinceMs = AutoVpnPolicyEngine.notNeededSinceAfterPolicyChange(
            awakePolicy(), vpnNotNeededSinceMs, atMs,
        )
        evaluate(now)
    }

    private fun policyOf(packageName: String?): AppVpnPolicy? =
        packageName?.let { AutoVpnPolicyRepository.get(it) }

    private fun evaluate(nowMs: Long) {
        val policy = effectivePolicy()
        val situation = AutoVpnPolicyEngine.Situation(
            vpnState = lifecycle.currentState(),
            policy = policy,
            nowMs = nowMs,
            vpnNotNeededSinceMs = vpnNotNeededSinceMs,
            screenInteractive = screenUsable,
        )
        idleJob?.cancel()
        idleJob = null

        when (engine.decide(situation)) {
            // Only the tunnel mode is started automatically: silently bringing up root mode or a
            // local proxy from the background is not what the user asked this feature to do.
            AutoVpnPolicyEngine.Action.START -> if (isSupportedRunMode()) {
                lifecycle.request(VpnLifecycleController.Desired.ON)
            } else {
                LogUtil.w(AppConfig.TAG, "AutoVpn: start skipped, current run mode is not VPN")
            }
            AutoVpnPolicyEngine.Action.STOP -> lifecycle.request(VpnLifecycleController.Desired.OFF)
            AutoVpnPolicyEngine.Action.STOP_IMMEDIATELY ->
                lifecycle.request(VpnLifecycleController.Desired.OFF, immediate = true)

            AutoVpnPolicyEngine.Action.KEEP -> scheduleIdleRecheck(situation)
        }
        publishStatus()
    }

    /**
     * Re-evaluates once when the idle delay expires. The monitor is silent while the user stays
     * inside one app, so without this timer a "Без VPN" app would keep the tunnel forever.
     */
    private fun scheduleIdleRecheck(situation: AutoVpnPolicyEngine.Situation) {
        if (situation.policy == AppVpnPolicy.VPN) return
        if (situation.vpnNotNeededSinceMs == null) return
        if (situation.vpnState != AutoVpnPolicyEngine.VpnState.ON &&
            situation.vpnState != AutoVpnPolicyEngine.VpnState.STARTING
        ) {
            return
        }
        val left = engine.idleDelayLeftMs(situation)
        idleJob = scope.launch {
            delay(left + IDLE_RECHECK_MARGIN_MS)
            synchronized(lock) { evaluate(System.currentTimeMillis()) }
        }
    }

    /** The policy that applies as soon as someone is looking at the screen. */
    private fun awakePolicy(): AppVpnPolicy? = AutoVpnPolicyEngine.effectivePolicy(
        primaryPolicy = policyOf(primary),
        visiblePolicies = visiblePackages.map { AutoVpnPolicyRepository.get(it) },
        screenUsable = true,
        keepWhileScreenOff = keepWhileScreenOff,
    )

    private fun effectivePolicy(): AppVpnPolicy? = AutoVpnPolicyEngine.effectivePolicy(
        primaryPolicy = policyOf(primary),
        visiblePolicies = visiblePackages.map { AutoVpnPolicyRepository.get(it) },
        screenUsable = screenUsable,
        keepWhileScreenOff = keepWhileScreenOff,
    )

    private fun publish(event: AutoVpnEvent) {
        AutoVpnMetrics.record(event)
        if (BuildConfig.DEBUG) LogUtil.d(AppConfig.TAG, "AutoVpn: $event")
        when (event) {
            is AutoVpnEvent.VpnStarted, is AutoVpnEvent.VpnStopped,
            is AutoVpnEvent.VpnStartFailed, is AutoVpnEvent.VpnStopFailed,
            -> publishStatus()

            else -> Unit
        }
    }

    private fun publishStatus() {
        AutoVpnStatusStore.publish(
            AutoVpnStatus(
                primaryPackage = primary,
                // Shows the rule actually applied, so a split-screen override is not reported
                // as "through VPN" while the tunnel is kept off.
                policy = effectivePolicy() ?: policyOf(primary),
                vpnState = lifecycle.currentState(),
                updatedAtMs = System.currentTimeMillis(),
            )
        )
    }

    companion object {
        private const val IDLE_RECHECK_MARGIN_MS = 50L

        /**
         * `PACKAGE_USAGE_STATS` is a special access granted on a system screen, so it cannot be
         * requested at runtime and must be read back through AppOps.
         */
        fun hasUsageAccess(context: Context): Boolean = try {
            val appOps = context.getSystemService(AppOpsManager::class.java)
            checkUsageStatsOp(appOps, context.packageName) == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            LogUtil.w(AppConfig.TAG, "AutoVpn: failed to read usage access state", e)
            false
        }

        /**
         * `unsafeCheckOpNoThrow` exists only from API 29; below it the identical
         * `checkOpNoThrow` is the sole way to read the op. Remove this branch when minSdk
         * reaches 29.
         */
        @Suppress("DEPRECATION")
        private fun checkUsageStatsOp(appOps: AppOpsManager?, packageName: String): Int? {
            appOps ?: return null
            val uid = Process.myUid()
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, packageName)
            } else {
                appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, packageName)
            }
        }

        /** Auto VPN only makes sense for the tunnel mode; proxy-only mode has no TUN to hide. */
        fun isSupportedRunMode(): Boolean = SettingsManager.isVpnMode() && !SettingsManager.isRootMode()
    }
}
