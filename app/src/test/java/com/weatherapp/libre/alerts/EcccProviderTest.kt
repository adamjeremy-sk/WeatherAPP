package com.weatherapp.libre.alerts

import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.alerts.models.AlertFetchResult
import com.weatherapp.libre.alerts.models.AlertSeverity
import com.weatherapp.libre.alerts.models.AlertSource
import com.weatherapp.libre.alerts.models.UnifiedAlert
import com.weatherapp.libre.alerts.providers.EcccMemoEntry
import com.weatherapp.libre.alerts.providers.EcccProvider
import com.weatherapp.libre.alerts.providers.InMemoryEcccMemoStore
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

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

    private fun createFailingHttpClient(statusCode: Int = 500): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(statusCode)
                    .message("Internal Server Error")
                    .body(ResponseBody.create(null, "Error"))
                    .build()
            }
            .build()
    }

    private fun createIoExceptionHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { _ ->
                throw IOException("Network unreachable")
            }
            .build()
    }

    @Test
    fun testProviderSourceIsEccc() {
        val provider = EcccProvider()
        assertEquals(AlertSource.ECCC, provider.source)
    }

    @Test
    fun testNonCanadianLocationReturnsEmptyList() = runBlocking {
        val provider = EcccProvider()
        val result = provider.fetchAlertsForLocation(newYorkLocation)
        assertTrue(result is AlertFetchResult.Success)
        val alerts = (result as AlertFetchResult.Success).alerts
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun testHttpErrorReturnsFailure() = runBlocking {
        val failingClient = createFailingHttpClient(500)
        val provider = EcccProvider(httpClient = failingClient)
        val result = provider.fetchAlertsForLocation(torontoLocation)
        assertTrue(result is AlertFetchResult.Failure)
        val failure = result as AlertFetchResult.Failure
        assertTrue(failure.cause is IOException)
    }

    @Test
    fun testIOExceptionReturnsFailure() = runBlocking {
        val ioExceptionClient = createIoExceptionHttpClient()
        val provider = EcccProvider(httpClient = ioExceptionClient)
        val result = provider.fetchAlertsForLocation(torontoLocation)
        assertTrue(result is AlertFetchResult.Failure)
        val failure = result as AlertFetchResult.Failure
        assertTrue(failure.cause is IOException)
        assertEquals("Network unreachable", failure.cause.message)
    }

    @Test
    fun testProvinceToEcccCodeMapping() {
        val codeOn = EcccProvider.PROVINCE_TO_ECCC_CODE["Ontario"]
        assertEquals("CWTO", codeOn)

        val codeQc = EcccProvider.PROVINCE_TO_ECCC_CODE["Quebec"]
        assertEquals("CWUL", codeQc)

        val codeAb = EcccProvider.PROVINCE_TO_ECCC_CODE["Alberta"]
        assertEquals("CWEG", codeAb)
    }

    @Test
    fun testMemoSkippingHttpRequest() = runBlocking {
        var capFileRequestCount = 0
        val testMemoStore = InMemoryEcccMemoStore()

        val capFilename = "T_WWCN13_C_CWTO_20260301120000.cap"
        val memoKey = "eccc_memo_CWTO_$capFilename"
        testMemoStore.put(memoKey, EcccMemoEntry(matched = false))

        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val url = request.url.toString()

                if (url.endsWith(".cap") || url.endsWith(".xml")) {
                    capFileRequestCount++
                }

                val html = """
                    <html>
                    <body>
                    <a href="12/">12/</a>
                    <a href="$capFilename">$capFilename</a>
                    </body>
                    </html>
                """.trimIndent()

                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(null, html))
                    .build()
            }
            .build()

        val provider = EcccProvider(httpClient = mockClient, memoStore = testMemoStore)
        val result = provider.fetchAlertsForLocation(torontoLocation)

        assertTrue(result is AlertFetchResult.Success)
        assertEquals(0, capFileRequestCount)
    }

    @Test
    fun testMemoReturnsCachedUnifiedAlertWhenMatched() = runBlocking {
        var capFileRequestCount = 0
        val testMemoStore = InMemoryEcccMemoStore()

        val capFilename = "T_WWCN13_C_CWTO_20260301120000.cap"
        val memoKey = "eccc_memo_CWTO_$capFilename"

        val cachedAlert = UnifiedAlert(
            id = "test-cap-id",
            source = AlertSource.ECCC,
            locationLat = 43.6532,
            locationLon = -79.3832,
            severity = AlertSeverity.ORANGE,
            eventNative = "Severe Thunderstorm Warning",
            headline = "Severe Thunderstorm Warning in effect",
            description = "Test description",
            instruction = null,
            onset = null,
            expires = null,
            areaDesc = "Toronto"
        )
        testMemoStore.put(memoKey, EcccMemoEntry(matched = true, alert = cachedAlert))

        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val url = request.url.toString()

                if (url.endsWith(".cap") || url.endsWith(".xml")) {
                    capFileRequestCount++
                }

                val html = """
                    <html>
                    <body>
                    <a href="12/">12/</a>
                    <a href="$capFilename">$capFilename</a>
                    </body>
                    </html>
                """.trimIndent()

                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(null, html))
                    .build()
            }
            .build()

        val provider = EcccProvider(httpClient = mockClient, memoStore = testMemoStore)
        val result = provider.fetchAlertsForLocation(torontoLocation)

        assertTrue(result is AlertFetchResult.Success)
        assertEquals(0, capFileRequestCount)
        val alerts = (result as AlertFetchResult.Success).alerts
        assertEquals(1, alerts.size)
        assertEquals("test-cap-id", alerts[0].id)
    }

    @Test
    fun testEndToEndHappyPath() = runBlocking {
        val testMemoStore = InMemoryEcccMemoStore()
        val capXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <alert xmlns="urn:oasis:names:tc:emergency:cap:1.2">
              <identifier>urn:oid:2.49.0.1.124.0.2026.CAP-CP-ECCC-HAPPY-001</identifier>
              <sender>cap-pac@ec.gc.ca</sender>
              <sent>2026-03-01T12:00:00Z</sent>
              <status>Actual</status>
              <msgType>Alert</msgType>
              <scope>Public</scope>
              <info>
                <language>en-CA</language>
                <category>Met</category>
                <event>Severe Thunderstorm Warning</event>
                <urgency>Immediate</urgency>
                <severity>Severe</severity>
                <certainty>Observed</certainty>
                <effective>2026-03-01T12:00:00Z</effective>
                <expires>2026-03-01T18:00:00Z</expires>
                <senderName>Environment and Climate Change Canada</senderName>
                <headline>Severe Thunderstorm Warning for Toronto</headline>
                <description>Severe thunderstorm capable of producing strong wind gusts.</description>
                <instruction>Take cover if weather approaches.</instruction>
                <area>
                  <areaDesc>City of Toronto</areaDesc>
                  <circle>43.6532,-79.3832 50.0</circle>
                </area>
              </info>
            </alert>
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val url = request.url.toString()
                val bodyText = if (url.endsWith(".cap") || url.endsWith(".xml")) {
                    capXml
                } else {
                    """
                        <html>
                        <body>
                        <a href="12/">12/</a>
                        <a href="sample_happy.cap">sample_happy.cap</a>
                        </body>
                        </html>
                    """.trimIndent()
                }

                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(null, bodyText))
                    .build()
            }
            .build()

        val provider = EcccProvider(httpClient = mockClient, memoStore = testMemoStore)
        val result = provider.fetchAlertsForLocation(torontoLocation)

        assertTrue(result is AlertFetchResult.Success)
        val alerts = (result as AlertFetchResult.Success).alerts
        assertEquals(1, alerts.size)
        val alert = alerts[0]
        assertEquals("urn:oid:2.49.0.1.124.0.2026.CAP-CP-ECCC-HAPPY-001", alert.id)
        assertEquals(AlertSeverity.ORANGE, alert.severity)
        assertEquals(AlertSource.ECCC, alert.source)
        assertEquals("Severe Thunderstorm Warning", alert.eventNative)
        assertEquals("City of Toronto", alert.areaDesc)
    }

    @Test
    fun testHttp404Handling() = runBlocking {
        val testMemoStore = InMemoryEcccMemoStore()
        val notFoundClient = createFailingHttpClient(404)
        val provider = EcccProvider(httpClient = notFoundClient, memoStore = testMemoStore)

        val result = provider.fetchAlertsForLocation(torontoLocation)
        assertTrue(result is AlertFetchResult.Success)
        val alerts = (result as AlertFetchResult.Success).alerts
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun testSeverityMapping() = runBlocking {
        val severitiesToTest = listOf(
            "Extreme" to AlertSeverity.RED,
            "Severe" to AlertSeverity.ORANGE,
            "Moderate" to AlertSeverity.YELLOW,
            "Minor" to AlertSeverity.UNKNOWN
        )

        for ((xmlSeverity, expectedSeverity) in severitiesToTest) {
            val testMemoStore = InMemoryEcccMemoStore()
            val capXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <alert xmlns="urn:oasis:names:tc:emergency:cap:1.2">
                  <identifier>urn:oid:2.49.0.1.124.0.2026.CAP-SEV-$xmlSeverity</identifier>
                  <sender>cap-pac@ec.gc.ca</sender>
                  <sent>2026-03-01T12:00:00Z</sent>
                  <status>Actual</status>
                  <msgType>Alert</msgType>
                  <scope>Public</scope>
                  <info>
                    <language>en-CA</language>
                    <category>Met</category>
                    <event>Weather Warning</event>
                    <urgency>Immediate</urgency>
                    <severity>$xmlSeverity</severity>
                    <certainty>Observed</certainty>
                    <area>
                      <areaDesc>City of Toronto</areaDesc>
                      <circle>43.6532,-79.3832 50.0</circle>
                    </area>
                  </info>
                </alert>
            """.trimIndent()

            val mockClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val url = chain.request().url.toString()
                    val bodyText = if (url.endsWith(".cap") || url.endsWith(".xml")) {
                        capXml
                    } else {
                        """
                            <html>
                            <body>
                            <a href="12/">12/</a>
                            <a href="sev_$xmlSeverity.cap">sev_$xmlSeverity.cap</a>
                            </body>
                            </html>
                        """.trimIndent()
                    }

                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(ResponseBody.create(null, bodyText))
                        .build()
                }
                .build()

            val provider = EcccProvider(httpClient = mockClient, memoStore = testMemoStore)
            val result = provider.fetchAlertsForLocation(torontoLocation)

            assertTrue(result is AlertFetchResult.Success)
            val alerts = (result as AlertFetchResult.Success).alerts
            assertEquals(1, alerts.size)
            assertEquals("Expected $expectedSeverity for XML severity $xmlSeverity", expectedSeverity, alerts[0].severity)
        }
    }

    @Test
    fun testIsTestPropagation() = runBlocking {
        val testMemoStore = InMemoryEcccMemoStore()
        val capXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <alert xmlns="urn:oasis:names:tc:emergency:cap:1.2">
              <identifier>urn:oid:2.49.0.1.124.0.2026.CAP-CP-ECCC-TEST-PROP</identifier>
              <sender>cap-pac@ec.gc.ca</sender>
              <sent>2026-03-01T12:00:00Z</sent>
              <status>Test</status>
              <msgType>Alert</msgType>
              <scope>Public</scope>
              <info>
                <language>en-CA</language>
                <category>Met</category>
                <event>Test Tornado Warning</event>
                <urgency>Immediate</urgency>
                <severity>Extreme</severity>
                <certainty>Observed</certainty>
                <area>
                  <areaDesc>City of Toronto</areaDesc>
                  <circle>43.6532,-79.3832 50.0</circle>
                </area>
              </info>
            </alert>
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val url = chain.request().url.toString()
                val bodyText = if (url.endsWith(".cap") || url.endsWith(".xml")) {
                    capXml
                } else {
                    """
                        <html>
                        <body>
                        <a href="12/">12/</a>
                        <a href="test_prop.cap">test_prop.cap</a>
                        </body>
                        </html>
                    """.trimIndent()
                }

                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(null, bodyText))
                    .build()
            }
            .build()

        val provider = EcccProvider(httpClient = mockClient, memoStore = testMemoStore)
        val result = provider.fetchAlertsForLocation(torontoLocation)

        assertTrue(result is AlertFetchResult.Success)
        val alerts = (result as AlertFetchResult.Success).alerts
        assertEquals(1, alerts.size)
        assertTrue("isTest must be true when status is Test", alerts[0].isTest)
    }
}
