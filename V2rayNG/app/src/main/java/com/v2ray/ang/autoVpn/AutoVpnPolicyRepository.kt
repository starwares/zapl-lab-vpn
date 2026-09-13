package com.v2ray.ang.autoVpn

import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager

/**
 * MMKV-backed store for per-package policies.
 *
 * The whole table is kept in memory and only reloaded when [refresh] is called, so the event
 * path never touches storage. The persisted format is one `packageName=POLICY_NAME` line per
 * configured package; unknown names decode to [AppVpnPolicy.DEFAULT], which keeps a table
 * written by a newer build readable by an older one.
 */
object AutoVpnPolicyRepository {

    @Volatile
    private var snapshot: Map<String, AppVpnPolicy>? = null

    /** Returns the policy of [packageName], loading the table once on first use. */
    fun get(packageName: String): AppVpnPolicy = all()[packageName] ?: AppVpnPolicy.DEFAULT

    fun all(): Map<String, AppVpnPolicy> = snapshot ?: load().also { snapshot = it }

    /** Counts of explicitly configured packages, used by the settings screen. */
    fun count(policy: AppVpnPolicy): Int = all().values.count { it == policy }

    fun set(packageName: String, policy: AppVpnPolicy) {
        val values = all().toMutableMap()
        val previous = values[packageName] ?: AppVpnPolicy.DEFAULT
        if (previous == policy) return
        // The default is represented by absence so the stored table stays proportional to what
        // the user actually configured instead of to the number of installed packages.
        if (policy == AppVpnPolicy.DEFAULT) values.remove(packageName) else values[packageName] = policy
        snapshot = values
        MmkvManager.encodeSettings(AppConfig.PREF_AUTO_VPN_POLICIES, AutoVpnPolicyCodec.encode(values))
    }

    /** Drops the in-memory table; the next read loads what another process wrote. */
    fun refresh() {
        snapshot = null
    }

    private fun load(): Map<String, AppVpnPolicy> =
        AutoVpnPolicyCodec.decode(MmkvManager.decodeSettingsString(AppConfig.PREF_AUTO_VPN_POLICIES, ""))
}
