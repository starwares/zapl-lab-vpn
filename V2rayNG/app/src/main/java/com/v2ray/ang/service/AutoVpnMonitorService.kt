package com.v2ray.ang.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.autoVpn.AutoVpnCoordinator
import com.v2ray.ang.enums.NotificationChannelType
import com.v2ray.ang.handler.AppLocaleManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.helper.NotificationHelper
import com.v2ray.ang.util.LogUtil

/**
 * Keeps the foreground-app monitor alive while Auto VPN is on.
 *
 * It is a separate service from the core ones because it has a different lifetime: it must run
 * while the tunnel is stopped, which is exactly when no core service exists. It runs in the
 * daemon process so the coordinator can read the core state from its only authority.
 */
class AutoVpnMonitorService : Service() {

    private var coordinator: AutoVpnCoordinator? = null

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let(AppLocaleManager::localizedContext))
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // The foreground contract has to be fulfilled before any other work, including a start
        // command that will immediately stop the service again.
        NotificationHelper.startForeground(
            service = this,
            channelType = NotificationChannelType.AUTO_VPN_MONITOR,
            title = getString(R.string.auto_vpn_title),
            content = getString(R.string.auto_vpn_notification_text),
        )
        if (coordinator != null) return START_STICKY

        val started = AutoVpnCoordinator(this).let { created ->
            if (created.start()) {
                coordinator = created
                true
            } else {
                false
            }
        }
        if (!started) {
            LogUtil.w(AppConfig.TAG, "AutoVpn: monitor cannot run, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        coordinator?.stop()
        coordinator = null
        instance = null
        NotificationHelper.stopForeground(this)
        NotificationHelper.cancel(NotificationChannelType.AUTO_VPN_MONITOR, this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        /**
         * Process-local handle used only to forward a settings change to the running monitor.
         * It is never used as proof of daemon or tunnel state.
         */
        @Volatile
        private var instance: AutoVpnMonitorService? = null

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context.applicationContext, AutoVpnMonitorService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context.applicationContext, AutoVpnMonitorService::class.java))
        }

        /** Starts or stops the monitor so it matches the stored setting. */
        fun syncWithSettings(context: Context) {
            val enabled = MmkvManager.decodeSettingsBool(AppConfig.PREF_AUTO_VPN_ENABLED, false)
            if (enabled && AutoVpnCoordinator.hasUsageAccess(context)) start(context) else stop(context)
        }

        fun reloadRunning() {
            instance?.coordinator?.reload()
        }
    }
}
