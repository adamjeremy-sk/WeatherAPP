package com.weatherapp.libre.alerts

import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.alerts.models.AlertFetchResult
import com.weatherapp.libre.alerts.models.AlertSeverity
import com.weatherapp.libre.alerts.models.AlertSource
import com.weatherapp.libre.alerts.providers.MeteoAlarmProvider
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateApi
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateBasicProperties
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateCapArea
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateCapInfo
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateCapParameter
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateDetailedCap
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateFeature
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateFeatureCollection
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateLink
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalAlertTestHarnessTest {

    @Test
    fun testMockMeteoGateApiParsesAndMapsSeverity() = runBlocking {
        val mockCollection = MeteoGateFeatureCollection(
            features = listOf(
                MeteoGateFeature(
                    id = "test_feature_01",
                    properties = MeteoGateBasicProperties(countryCode = "FR"),
                    links = listOf(
                        MeteoGateLink(rel = "json", type = "application/json", href = "mock://detail/1")
                    )
                )
            )
        )

        val mockDetail = MeteoGateDetailedCap(
            identifier = "test_feature_01",
            info = listOf(
                MeteoGateCapInfo(
                    event = "Wind Warning",
                    headline = "Extreme Wind Warning",
                    parameter = listOf(
                        MeteoGateCapParameter(valueName = "awareness_level", value = "4; red; Extreme")
                    ),
                    area = listOf(
                        MeteoGateCapArea(
                            areaDesc = "Paris Region",
                            polygon = listOf("48.0,2.0 49.0,2.0 49.0,3.0 48.0,3.0 48.0,2.0")
                        )
                    )
                )
            )
        )

        val mockApi = object : MeteoGateApi {
            override suspend fun getWarningsForCountry(countryCode: String, datetime: String) = mockCollection
            override suspend fun getDetailedCapJson(url: String) = mockDetail
        }

        val provider = MeteoAlarmProvider(api = mockApi)
        val location = LocationItem(
            id = 500L,
            name = "Paris",
            country = "France",
            countryCode = "FR",
            latitude = 48.8566,
            longitude = 2.3522
        )

        val result = provider.fetchAlertsForLocation(location)
        assertTrue(result is AlertFetchResult.Success)
        val alerts = (result as AlertFetchResult.Success).alerts
        assertEquals(1, alerts.size)
        assertEquals("test_feature_01", alerts[0].id)
        assertEquals(AlertSeverity.RED, alerts[0].severity)
        assertEquals(AlertSource.METEOALARM, alerts[0].source)
        assertEquals(48.8566, alerts[0].locationLat, 0.0001)
        assertEquals(2.3522, alerts[0].locationLon, 0.0001)
    }
}
