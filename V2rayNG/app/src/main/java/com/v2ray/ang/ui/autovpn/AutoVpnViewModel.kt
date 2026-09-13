package com.v2ray.ang.ui.autovpn

import android.app.Application
import com.v2ray.ang.AppConfig
import com.v2ray.ang.autoVpn.AppVpnPolicy
import com.v2ray.ang.autoVpn.AutoVpnCoordinator
import com.v2ray.ang.autoVpn.AutoVpnPolicyEngine
import com.v2ray.ang.autoVpn.AutoVpnPolicyRepository
import com.v2ray.ang.autoVpn.AutoVpnStatus
import com.v2ray.ang.autoVpn.AutoVpnStatusStore
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.helper.MessageHelper
import com.v2ray.ang.service.AutoVpnMonitorService
import com.v2ray.ang.ui.base.BaseViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** State and operations of the Auto VPN settings screen. */
class AutoVpnViewModel(application: Application) : BaseViewModel(application) {

    data class UiState(
        val enabled: Boolean = false,
        val hasUsageAccess: Boolean = false,
        val supportedRunMode: Boolean = true,
        val idleTimeoutMs: Long = AutoVpnPolicyEngine.DEFAULT_IDLE_TIMEOUT_MS,
        val keepWhileScreenOff: Boolean = false,
        val counts: Map<AppVpnPolicy, Int> = emptyMap(),
        val status: AutoVpnStatus = AutoVpnStatus.EMPTY,
        val statusAppLabel: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Re-reads everything that a system screen or the daemon may have changed meanwhile. */
    fun refresh() {
        AutoVpnPolicyRepository.refresh()
        val status = AutoVpnStatusStore.read()
        _state.value = UiState(
            enabled = MmkvManager.decodeSettingsBool(AppConfig.PREF_AUTO_VPN_ENABLED, false),
            hasUsageAccess = AutoVpnCoordinator.hasUsageAccess(app),
            supportedRunMode = AutoVpnCoordinator.isSupportedRunMode(),
            idleTimeoutMs = MmkvManager.decodeSettingsLong(
                AppConfig.PREF_AUTO_VPN_IDLE_TIMEOUT_MS,
                AutoVpnPolicyEngine.DEFAULT_IDLE_TIMEOUT_MS,
            ),
            keepWhileScreenOff = MmkvManager.decodeSettingsBool(AppConfig.PREF_AUTO_VPN_KEEP_SCREEN_OFF, false),
            counts = AppVpnPolicy.entries.associateWith { AutoVpnPolicyRepository.count(it) },
            status = status,
            statusAppLabel = _state.value.statusAppLabel.takeIf { status.primaryPackage != null },
        )
        loadStatusLabel(status)
    }

    fun setEnabled(enabled: Boolean) {
        MmkvManager.encodeSettings(AppConfig.PREF_AUTO_VPN_ENABLED, enabled)
        if (enabled) AutoVpnMonitorService.start(app) else AutoVpnMonitorService.stop(app)
        refresh()
    }

    fun setIdleTimeout(valueMs: Long) {
        MmkvManager.encodeSettings(AppConfig.PREF_AUTO_VPN_IDLE_TIMEOUT_MS, valueMs)
        notifyMonitor()
        refresh()
    }

    fun setKeepWhileScreenOff(keep: Boolean) {
        MmkvManager.encodeSettings(AppConfig.PREF_AUTO_VPN_KEEP_SCREEN_OFF, keep)
        notifyMonitor()
        refresh()
    }

    private fun notifyMonitor() {
        MessageHelper.sendMsg2Service(app, AppConfig.MSG_AUTO_VPN_RELOAD, "")
    }

    private fun loadStatusLabel(status: AutoVpnStatus) {
        val packageName = status.primaryPackage ?: return
        viewModelScope.launch {
            val label = withContext(Dispatchers.IO) {
                runCatching {
                    val manager = app.packageManager
                    manager.getApplicationLabel(manager.getApplicationInfo(packageName, 0)).toString()
                }.getOrNull()
            }
            _state.value = _state.value.copy(statusAppLabel = label ?: packageName)
        }
    }

    companion object {
        /** Offered turn-off delays; 0 means the tunnel stops as soon as the app is left. */
        val IDLE_TIMEOUT_OPTIONS_MS = listOf(0L, 5_000L, 10_000L, 20_000L, 30_000L)
    }
}
