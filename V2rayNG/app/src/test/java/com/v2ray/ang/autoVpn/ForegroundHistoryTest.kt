package com.v2ray.ang.autoVpn

import com.v2ray.ang.autoVpn.ForegroundHistory.ActivityEvent
import com.v2ray.ang.autoVpn.ForegroundHistory.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForegroundHistoryTest {

    private fun event(kind: Kind, pkg: String, atMs: Long, cls: String = "$pkg.Main") =
        ActivityEvent(kind, pkg, cls, atMs)

    // Found on the emulator: the monitor came up after boot while Chrome was already open.
    @Test
    fun findsTheAppThatIsStillOnScreen() {
        val history = listOf(
            event(Kind.RESUMED, "launcher", 0),
            event(Kind.PAUSED, "launcher", 100),
            event(Kind.RESUMED, "com.android.chrome", 110),
        )

        assertEquals("com.android.chrome", ForegroundHistory.currentlyResumed(history)?.packageName)
    }

    @Test
    fun ignoresAppsThatWerePausedOrStoppedLater() {
        val history = listOf(
            event(Kind.RESUMED, "com.android.chrome", 0),
            event(Kind.RESUMED, "com.android.settings", 50),
            event(Kind.PAUSED, "com.android.settings", 60),
            event(Kind.STOPPED, "com.android.settings", 70),
        )

        assertEquals("com.android.chrome", ForegroundHistory.currentlyResumed(history)?.packageName)
    }

    @Test
    fun aDeviceShutdownEndsEveryEarlierActivity() {
        val history = listOf(
            event(Kind.RESUMED, "com.android.chrome", 0),
            event(Kind.DEVICE_BOUNDARY, "android", 1_000, cls = ""),
        )

        assertNull(ForegroundHistory.currentlyResumed(history))
    }

    @Test
    fun anActivityResumedAgainIsCurrentEvenAfterAnEarlierPause() {
        val history = listOf(
            event(Kind.RESUMED, "com.android.chrome", 0),
            event(Kind.PAUSED, "com.android.chrome", 10),
            event(Kind.RESUMED, "com.android.settings", 20),
            event(Kind.PAUSED, "com.android.settings", 30),
            event(Kind.RESUMED, "com.android.chrome", 40),
        )

        val current = ForegroundHistory.currentlyResumed(history)
        assertEquals("com.android.chrome", current?.packageName)
        assertEquals(40L, current?.atMs)
    }

    // Observed on the emulator: in split-screen both panes stay resumed and focus moves silently.
    @Test
    fun splitScreenKeepsBothPanesInTheResumedSet() {
        val tracker = ForegroundHistory.ResumedTracker()
        tracker.apply(event(Kind.RESUMED, "com.android.chrome", 0))
        val changed = tracker.apply(event(Kind.RESUMED, "com.android.settings", 5_000))

        assertEquals(true, changed)
        assertEquals(setOf("com.android.chrome", "com.android.settings"), tracker.packages())
        assertEquals("com.android.settings", tracker.current()?.packageName)
    }

    @Test
    fun leavingSplitScreenRemovesThePausedPane() {
        val tracker = ForegroundHistory.ResumedTracker()
        tracker.apply(event(Kind.RESUMED, "com.android.chrome", 0))
        tracker.apply(event(Kind.RESUMED, "com.android.settings", 10))

        assertEquals(true, tracker.apply(event(Kind.STOPPED, "com.android.settings", 20)))
        assertEquals(setOf("com.android.chrome"), tracker.packages())
        assertEquals(false, tracker.apply(event(Kind.PAUSED, "com.android.settings", 30)))
    }

    @Test
    fun aPausedPictureInPictureWindowIsNotVisibleForThePolicy() {
        val tracker = ForegroundHistory.ResumedTracker()
        tracker.apply(event(Kind.RESUMED, "com.example.video", 0))
        tracker.apply(event(Kind.PAUSED, "com.example.video", 100))
        tracker.apply(event(Kind.RESUMED, "ru.bank.app", 110))

        assertEquals(setOf("ru.bank.app"), tracker.packages())
    }

    @Test
    fun emptyHistoryHasNoCurrentApp() {
        assertNull(ForegroundHistory.currentlyResumed(emptyList()))
    }
}
