package com.v2ray.ang.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.service.AutoVpnMonitorService
import com.v2ray.ang.service.CoreVpnService
import com.v2ray.ang.util.LogUtil

/**
 * Handles the Auto VPN service commands inside the daemon process.
 *
 * A manifest receiver is used instead of the dynamic one owned by `CoreServiceManager` because
 * the dynamic receiver only exists while the core loop runs: a stop decided during the start
 * window, before `startCoreLoop()` registered it, would otherwise be dropped exactly in the
 * case the task calls critical.
 */
class AutoVpnCommandReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.getIntExtra("key", 0)) {
            AppConfig.MSG_AUTO_VPN_STOP -> stopTunnel(context)
            AppConfig.MSG_AUTO_VPN_RELOAD -> AutoVpnMonitorService.reloadRunning()
            else -> Unit
        }
    }

    private fun stopTunnel(context: Context) {
        LogUtil.i(AppConfig.TAG, "AutoVpn: stop command received")
        val control = CoreServiceManager.serviceControl?.get()
        if (CoreServiceManager.isRunning() && control != null) {
            // Normal teardown: stops tun2socks and the core, then closes the descriptor.
            control.stopService()
            return
        }
        // The core loop has not started yet, so only the Android service exists; destroying it
        // runs the same cleanup through onDestroy().
        context.stopService(Intent(context, CoreVpnService::class.java))
    }
}
