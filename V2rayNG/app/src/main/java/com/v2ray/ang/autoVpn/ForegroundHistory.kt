package com.v2ray.ang.autoVpn

/**
 * Reconstructs which activities are on screen from usage history.
 *
 * Two gaps are closed with it. The monitor only learns about transitions, so when it starts
 * while the user is already inside an app (after a reboot, an APK update, or the process being
 * killed) there is no new event to react to. And in split-screen both panes stay `RESUMED`
 * while focus moves between them without any usage event, so the set of still-resumed
 * activities is the only visible trace of the second pane.
 */
object ForegroundHistory {

    enum class Kind { RESUMED, PAUSED, STOPPED, DEVICE_BOUNDARY }

    data class ActivityEvent(
        val kind: Kind,
        val packageName: String,
        val className: String?,
        val atMs: Long,
    )

    /**
     * Activities that were resumed and neither paused nor stopped afterwards.
     *
     * A picture-in-picture window is paused, so it never counts. A device shutdown or startup
     * ends every earlier activity, because the system does not always record their pause before
     * power-off.
     */
    class ResumedTracker {
        private val open = LinkedHashMap<String, ActivityEvent>()

        /** @return true when the set of resumed packages changed. */
        fun apply(event: ActivityEvent): Boolean {
            val before = packages()
            val key = "${event.packageName}/${event.className}"
            when (event.kind) {
                Kind.RESUMED -> {
                    open.remove(key)
                    open[key] = event
                }

                Kind.PAUSED, Kind.STOPPED -> open.remove(key)
                Kind.DEVICE_BOUNDARY -> open.clear()
            }
            return packages() != before
        }

        fun packages(): Set<String> = open.values.mapTo(LinkedHashSet()) { it.packageName }

        /** The most recently resumed activity that is still open. */
        fun current(): ActivityEvent? = open.values.maxByOrNull { it.atMs }

        fun clear() = open.clear()
    }

    fun currentlyResumed(events: List<ActivityEvent>): ActivityEvent? {
        val tracker = ResumedTracker()
        events.sortedBy { it.atMs }.forEach { tracker.apply(it) }
        return tracker.current()
    }
}
