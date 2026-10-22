package com.weatherapp.libre.alerts

import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.alerts.models.AlertSeverity
import com.weatherapp.libre.alerts.models.AlertSource
import com.weatherapp.libre.alerts.providers.MeteoAlarmProvider
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateApi
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateFeature
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateFeatureCollection
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateProperties
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MeteoAlarmProviderTest {

    private val berlinLocation = LocationItem(
        id = 101L,
        name = "Berlin",
        admin1 = "Berlin",
        country = "Germany",
        countryCode = "DE",
        latitude = 52.5200,
        longitude = 13.4050
    )

    class FakeMeteoGateApi(private val response: MeteoGateFeatureCollection) : MeteoGateApi {
        override suspend fun getWarningsForPosition(coords: String): MeteoGateFeatureCollection {
            return response
        }

        override suspend fun getWarningsItems(bbox: String?): MeteoGateFeatureCollection {
            return response
        }
    }

    @Test
    fun testSeverityMappingAndLevelFilter() = runBlocking {
        val features = listOf(
            MeteoGateFeature(
                id = "f1",
                properties = MeteoGateProperties(
                    event = "Wind",
                    awarenessLevel = "2; yellow; Moderate",
                    onset = "2025-01-01T10:00:00Z",
                    expires = "2025-01-01T18:00:00Z",
                    headline = "Moderate Wind Warning"
                )
            ),
            MeteoGateFeature(
                id = "f2",
                properties = MeteoGateProperties(
                    event = "Rain",
                    awarenessLevel = "3; orange; Severe",
                    onset = "2025-01-01T10:00:00Z",
                    expires = "2025-01-01T18:00:00Z",
                    headline = "Severe Rain Warning"
                )
            ),
            MeteoGateFeature(
                id = "f3",
                properties = MeteoGateProperties(
                    event = "Thunderstorm",
                    awarenessLevel = "4; red; Extreme",
                    onset = "2025-01-01T10:00:00Z",
                    expires = "2025-01-01T18:00:00Z",
                    headline = "Extreme Thunderstorm Warning"
                )
            ),
            MeteoGateFeature(
                id = "f4",
                properties = MeteoGateProperties(
                    event = "Minor Fog",
                    awarenessLevel = "1; green; Minor",
                    onset = "2025-01-01T10:00:00Z",
                    expires = "2025-01-01T18:00:00Z",
                    headline = "Minor Fog"
                )
            )
        )

        val fakeApi = FakeMeteoGateApi(MeteoGateFeatureCollection(features = features))
        val provider = MeteoAlarmProvider(api = fakeApi)

        val alerts = provider.fetchAlertsForLocation(berlinLocation)

        assertEquals(3, alerts.size)

        assertEquals("f1", alerts[0].id)
        assertEquals(AlertSeverity.YELLOW, alerts[0].severity)
        assertEquals(AlertSource.METEOALARM, alerts[0].source)
        assertEquals(52.5200, alerts[0].locationLat, 0.0001)
        assertEquals(13.4050, alerts[0].locationLon, 0.0001)

        assertEquals("f2", alerts[1].id)
        assertEquals(AlertSeverity.ORANGE, alerts[1].severity)

        assertEquals("f3", alerts[2].id)
        assertEquals(AlertSeverity.RED, alerts[2].severity)
    }
}
