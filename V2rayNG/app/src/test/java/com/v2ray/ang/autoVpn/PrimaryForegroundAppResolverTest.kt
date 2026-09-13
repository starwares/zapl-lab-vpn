package com.v2ray.ang.autoVpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrimaryForegroundAppResolverTest {

    private val launcher = "com.android.launcher3"
    private val launcherActivity = "com.android.launcher3.Launcher"
    private val launcherComponent = "$launcher/$launcherActivity"
    private val keyboard = "com.example.keyboard"

    private fun resolver(stabilizationMs: Long = 250L) = PrimaryForegroundAppResolver(
        ownPackage = "com.v2ray.ang",
        launcherActivities = setOf(launcherComponent),
        transientPackages = setOf(keyboard),
        stabilizationMs = stabilizationMs,
    )

    private fun PrimaryForegroundAppResolver.open(packageName: String, atMs: Long) =
        onResumed(packageName, "$packageName.MainActivity", atMs)

    @Test
    fun candidateBecomesPrimaryOnlyAfterItStabilizes() {
        val resolver = resolver()
        assertEquals(null, resolver.open("com.example.video", 0).primary)
        assertEquals(null, resolver.onTick(249).primary)

        val resolved = resolver.onTick(250)
        assertEquals("com.example.video", resolved.primary)
        assertTrue(resolved.changed)
    }

    // Section 20 and AC-7: transient system windows must not move the primary app.
    @Test
    fun systemUiKeyboardAndPermissionDialogsKeepThePrimaryApp() {
        val resolver = resolver(stabilizationMs = 0L)
        resolver.open("com.example.bank", 0)

        listOf(
            "com.android.systemui",
            keyboard,
            "com.google.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.android.intentresolver",
            "com.v2ray.ang",
        ).forEach { transient ->
            val result = resolver.open(transient, 100)
            assertEquals("transient=$transient", "com.example.bank", result.primary)
            assertFalse("transient=$transient", result.changed)
        }
    }

    // Section 18: the launcher means NO_ACTIVE_APP, not "the launcher is the active app".
    @Test
    fun launcherResolvesToNoActiveApp() {
        val resolver = resolver(stabilizationMs = 0L)
        resolver.open("com.example.video", 0)

        val home = resolver.onResumed(launcher, launcherActivity, 100)
        assertEquals(null, home.primary)
        assertTrue(home.changed)
    }

    // Found on the emulator: Settings declares a boot-time FallbackHome, but its normal screens
    // are an ordinary app and must keep their own policy.
    @Test
    fun otherActivitiesOfAHomePackageAreARegularApp() {
        val resolver = PrimaryForegroundAppResolver(
            ownPackage = "com.v2ray.ang",
            launcherActivities = setOf(launcherComponent),
            stabilizationMs = 0L,
        )

        val settings = resolver.onResumed(
            "com.android.settings",
            "com.android.settings.homepage.SettingsHomepageActivity",
            0,
        )
        assertEquals("com.android.settings", settings.primary)

        val otherLauncherScreen = resolver.onResumed(launcher, "com.android.launcher3.WallpaperPicker", 100)
        assertEquals(launcher, otherLauncherScreen.primary)
    }

    // Section 16: a burst of switches must settle on the last app, not on the ones passed through.
    @Test
    fun rapidSwitchesResolveToTheLastStableApp() {
        val resolver = resolver(stabilizationMs = 250L)
        resolver.open("com.example.a", 0)
        resolver.open("com.example.b", 60)
        resolver.open("com.example.c", 120)

        assertEquals(null, resolver.onTick(200).primary)
        assertEquals("com.example.c", resolver.onTick(370).primary)
    }

    @Test
    fun repeatedEventsOfTheSameAppDoNotReportAChange() {
        val resolver = resolver(stabilizationMs = 0L)
        assertTrue(resolver.open("com.example.video", 0).changed)
        assertFalse(resolver.open("com.example.video", 500).changed)
        assertFalse(resolver.onTick(1_000).changed)
    }

    // A keyboard installed after the monitor started must not become the primary app.
    @Test
    fun refreshedFiltersApplyWithoutLosingThePrimaryApp() {
        val resolver = resolver(stabilizationMs = 0L)
        resolver.open("com.example.video", 0)
        resolver.updateFilters(
            launcherActivities = setOf(launcherComponent),
            transientPackages = setOf(keyboard, "com.example.newkeyboard"),
        )

        val result = resolver.open("com.example.newkeyboard", 100)

        assertEquals("com.example.video", result.primary)
        assertFalse(result.changed)
    }

    @Test
    fun userFacingDropsTransientPackagesFromTheVisibleSet() {
        val visible = resolver().userFacing(
            setOf("com.android.chrome", "com.android.systemui", keyboard, "com.v2ray.ang", "ru.bank.app")
        )

        assertEquals(setOf("com.android.chrome", "ru.bank.app"), visible)
    }

    @Test
    fun resetClearsThePrimaryAppOnce() {
        val resolver = resolver(stabilizationMs = 0L)
        resolver.open("com.example.video", 0)
        assertTrue(resolver.reset().changed)
        assertFalse(resolver.reset().changed)
        assertEquals(null, resolver.current())
    }
}
