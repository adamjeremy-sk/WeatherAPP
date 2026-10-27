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
import java.io.IOException

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

    private val newYorkLocation = LocationItem(
        id = 102L,
        name = "New York",
        admin1 = "New York",
        country = "United States",
        countryCode = "US",
        latitude = 40.7128,
        longitude = -74.0060
    )

    private val berlinPolygon = "52.0,13.0 53.0,13.0 53.0,14.0 52.0,14.0 52.0,13.0"

    class FakeMeteoGateApi(
        private val indexResponse: MeteoGateFeatureCollection,
        private val detailsMap: Map<String, MeteoGateDetailedCap> = emptyMap()
    ) : MeteoGateApi {
        override suspend fun getWarningsForCountry(countryCode: String, datetime: String): MeteoGateFeatureCollection {
            return indexResponse
        }

        override suspend fun getDetailedCapJson(url: String): MeteoGateDetailedCap {
            return detailsMap[url] ?: throw IOException("Detail failed for $url")
        }
    }

    class FailingMeteoGateApi(private val exception: Exception) : MeteoGateApi {
        override suspend fun getWarningsForCountry(countryCode: String, datetime: String): MeteoGateFeatureCollection {
            throw exception
        }

        override suspend fun getDetailedCapJson(url: String): MeteoGateDetailedCap {
            throw exception
        }
    }

    @Test
    fun testNonEuropeanLocationReturnsEmptyList() = runBlocking {
        val fakeApi = FakeMeteoGateApi(MeteoGateFeatureCollection(features = emptyList()))
        val provider = MeteoAlarmProvider(api = fakeApi)

        val result = provider.fetchAlertsForLocation(newYorkLocation)
        assertTrue(result is AlertFetchResult.Success)
        val alerts = (result as AlertFetchResult.Success).alerts
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun testValidAlertReturnedOnSuccessfulTwoTierFetch() = runBlocking {
        val feature = MeteoGateFeature(
            id = "f1",
            properties = MeteoGateBasicProperties(countryCode = "DE"),
            links = listOf(
                MeteoGateLink(rel = "json", type = "application/json", href = "mock://detail/1")
            )
        )
        val detail = MeteoGateDetailedCap(
            identifier = "cap_f1",
            info = listOf(
                MeteoGateCapInfo(
                    event = "Wind Warning",
                    headline = "Severe Wind in Berlin",
                    severity = "Severe",
                    onset = "2026-10-27T12:00:00Z",
                    expires = "2026-10-27T18:00:00Z",
                    parameter = listOf(
                        MeteoGateCapParameter(valueName = "awareness_level", value = "3; Orange; Severe")
                    ),
                    area = listOf(
                        MeteoGateCapArea(
                            areaDesc = "Berlin Region",
                            polygon = listOf(berlinPolygon)
                        )
                    )
                )
            )
        )

        val fakeApi = FakeMeteoGateApi(
            indexResponse = MeteoGateFeatureCollection(features = listOf(feature)),
            detailsMap = mapOf("mock://detail/1" to detail)
        )
        val provider = MeteoAlarmProvider(api = fakeApi)

        val result = provider.fetchAlertsForLocation(berlinLocation)
        assertTrue(result is AlertFetchResult.Success)
        val alerts = (result as AlertFetchResult.Success).alerts

        assertEquals(1, alerts.size)
        val alert = alerts[0]
        assertEquals("cap_f1", alert.id)
        assertEquals(AlertSeverity.ORANGE, alert.severity)
        assertEquals(AlertSource.METEOALARM, alert.source)
        assertEquals("Wind Warning", alert.eventNative)
        assertEquals("Severe Wind in Berlin", alert.headline)
        assertEquals("Berlin Region", alert.areaDesc)
    }

    @Test
    fun testLevel1GreenAlertFilteredOut() = runBlocking {
        val feature = MeteoGateFeature(
            id = "f_green",
            properties = MeteoGateBasicProperties(countryCode = "DE"),
            links = listOf(
                MeteoGateLink(rel = "json", type = "application/json", href = "mock://detail/green")
            )
        )
        val detail = MeteoGateDetailedCap(
            identifier = "cap_green",
            info = listOf(
                MeteoGateCapInfo(
                    event = "Minor Rain",
                    parameter = listOf(
                        MeteoGateCapParameter(valueName = "awareness_level", value = "1; Green; Minor")
                    ),
                    area = listOf(
                        MeteoGateCapArea(
                            areaDesc = "Berlin Region",
                            polygon = listOf(berlinPolygon)
                        )
                    )
                )
            )
        )

        val fakeApi = FakeMeteoGateApi(
            indexResponse = MeteoGateFeatureCollection(features = listOf(feature)),
            detailsMap = mapOf("mock://detail/green" to detail)
        )
        val provider = MeteoAlarmProvider(api = fakeApi)

        val result = provider.fetchAlertsForLocation(berlinLocation)
        assertTrue(result is AlertFetchResult.Success)
        val alerts = (result as AlertFetchResult.Success).alerts
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun testTier2FailureSkipsFeatureButSucceedsOverall() = runBlocking {
        val f1 = MeteoGateFeature(
            id = "f_failing",
            links = listOf(MeteoGateLink(rel = "json", type = "application/json", href = "mock://detail/fail"))
        )
        val f2 = MeteoGateFeature(
            id = "f_working",
            links = listOf(MeteoGateLink(rel = "json", type = "application/json", href = "mock://detail/ok"))
        )
        val detail2 = MeteoGateDetailedCap(
            identifier = "cap_working",
            info = listOf(
                MeteoGateCapInfo(
                    event = "Thunderstorm",
                    parameter = listOf(
                        MeteoGateCapParameter(valueName = "awareness_level", value = "4; Red; Extreme")
                    ),
                    area = listOf(
                        MeteoGateCapArea(
                            areaDesc = "Berlin Region",
                            polygon = listOf(berlinPolygon)
                        )
                    )
                )
            )
        )

        val fakeApi = FakeMeteoGateApi(
            indexResponse = MeteoGateFeatureCollection(features = listOf(f1, f2)),
            detailsMap = mapOf("mock://detail/ok" to detail2)
        )
        val provider = MeteoAlarmProvider(api = fakeApi)

        val result = provider.fetchAlertsForLocation(berlinLocation)
        assertTrue(result is AlertFetchResult.Success)
        val alerts = (result as AlertFetchResult.Success).alerts

        assertEquals(1, alerts.size)
        assertEquals("cap_working", alerts[0].id)
        assertEquals(AlertSeverity.RED, alerts[0].severity)
    }

    @Test
    fun testHttpErrorReturnsFailure() = runBlocking {
        val fakeApi = FailingMeteoGateApi(IOException("HTTP 500 Internal Server Error"))
        val provider = MeteoAlarmProvider(api = fakeApi)

        val result = provider.fetchAlertsForLocation(berlinLocation)

        assertTrue(result is AlertFetchResult.Failure)
        val failure = result as AlertFetchResult.Failure
        assertTrue(failure.cause is IOException)
        assertEquals("HTTP 500 Internal Server Error", failure.cause.message)
    }

    @Test
    fun testIOExceptionReturnsFailure() = runBlocking {
        val fakeApi = FailingMeteoGateApi(IOException("Network timeout"))
        val provider = MeteoAlarmProvider(api = fakeApi)

        val result = provider.fetchAlertsForLocation(berlinLocation)

        assertTrue(result is AlertFetchResult.Failure)
        val failure = result as AlertFetchResult.Failure
        assertTrue(failure.cause is IOException)
        assertEquals("Network timeout", failure.cause.message)
    }
}
