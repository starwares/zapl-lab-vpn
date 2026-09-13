package com.v2ray.ang.autoVpn

import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager

/** Snapshot of what the Auto VPN engine currently believes, shown on the settings screen. */
data class AutoVpnStatus(
    val primaryPackage: String?,
    val policy: AppVpnPolicy?,
    val vpnState: AutoVpnPolicyEngine.VpnState,
    val updatedAtMs: Long,
) {
    companion object {
        val EMPTY = AutoVpnStatus(null, null, AutoVpnPolicyEngine.VpnState.OFF, 0L)
    }
}

/**
 * Carries the status across the process boundary between the daemon that owns the engine and
 * the UI. MMKV is opened in multi-process mode, and the value is written only when the status
 * actually changes, so the event path does not write on every tick.
 */
object AutoVpnStatusStore {

    private const val SEPARATOR = "|"
    private const val NONE = "-"

    @Volatile
    private var lastSerialized: String? = null

    fun publish(status: AutoVpnStatus) {
        // The timestamp is deliberately excluded from the comparison: it changes on every call
        // and would turn "write only on change" into "write on every evaluation".
        val payload = listOf(
            status.primaryPackage ?: NONE,
            status.policy?.name ?: NONE,
            status.vpnState.name,
        ).joinToString(SEPARATOR)
        if (payload == lastSerialized) return
        lastSerialized = payload
        MmkvManager.encodeSettings(
            AppConfig.PREF_AUTO_VPN_STATUS,
            payload + SEPARATOR + status.updatedAtMs,
        )
    }

    fun read(): AutoVpnStatus {
        val raw = MmkvManager.decodeSettingsString(AppConfig.PREF_AUTO_VPN_STATUS, "").orEmpty()
        val parts = raw.split(SEPARATOR)
        if (parts.size != 4) return AutoVpnStatus.EMPTY
        return AutoVpnStatus(
            primaryPackage = parts[0].takeUnless { it == NONE },
            policy = parts[1].takeUnless { it == NONE }?.let { AppVpnPolicy.fromPersisted(it) },
            vpnState = runCatching { AutoVpnPolicyEngine.VpnState.valueOf(parts[2]) }
                .getOrDefault(AutoVpnPolicyEngine.VpnState.OFF),
            updatedAtMs = parts[3].toLongOrNull() ?: 0L,
        )
    }

    fun clear() {
        lastSerialized = null
        MmkvManager.encodeSettings(AppConfig.PREF_AUTO_VPN_STATUS, "")
    }
}
