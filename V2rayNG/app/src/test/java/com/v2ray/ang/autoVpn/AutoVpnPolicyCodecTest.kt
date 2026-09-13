package com.v2ray.ang.autoVpn

import org.junit.Assert.assertEquals
import org.junit.Test

class AutoVpnPolicyCodecTest {

    @Test
    fun readsTheFormatWrittenByTheReleasedBuild() {
        val stored = "com.openai.chatgpt=VPN\nru.bank.app=VPN_FORBIDDEN"

        val decoded = AutoVpnPolicyCodec.decode(stored)

        assertEquals(AppVpnPolicy.VPN, decoded["com.openai.chatgpt"])
        assertEquals(AppVpnPolicy.VPN_FORBIDDEN, decoded["ru.bank.app"])
        assertEquals(2, decoded.size)
    }

    @Test
    fun roundTripsAndKeepsTheOrderStable() {
        val values = mapOf(
            "ru.bank.app" to AppVpnPolicy.VPN_FORBIDDEN,
            "com.openai.chatgpt" to AppVpnPolicy.VPN,
        )

        val encoded = AutoVpnPolicyCodec.encode(values)

        assertEquals("com.openai.chatgpt=VPN\nru.bank.app=VPN_FORBIDDEN", encoded)
        assertEquals(values, AutoVpnPolicyCodec.decode(encoded))
    }

    @Test
    fun defaultsAreNotStored() {
        val encoded = AutoVpnPolicyCodec.encode(mapOf("com.android.calculator2" to AppVpnPolicy.DIRECT))

        assertEquals("", encoded)
    }

    @Test
    fun unknownAndMalformedEntriesFallBackInsteadOfBreakingTheTable() {
        val decoded = AutoVpnPolicyCodec.decode("a.pkg=FUTURE_POLICY\n\nbroken-line\n=VPN\nb.pkg=VPN")

        assertEquals(AppVpnPolicy.DEFAULT, decoded["a.pkg"])
        assertEquals(AppVpnPolicy.VPN, decoded["b.pkg"])
        assertEquals(2, decoded.size)
    }

    @Test
    fun emptyStorageDecodesToAnEmptyTable() {
        assertEquals(emptyMap<String, AppVpnPolicy>(), AutoVpnPolicyCodec.decode(null))
        assertEquals(emptyMap<String, AppVpnPolicy>(), AutoVpnPolicyCodec.decode(""))
    }
}
