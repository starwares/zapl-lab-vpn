package com.v2ray.ang.autoVpn

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * The only component allowed to start or stop the tunnel automatically.
 *
 * Commands are executed one at a time by a single worker coroutine reading a conflated
 * channel, which is what makes the task's race scenarios safe: while one command runs, a newer
 * desired state simply replaces the queued one, so a burst of app switches can never produce
 * two starts, two stops, a start during a stop, or a late command overwriting a newer decision.
 * Each command carries a monotonically increasing generation for the log.
 *
 * The controller never trusts its own memory for "is the tunnel up": [isTunnelRunning] is asked
 * again before and after every command, so a manual start or stop by the user is picked up.
 */
class VpnLifecycleController(
    private val scope: CoroutineScope,
    private val isTunnelRunning: () -> Boolean,
    private val startTunnel: () -> Unit,
    private val stopTunnel: () -> Unit,
    private val onEvent: (AutoVpnEvent) -> Unit = {},
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    private val awaitTunnel: suspend (expected: Boolean) -> Boolean = { expected ->
        defaultAwait(expected, isTunnelRunning)
    },
) {

    enum class Desired { ON, OFF }

    private data class Command(val desired: Desired, val generation: Long, val immediate: Boolean)

    private val queue = Channel<Command>(Channel.CONFLATED)
    private val generation = AtomicLong()
    private val pending = AtomicReference<Command?>(null)
    private var worker: Job? = null

    @Volatile
    private var state: AutoVpnPolicyEngine.VpnState = AutoVpnPolicyEngine.VpnState.OFF

    @Volatile
    private var busy = false

    fun start() {
        if (worker != null) return
        worker = scope.launch {
            for (command in queue) {
                pending.compareAndSet(command, null)
                execute(command)
            }
        }
    }

    fun stop() {
        worker?.cancel()
        worker = null
        queue.close()
    }

    /**
     * Reports the tunnel state, reconciled with the daemon while no command is in flight so the
     * UI and the policy engine never see a stale value after a manual start or stop.
     */
    fun currentState(): AutoVpnPolicyEngine.VpnState {
        if (busy) return state
        val running = isTunnelRunning()
        if (running && state != AutoVpnPolicyEngine.VpnState.ON) state = AutoVpnPolicyEngine.VpnState.ON
        if (!running && state == AutoVpnPolicyEngine.VpnState.ON) state = AutoVpnPolicyEngine.VpnState.OFF
        return state
    }

    fun latestGeneration(): Long = generation.get()

    /** Enqueues a desired state; an unconsumed older command is reported as superseded. */
    fun request(desired: Desired, immediate: Boolean = false): Long {
        val command = Command(desired, generation.incrementAndGet(), immediate)
        pending.getAndSet(command)?.let { onEvent(AutoVpnEvent.CommandSuperseded(it.generation)) }
        queue.trySend(command)
        return command.generation
    }

    private suspend fun execute(command: Command) {
        busy = true
        try {
            when (command.desired) {
                Desired.ON -> executeStart(command)
                Desired.OFF -> executeStop(command)
            }
        } finally {
            busy = false
        }
    }

    private suspend fun executeStart(command: Command) {
        if (isTunnelRunning()) {
            state = AutoVpnPolicyEngine.VpnState.ON
            return
        }
        state = AutoVpnPolicyEngine.VpnState.STARTING
        onEvent(AutoVpnEvent.VpnStartRequested(command.generation))
        val startedAt = nowMs()
        startTunnel()
        val ok = awaitTunnel(true)
        val latency = nowMs() - startedAt
        state = if (ok) AutoVpnPolicyEngine.VpnState.ON else AutoVpnPolicyEngine.VpnState.ERROR
        onEvent(
            if (ok) AutoVpnEvent.VpnStarted(command.generation, latency)
            else AutoVpnEvent.VpnStartFailed(command.generation, latency)
        )
    }

    private suspend fun executeStop(command: Command) {
        if (!isTunnelRunning()) {
            state = AutoVpnPolicyEngine.VpnState.OFF
            return
        }
        state = AutoVpnPolicyEngine.VpnState.STOPPING
        onEvent(AutoVpnEvent.VpnStopRequested(command.generation, command.immediate))
        val startedAt = nowMs()
        stopTunnel()
        val ok = awaitTunnel(false)
        val latency = nowMs() - startedAt
        state = if (ok) AutoVpnPolicyEngine.VpnState.OFF else AutoVpnPolicyEngine.VpnState.ERROR
        onEvent(
            if (ok) AutoVpnEvent.VpnStopped(command.generation, latency)
            else AutoVpnEvent.VpnStopFailed(command.generation, latency)
        )
    }

    companion object {
        const val AWAIT_TIMEOUT_MS = 15_000L
        private const val AWAIT_STEP_MS = 50L

        /**
         * Polls the daemon until it reports the expected state. A bounded wait is required
         * because the native stop is asynchronous; an unbounded one would pin the worker and
         * block every later command.
         */
        private suspend fun defaultAwait(expected: Boolean, isRunning: () -> Boolean): Boolean {
            var waited = 0L
            while (waited < AWAIT_TIMEOUT_MS) {
                if (isRunning() == expected) return true
                delay(AWAIT_STEP_MS)
                waited += AWAIT_STEP_MS
            }
            return isRunning() == expected
        }
    }
}
