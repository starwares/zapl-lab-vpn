package com.v2ray.ang.autoVpn

import androidx.annotation.StringRes
import com.v2ray.ang.R

/**
 * The single network policy assigned to an installed package by the user.
 *
 * Persisted by name through [AutoVpnPolicyRepository]; never rename an entry without a
 * migration, because the name is the stored value.
 */
enum class AppVpnPolicy(@StringRes val labelRes: Int) {
    /** The tunnel must be up while the user actually works with the app. */
    VPN(R.string.auto_vpn_policy_vpn),

    /** The app does not need the tunnel; it may be stopped after the idle delay. */
    DIRECT(R.string.auto_vpn_policy_direct),

    /** The tunnel must be fully stopped as soon as the app becomes the primary app. */
    VPN_FORBIDDEN(R.string.auto_vpn_policy_forbidden);

    companion object {
        /** Default for a package the user never configured. */
        val DEFAULT = DIRECT

        fun fromPersisted(value: String?) = entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}
