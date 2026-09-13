package com.v2ray.ang.autoVpn

/**
 * Answers "which app is the user really working with right now?".
 *
 * Android has no public callback for window focus of a foreign app, so the input is the last
 * `ACTIVITY_RESUMED` package reported by [ForegroundAppMonitor]. This class removes the
 * packages that resume on top of the real app without the user leaving it (System UI, the
 * keyboard, permission dialogs, installers, this app itself) and requires a candidate to stay
 * stable for [stabilizationMs] before it replaces the primary app, which absorbs the short
 * transition bursts produced by launcher animations and task switches.
 *
 * The class is deliberately free of Android types: the package sets are resolved once by
 * [AndroidForegroundFilters] and injected, so every rule is covered by a JVM test.
 */
class PrimaryForegroundAppResolver(
    private val ownPackage: String,
    launcherActivities: Set<String> = emptySet(),
    transientPackages: Set<String> = emptySet(),
    private val transientPrefixes: List<String> = DEFAULT_TRANSIENT_PREFIXES,
    private val stabilizationMs: Long = DEFAULT_STABILIZATION_MS,
) {

    /**
     * Home activities as `package/fully.qualified.Class`. Matching the activity instead of the
     * package matters: Android's Settings package declares a boot-time `FallbackHome`, so a
     * package-level rule turned every opened Settings screen into "no active app".
     */
    private var launcherActivities: Set<String> = launcherActivities
    private var transientPackages: Set<String> = transientPackages

    /**
     * @param primary package of the primary app, or null for `NO_ACTIVE_APP` (launcher, or
     *   nothing attributable yet).
     * @param changed true only when this call moved the primary app.
     */
    data class Resolution(val primary: String?, val changed: Boolean)

    private var primary: String? = null
    private var pending: String? = null
    private var pendingIsLauncher = false
    private var pendingSinceMs = 0L
    private var hasPending = false

    fun current(): String? = primary

    /**
     * Replaces the queried package sets without losing the current primary app. A keyboard or a
     * launcher installed after the monitor started would otherwise be treated as a regular app
     * and could drop the tunnel while the user is only typing.
     */
    fun updateFilters(launcherActivities: Set<String>, transientPackages: Set<String>) {
        this.launcherActivities = launcherActivities
        this.transientPackages = transientPackages
    }

    /** Feeds one observed `ACTIVITY_RESUMED` event. */
    fun onResumed(packageName: String, className: String?, nowMs: Long): Resolution {
        if (isTransient(packageName)) return Resolution(primary, false)

        val isLauncher = className != null && "$packageName/$className" in launcherActivities
        val target = if (isLauncher) null else packageName
        if (target == primary && !hasPending) return Resolution(primary, false)

        if (!hasPending || pending != packageName) {
            pending = packageName
            pendingIsLauncher = isLauncher
            pendingSinceMs = nowMs
            hasPending = true
        }
        return onTick(nowMs)
    }

    /** Lets time pass so a candidate that stayed stable long enough can be promoted. */
    fun onTick(nowMs: Long): Resolution {
        if (!hasPending) return Resolution(primary, false)
        if (nowMs - pendingSinceMs < stabilizationMs) return Resolution(primary, false)

        val resolved = if (pendingIsLauncher) null else pending
        hasPending = false
        pending = null
        if (resolved == primary) return Resolution(primary, false)
        primary = resolved
        return Resolution(primary, true)
    }

    /** Drops System UI, keyboards, dialogs and this app from a set of resumed packages. */
    fun userFacing(packageNames: Set<String>): Set<String> =
        packageNames.filterTo(LinkedHashSet()) { !isTransient(it) }

    /** Forgets the primary app, e.g. after the screen was locked. */
    fun reset(): Resolution {
        hasPending = false
        pending = null
        if (primary == null) return Resolution(null, false)
        primary = null
        return Resolution(null, true)
    }

    private fun isTransient(packageName: String): Boolean =
        packageName == ownPackage ||
            packageName in transientPackages ||
            transientPrefixes.any { packageName.startsWith(it) }

    companion object {
        const val DEFAULT_STABILIZATION_MS = 250L

        /**
         * Packages that draw over the real app without the user leaving it. Prefixes are used
         * because OEMs ship their own System UI and input-method variants; the exact package
         * lists that can be queried are added on top by [AndroidForegroundFilters].
         */
        val DEFAULT_TRANSIENT_PREFIXES = listOf(
            "com.android.systemui",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.settings.intelligence",
            "com.android.intentresolver",
        )
    }
}
