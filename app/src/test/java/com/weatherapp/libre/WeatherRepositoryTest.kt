package com.weatherapp.libre

import android.content.Context
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests for [WeatherRepository] hardening invariants:
 *  - cache TTL expiration (memory + disk)
 *  - disk-cache rehydration / fallback
 *  - in-flight request deduplication (non-blocking, shared calls)
 *  - network error mapping to Result.failure (no crash)
 *  - geocoding response parsing
 *
 * Follows the established project conventions: JUnit 4, Robolectric for a real
 * SharedPreferences-backed [LocationPreferences], and an OkHttp interceptor as the
 * HTTP fake (no mocking frameworks).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WeatherRepositoryTest {

    private lateinit var context: Context
    private lateinit var prefs: LocationPreferences

    private companion object {
        const val LAT = 43.6532
        const val LON = -79.3832

        // A COMPLETE forecast payload: current + daily + hourly all populated so that
        // isBestMatchNeeded() is false and a single getWeatherForecast() issues exactly
        // two upstream calls (forecast + air-quality). This keeps request counts stable.
        val CANNED_FORECAST_JSON = """
            {
              "latitude": 43.6532,
              "longitude": -79.3832,
              "elevation": 76.0,
              "timezone": "America/Toronto",
              "current": {
                "temperature_2m": 20.5,
                "relative_humidity_2m": 60,
                "apparent_temperature": 19.0,
                "weather_code": 2,
                "is_day": 1,
                "wind_speed_10m": 10.0,
                "wind_direction_10m": 180,
                "visibility": 10000,
                "uv_index": 3,
                "cloud_cover": 40
              },
              "daily": {
                "time": ["2026-01-15","2026-01-16"],
                "weather_code": [2,3],
                "temperature_2m_max": [22.0,21.0],
                "temperature_2m_min": [10.0,9.0],
                "sunrise": ["2026-01-15T07:30","2026-01-16T07:31"],
                "sunset": ["2026-01-15T16:45","2026-01-16T16:46"],
                "uv_index_max": [3.0,2.5],
                "wind_speed_10m_max": [15.0,14.0]
              },
              "hourly": {
                "time": ["2026-01-15T00:00","2026-01-15T01:00"],
                "temperature_2m": [10.0,11.0]
              }
            }
        """.trimIndent()

        val CANNED_GEOCODING_JSON = """
            {
              "results": [
                {
                  "id": 6054564,
                  "name": "Toronto",
                  "admin1": "Ontario",
                  "country": "Canada",
                  "country_code": "CA",
                  "latitude": 43.6532,
                  "longitude": -79.3832,
                  "elevation": 76.0
                }
              ]
            }
        """.trimIndent()
    }

    /** OkHttp interceptor that counts requests and returns a canned response. */
    private class CountingInterceptor(
        private val status: Int = 200,
        private val body: String = CANNED_FORECAST_JSON,
        private val delayMillis: Long = 0L,
        private val ioException: IOException? = null
    ) : Interceptor {
        val requestCount = AtomicInteger(0)

        override fun intercept(chain: Interceptor.Chain): Response {
            requestCount.incrementAndGet()
            if (delayMillis > 0L) Thread.sleep(delayMillis)
            if (ioException != null) throw ioException
            return Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(status)
                .message(if (status in 200..299) "OK" else "Server Error")
                .body(ResponseBody.create(null, body))
                .build()
        }
    }

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        // Start from a clean slate so tests never leak into one another.
        context.getSharedPreferences("weather_location_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
        prefs = LocationPreferences(context)
    }

    private fun cacheKeyFor(model: ForecastModel = ForecastModel.GEM): String =
        String.format(Locale.US, "cache_v2_%.4f_%.4f_%s", LAT, LON, model.openMeteoKey)

    // ---------------------------------------------------------------------
    // Cache TTL expiration
    // ---------------------------------------------------------------------

    @Test
    fun testCacheTtlExpiration() {
        val key = cacheKeyFor()
        val now = System.currentTimeMillis()
        val repo = WeatherRepository()

        // A disk entry written 2 hours ago is beyond the 60-minute TTL -> stale.
        prefs.savePersistentCache(key, CANNED_FORECAST_JSON, now - 2 * 60 * 60 * 1000L)
        assertFalse(
            "A cache entry older than the TTL must not be reported as valid",
            repo.hasValidCache(LAT, LON, ForecastModel.GEM, 60, prefs)
        )

        // A fresh disk entry within the TTL must be reported as valid.
        prefs.savePersistentCache(key, CANNED_FORECAST_JSON, now)
        assertTrue(
            "A cache entry within the TTL must be reported as valid",
            repo.hasValidCache(LAT, LON, ForecastModel.GEM, 60, prefs)
        )
    }

    // ---------------------------------------------------------------------
    // Disk-cache fallback / rehydration
    // ---------------------------------------------------------------------

    @Test
    fun testCacheDiskFallback() {
        val key = cacheKeyFor()
        val now = System.currentTimeMillis()
        val repo = WeatherRepository()

        // Seed the persistent (disk) cache; the in-memory cache on a fresh repo is empty.
        prefs.savePersistentCache(key, CANNED_FORECAST_JSON, now)

        val response = repo.getCachedForecast(LAT, LON, ForecastModel.GEM, prefs)
        assertNotNull("Disk fallback must return the cached response", response)
        val temp = response!!.current!!.temperature
        assertNotNull("Cached current temperature must be present", temp)
        assertEquals(20.5, temp!!, 0.0001)
        assertEquals("America/Toronto", response.timezone)

        // The rehydrated entry now satisfies hasValidCache (memory populated).
        assertTrue(
            "After disk rehydration the in-memory cache must be reported valid",
            repo.hasValidCache(LAT, LON, ForecastModel.GEM, 60, prefs)
        )
    }

    @Test
    fun testCacheDiskFallbackCorruptJsonReturnsNull() {
        val key = cacheKeyFor()
        val now = System.currentTimeMillis()
        val repo = WeatherRepository()

        // Corrupt disk JSON must be swallowed (no crash) and yield null.
        prefs.savePersistentCache(key, "{ this is : not valid json ]", now)
        assertNull(
            "Corrupt disk cache JSON must return null instead of throwing",
            repo.getCachedForecast(LAT, LON, ForecastModel.GEM, prefs)
        )
    }

    // ---------------------------------------------------------------------
    // In-flight request deduplication (non-blocking, shared calls)
    // ---------------------------------------------------------------------

    @Test
    fun testInFlightDeduplication() {
        // A 500ms artificial latency guarantees both concurrent callers observe the
        // same in-flight request instead of each enqueuing their own.
        val interceptor = CountingInterceptor(delayMillis = 500L)
        val repo = WeatherRepository(
            OkHttpClient.Builder().addInterceptor(interceptor).build()
        )

        runBlocking {
            val d1 = async { repo.getWeatherForecast(LAT, LON, ForecastModel.GEM) }
            val d2 = async { repo.getWeatherForecast(LAT, LON, ForecastModel.GEM) }
            val r1 = d1.await()
            val r2 = d2.await()
            assertTrue("First concurrent call must succeed", r1.isSuccess)
            assertTrue("Second concurrent call must succeed", r2.isSuccess)
        }

        // A complete forecast response triggers exactly 2 upstream calls (forecast +
        // air-quality). Both concurrent callers MUST share those 2 calls, not issue 4.
        assertEquals(
            "Two concurrent identical requests must share in-flight calls (2 total, not 4)",
            2,
            interceptor.requestCount.get()
        )
    }

    // ---------------------------------------------------------------------
    // Network error mapping (no crash)
    // ---------------------------------------------------------------------

    @Test
    fun testNetworkErrorMapping() {
        val interceptor = CountingInterceptor(status = 500)
        val repo = WeatherRepository(
            OkHttpClient.Builder().addInterceptor(interceptor).build()
        )

        val result = runBlocking { repo.getWeatherForecast(LAT, LON, ForecastModel.GEM) }

        assertTrue(
            "An HTTP 500 must map to Result.failure, not crash",
            result.isFailure
        )
        assertTrue(
            "The failure cause must be an IOException",
            result.exceptionOrNull() is IOException
        )
    }

    @Test
    fun testNetworkIoExceptionMapping() {
        val interceptor = CountingInterceptor(ioException = IOException("Network unreachable"))
        val repo = WeatherRepository(
            OkHttpClient.Builder().addInterceptor(interceptor).build()
        )

        val result = runBlocking { repo.getWeatherForecast(LAT, LON, ForecastModel.GEM) }

        assertTrue("An IOException must map to Result.failure, not crash", result.isFailure)
        val cause = result.exceptionOrNull()
        assertTrue("The failure cause must be the IOException", cause is IOException)
        assertEquals("Network unreachable", cause?.message)
    }

    // ---------------------------------------------------------------------
    // Geocoding parsing
    // ---------------------------------------------------------------------

    @Test
    fun testSearchLocations() {
        val interceptor = CountingInterceptor(body = CANNED_GEOCODING_JSON)
        val repo = WeatherRepository(
            OkHttpClient.Builder().addInterceptor(interceptor).build()
        )

        val result = runBlocking { repo.searchLocations("Toronto") }

        assertTrue("Geocoding must succeed", result.isSuccess)
        val items = result.getOrThrow()
        assertEquals("Exactly one geocoding result expected", 1, items.size)
        val first = items.first()
        assertEquals("Toronto", first.name)
        assertEquals("Ontario", first.admin1)
        assertEquals("CA", first.countryCode)
        assertEquals(43.6532, first.latitude, 0.0001)
        assertEquals(-79.3832, first.longitude, 0.0001)
    }

    @Test
    fun testSearchLocationsBlankQueryReturnsEmpty() {
        val interceptor = CountingInterceptor()
        val repo = WeatherRepository(
            OkHttpClient.Builder().addInterceptor(interceptor).build()
        )

        val result = runBlocking { repo.searchLocations("   ") }

        assertTrue("A blank query must short-circuit to success", result.isSuccess)
        assertTrue("A blank query must return an empty list", result.getOrThrow().isEmpty())
        assertEquals(
            "A blank query must not hit the network",
            0,
            interceptor.requestCount.get()
        )
    }
}
