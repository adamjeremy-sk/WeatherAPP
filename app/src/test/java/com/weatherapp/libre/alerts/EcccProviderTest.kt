package com.weatherapp.libre.alerts

import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.alerts.models.AlertFetchResult
import com.weatherapp.libre.alerts.models.AlertSource
import com.weatherapp.libre.alerts.providers.EcccProvider
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
}
