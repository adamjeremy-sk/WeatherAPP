package com.weatherapp.libre.alerts

import com.weatherapp.libre.alerts.models.AlertSource
import com.weatherapp.libre.alerts.routing.AlertProviderRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AlertProviderRouterTest {

    @Test
    fun testEuCountryCodesReturnMeteoAlarmProvider() {
        val providerDe = AlertProviderRouter.getProviderForCountryCode("DE")
        assertNotNull(providerDe)
        assertEquals(AlertSource.METEOALARM, providerDe?.source)

        val providerFr = AlertProviderRouter.getProviderForCountryCode("fr")
        assertNotNull(providerFr)
        assertEquals(AlertSource.METEOALARM, providerFr?.source)

        val providerIt = AlertProviderRouter.getProviderForCountryCode("IT")
        assertNotNull(providerIt)
        assertEquals(AlertSource.METEOALARM, providerIt?.source)
    }

    @Test
    fun testCanadaCountryCodeReturnsEcccProvider() {
        val providerCa = AlertProviderRouter.getProviderForCountryCode("CA")
        assertNotNull(providerCa)
        assertEquals(AlertSource.ECCC, providerCa?.source)

        val providerCan = AlertProviderRouter.getProviderForCountryCode("can")
        assertNotNull(providerCan)
        assertEquals(AlertSource.ECCC, providerCan?.source)
    }

    @Test
    fun testUnsupportedCountryCodeReturnsNull() {
        assertNull(AlertProviderRouter.getProviderForCountryCode("US"))
        assertNull(AlertProviderRouter.getProviderForCountryCode("AU"))
        assertNull(AlertProviderRouter.getProviderForCountryCode(null))
        assertNull(AlertProviderRouter.getProviderForCountryCode(""))
    }
}
