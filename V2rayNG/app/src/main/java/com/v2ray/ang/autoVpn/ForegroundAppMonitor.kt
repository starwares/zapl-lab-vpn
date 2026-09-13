package com.v2ray.ang.autoVpn

import android.app.KeyguardManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Collects Android foreground signals. It decides nothing about the VPN.
 *
 * `UsageStatsManager.queryEvents()` is the only source that reports the resumed activity of a
 * foreign app without Accessibility or root. There is no callback for it, so the event history
 * is read on a low-frequency tick; the tick exists only while the screen is interactive, and
 * the coroutine suspends on the screen-state flow while it is off, so a locked device produces
 * no wakeups at all.
 */
class ForegroundAppMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSignal: (Signal) -> Unit,
    private val intervalMs: Long = DEFAULT_INTERVAL_MS,
) {

    sealed interface Signal {
        /** One observed `ACTIVITY_RESUMED`; [className] is the resumed activity. */
        data class Resumed(val packageName: String, val className: String?, val atMs: Long) : Signal

        /** Packages whose activities are still resumed; two of them mean split-screen. */
        data class VisibleApps(val packageNames: Set<String>) : Signal

        /** Nothing new happened; lets the resolver promote a stabilized candidate. */
        data class Tick(val atMs: Long) : Signal

        /** Screen became usable or unusable; `locked` is true while the keyguard is shown. */
        data class ScreenState(val interactive: Boolean, val locked: Boolean) : Signal
    }

    private val usage = context.getSystemService(UsageStatsManager::class.java)
    private val power = context.getSystemService(PowerManager::class.java)
    private val keyguard = context.getSystemService(KeyguardManager::class.java)
    private val usable = MutableStateFlow(false)
    private var job: Job? = null
    private val tracker = ForegroundHistory.ResumedTracker()

    /** 0 forces a history reconstruction on the next poll; written by the screen receiver. */
    @Volatile
    private var lastQueryMs = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> publishScreenState()
                Intent.ACTION_SCREEN_OFF -> publishScreenState()
            }
        }
    }

    fun start() {
        if (job != null) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(context, screenReceiver, filter, Utils.receiverFlags())
        publishScreenState()
        job = scope.launch {
            while (isActive) {
                // Suspends instead of ticking while the device is off or locked.
                if (!usable.value) usable.first { it }
                poll()
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        try {
            context.unregisterReceiver(screenReceiver)
        } catch (e: IllegalArgumentException) {
            LogUtil.w(AppConfig.TAG, "AutoVpn: screen receiver was not registered", e)
        }
    }

    private fun publishScreenState() {
        val interactive = power?.isInteractive != false
        val locked = keyguard?.isKeyguardLocked == true
        val nowUsable = interactive && !locked
        // Pause and stop events fired while polling was suspended are not replayed by the short
        // lookback, so the resumed set is rebuilt from history once the screen is usable again.
        if (nowUsable && !usable.value) lastQueryMs = 0L
        usable.value = nowUsable
        onSignal(Signal.ScreenState(interactive, locked))
    }

    private fun poll() {
        if (lastQueryMs == 0L) {
            restoreCurrentApp()
            return
        }
        val now = System.currentTimeMillis()
        // After a long suspension only look back far enough to catch the switch that woke the
        // screen; a wider window would replay stale history as if it just happened.
        val from = lastQueryMs.coerceAtLeast(now - LOOKBACK_MS)
        val events = try {
            usage?.queryEvents(from, now)
        } catch (e: SecurityException) {
            LogUtil.w(AppConfig.TAG, "AutoVpn: usage access is not granted", e)
            null
        } ?: return
        lastQueryMs = now

        val event = UsageEvents.Event()
        var emitted = false
        var visibleChanged = false
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val activityEvent = event.toActivityEvent() ?: continue
            if (tracker.apply(activityEvent)) visibleChanged = true
            if (activityEvent.kind != ForegroundHistory.Kind.RESUMED) continue
            emitted = true
            onSignal(Signal.Resumed(activityEvent.packageName, activityEvent.className, activityEvent.atMs))
        }
        if (visibleChanged) {
            onSignal(Signal.VisibleApps(tracker.packages()))
            // A pane that closes in split-screen produces no resume event for the pane that
            // stays, so the remaining activity is reported as the current one.
            if (!emitted) {
                tracker.current()?.let {
                    emitted = true
                    onSignal(Signal.Resumed(it.packageName, it.className, it.atMs))
                }
            }
        }
        if (!emitted) onSignal(Signal.Tick(now))
    }

    private fun UsageEvents.Event.toActivityEvent(): ForegroundHistory.ActivityEvent? {
        val kind = when (eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> ForegroundHistory.Kind.RESUMED
            UsageEvents.Event.ACTIVITY_PAUSED -> ForegroundHistory.Kind.PAUSED
            UsageEvents.Event.ACTIVITY_STOPPED -> ForegroundHistory.Kind.STOPPED
            UsageEvents.Event.DEVICE_SHUTDOWN,
            UsageEvents.Event.DEVICE_STARTUP -> ForegroundHistory.Kind.DEVICE_BOUNDARY

            else -> return null
        }
        return ForegroundHistory.ActivityEvent(kind, packageName.orEmpty(), className, timeStamp)
    }

    /**
     * First poll of a monitor session, and first poll after the screen was unusable: rebuild the
     * resumed set and find the app already on screen instead of waiting for the next transition,
     * which may never come while the user stays in that app.
     */
    private fun restoreCurrentApp() {
        val now = System.currentTimeMillis()
        val events = try {
            usage?.queryEvents(now - RESTORE_LOOKBACK_MS, now)
        } catch (e: SecurityException) {
            LogUtil.w(AppConfig.TAG, "AutoVpn: usage access is not granted", e)
            null
        } ?: return
        lastQueryMs = now

        tracker.clear()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            event.toActivityEvent()?.let { tracker.apply(it) }
        }
        onSignal(Signal.VisibleApps(tracker.packages()))
        val current = tracker.current()
        if (current == null) {
            onSignal(Signal.Tick(now))
            return
        }
        onSignal(Signal.Resumed(current.packageName, current.className, current.atMs))
    }

    companion object {
        /**
         * 500 ms is the task's stated ceiling for a non-aggressive monitor: it bounds the
         * detection delay at roughly one tick while staying far away from the 50-100 ms polling
         * the task forbids.
         */
        const val DEFAULT_INTERVAL_MS = 500L
        private const val LOOKBACK_MS = 5_000L

        /**
         * History replayed once when a monitor session starts. An hour covers a restart while
         * the user stays inside one app; older history is not a reliable picture of the screen.
         */
        private const val RESTORE_LOOKBACK_MS = 60 * 60 * 1000L
    }
}
