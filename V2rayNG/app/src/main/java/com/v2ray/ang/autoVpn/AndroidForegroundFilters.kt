package com.v2ray.ang.autoVpn

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.inputmethod.InputMethodManager
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil

/**
 * Resolves, once per monitor session, the package sets [PrimaryForegroundAppResolver] needs.
 *
 * Querying the package manager is expensive, so it is done when the monitor starts and never
 * on the event path.
 */
object AndroidForegroundFilters {

    /**
     * Every real Home activity as `package/Class`; resuming one of them means `NO_ACTIVE_APP`.
     *
     * AOSP Settings ships `FallbackHome` with a negative priority as a placeholder shown until
     * the launcher is ready after boot; it is excluded so the Settings app stays a normal app.
     * An activity-alias is recorded under its target too, because usage events report the
     * class that actually runs.
     */
    fun launcherActivities(context: Context): Set<String> = try {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        context.packageManager.queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY)
            .filter { it.priority >= 0 }
            .mapNotNull { it.activityInfo }
            .flatMap { info ->
                listOfNotNull(info.name, info.targetActivity).map { "${info.packageName}/$it" }
            }
            .toSet()
    } catch (e: Exception) {
        LogUtil.w(AppConfig.TAG, "AutoVpn: failed to resolve launcher packages", e)
        emptySet()
    }

    /** Enabled input methods; a keyboard resuming on top must not move the primary app. */
    fun keyboardPackages(context: Context): Set<String> = try {
        val manager = context.getSystemService(InputMethodManager::class.java)
        manager?.enabledInputMethodList.orEmpty().map { it.packageName }.toSet()
    } catch (e: Exception) {
        LogUtil.w(AppConfig.TAG, "AutoVpn: failed to resolve keyboard packages", e)
        emptySet()
    }

    fun newResolver(context: Context): PrimaryForegroundAppResolver = PrimaryForegroundAppResolver(
        ownPackage = context.packageName,
        launcherActivities = launcherActivities(context),
        transientPackages = keyboardPackages(context),
    )

    fun refresh(context: Context, resolver: PrimaryForegroundAppResolver) {
        resolver.updateFilters(launcherActivities(context), keyboardPackages(context))
    }
}
