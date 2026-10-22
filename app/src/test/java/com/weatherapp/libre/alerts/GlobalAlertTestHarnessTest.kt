package com.weatherapp.libre.alerts

import com.weatherapp.libre.alerts.models.AlertSeverity
import com.weatherapp.libre.alerts.models.AlertSource
import com.weatherapp.libre.alerts.providers.MeteoAlarmProvider
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateApi
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateFeature
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateFeatureCollection
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateProperties
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GlobalAlertTestHarnessTest {

    @Test
    fun testMockMeteoGateApiParsesAndMapsSeverity() = runBlocking {
        val mockCollection = MeteoGateFeatureCollection(
            features = listOf(
                MeteoGateFeature(
                    id = "test_feature_01",
                    properties = MeteoGateProperties(
                        event = "Wind Warning",
                        awarenessLevel = "4; red; Extreme",
                        headline = "Extreme Wind Warning"
                    )
                )
            )
        )

        val mockApi = object : MeteoGateApi {
            override suspend fun getWarningsForPosition(coords: String) = mockCollection
            override suspend fun getWarningsItems(bbox: String?) = mockCollection
        }

        val provider = MeteoAlarmProvider(api = mockApi)
        val location = com.weatherapp.libre.LocationItem(
            id = 500L,
            name = "Paris",
            country = "France",
            countryCode = "FR",
            latitude = 48.8566,
            longitude = 2.3522
        )

        val alerts = provider.fetchAlertsForLocation(location)
        assertEquals(1, alerts.size)
        assertEquals("test_feature_01", alerts[0].id)
        assertEquals(AlertSeverity.RED, alerts[0].severity)
        assertEquals(AlertSource.METEOALARM, alerts[0].source)
        assertEquals(48.8566, alerts[0].locationLat, 0.0001)
        assertEquals(2.3522, alerts[0].locationLon, 0.0001)
    }
}
