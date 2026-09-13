package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.ServerAffiliationInfo
import com.v2ray.ang.enums.EConfigType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MainServerRowModelsTest {

    @Test
    fun selectedServerIsNullWithoutSelection() {
        assertNull(buildSelectedServerUiModel(guid = null, profile = profile(), affiliation = null))
        assertNull(buildSelectedServerUiModel(guid = "", profile = profile(), affiliation = null))
    }

    @Test
    fun selectedServerIsNullWhenProfileNoLongerExists() {
        assertNull(buildSelectedServerUiModel(guid = "guid-1", profile = null, affiliation = ServerAffiliationInfo(48L)))
    }

    @Test
    fun selectedServerWithoutTestResultHasNoDelay() {
        val model = buildSelectedServerUiModel(guid = "guid-1", profile = profile(), affiliation = null)!!

        assertEquals("guid-1", model.guid)
        assertEquals("Germany 1", model.remarks)
        assertEquals("de.example", model.statistics)
        assertEquals("VLESS / ws / tls", model.typeDescription)
        assertEquals(0L, model.testDelayMillis)
        assertEquals("", model.subscriptionBadge)
    }

    @Test
    fun selectedServerCarriesSuccessfulAndFailedDelays() {
        val reachable = buildSelectedServerUiModel("guid-1", profile(), ServerAffiliationInfo(48L))!!
        val unreachable = buildSelectedServerUiModel("guid-1", profile(), ServerAffiliationInfo(-1L))!!

        assertEquals(48L, reachable.testDelayMillis)
        assertEquals(-1L, unreachable.testDelayMillis)
    }

    private fun profile() = ProfileItem.create(EConfigType.VLESS).apply {
        remarks = "Germany 1"
        description = "de.example"
        network = "ws"
        security = "tls"
    }
}
