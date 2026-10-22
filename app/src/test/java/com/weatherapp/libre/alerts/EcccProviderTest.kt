package com.weatherapp.libre.alerts

import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.alerts.models.AlertSource
import com.weatherapp.libre.alerts.providers.EcccProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EcccProviderTest {

    private val torontoLocation = LocationItem(
        id = 1L,
        name = "Toronto",
        admin1 = "Ontario",
        country = "Canada",
        countryCode = "CA",
        latitude = 43.6532,
        longitude = -79.3832
    )

    private val newYorkLocation = LocationItem(
        id = 2L,
        name = "New York",
        admin1 = "New York",
        country = "United States",
        countryCode = "US",
        latitude = 40.7128,
        longitude = -74.0060
    )

    @Test
    fun testProviderSourceIsEccc() {
        val provider = EcccProvider()
        assertEquals(AlertSource.ECCC, provider.source)
    }

    @Test
    fun testNonCanadianLocationReturnsEmptyList() = runBlocking {
        val provider = EcccProvider()
        val alerts = provider.fetchAlertsForLocation(newYorkLocation)
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun testProvinceToEcccCodeMapping() {
        val codeOn = EcccProvider.PROVINCE_TO_ECCC_CODE["Ontario"]
        assertEquals("CWTO", codeOn)

        val codeQc = EcccProvider.PROVINCE_TO_ECCC_CODE["Quebec"]
        assertEquals("CWUL", codeQc)

        val codeAb = EcccProvider.PROVINCE_TO_ECCC_CODE["Alberta"]
        assertEquals("CWAO", codeAb)
    }
}
