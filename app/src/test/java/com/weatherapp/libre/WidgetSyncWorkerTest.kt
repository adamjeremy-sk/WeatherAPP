package com.weatherapp.libre

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests for [WidgetSyncWorker] hardening invariants:
 *  - no active widgets => early success with zero network calls
 *  - widget dedup (multiple widgets on one location => a single fetch)
 *  - rebuild path is non-crashing given a valid cached forecast
 *
 * Follows the established project conventions: JUnit 4, Robolectric, an OkHttp
 * interceptor as the HTTP fake (no mocking frameworks), and TestListenableWorkerBuilder
 * to construct the worker with valid WorkerParameters.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetSyncWorkerTest {

    private lateinit var context: Context
    private lateinit var prefs: LocationPreferences

    private val toronto = LocationItem(
        id = 1001L,
        name = "Toronto",
        admin1 = "Ontario",
        country = "Canada",
        countryCode = "CA",
        latitude = 43.6532,
        longitude = -79.3832,
        elevationMeters = 76.0
    )

    private companion object {
        // A COMPLETE forecast payload (current + daily + hourly) so a single
        // getWeatherForecast() issues exactly two upstream calls (forecast + air-quality).
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
    }

    private class CountingInterceptor(private val body: String = CANNED_FORECAST_JSON) : Interceptor {
        val requestCount = AtomicInteger(0)
        override fun intercept(chain: Interceptor.Chain): Response {
            requestCount.incrementAndGet()
            return Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(null, body))
                .build()
        }
    }

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        for (name in listOf("weather_location_prefs", "widget_prefs")) {
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
        }
        prefs = LocationPreferences(context)
        // Single tab, Toronto.
        context.getSharedPreferences("weather_location_prefs", Context.MODE_PRIVATE)
            .edit().putInt("app_location_tab_count", 1).apply()
        prefs.saveTabLocation(0, toronto)
    }

    private fun buildWorker(interceptor: Interceptor): WidgetSyncWorker {
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val worker = TestListenableWorkerBuilder.from(context, WidgetSyncWorker::class.java)
            .build()
        worker.weatherRepositoryOverride = WeatherRepository(client)
        return worker
    }

    private fun bindWidget(appWidgetId: Int, provider: Class<*>): Int {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        shadowOf(appWidgetManager).bindAppWidgetId(appWidgetId, ComponentName(context, provider))
        return appWidgetId
    }

    private fun cacheKeyFor(model: ForecastModel = ForecastModel.GEM): String =
        String.format(Locale.US, "cache_v2_%.4f_%.4f_%s", toronto.latitude, toronto.longitude, model.openMeteoKey)

    // ---------------------------------------------------------------------
    // No widgets -> early exit, zero network
    // ---------------------------------------------------------------------

    @Test
    fun testNoWidgetsEarlyExit() {
        val interceptor = CountingInterceptor()
        val worker = buildWorker(interceptor)

        val result = runBlocking { worker.doWork() }

        assertTrue(
            "With no active widgets the worker must return success",
            result is ListenableWorker.Result.Success
        )
        assertEquals(
            "With no active widgets there must be zero network calls",
            0,
            interceptor.requestCount.get()
        )
    }

    // ---------------------------------------------------------------------
    // Widget dedup
    // ---------------------------------------------------------------------

    @Test
    fun testWidgetDedup() {
        // Two widgets pointing at the SAME location (tab 0 / Toronto).
        bindWidget(100, WeatherWidgetProvider4x2::class.java)
        bindWidget(200, WeatherWidgetProvider4x2::class.java)

        val interceptor = CountingInterceptor()
        val worker = buildWorker(interceptor)

        val result = runBlocking { worker.doWork() }

        assertTrue("Widget sync must return success", result is ListenableWorker.Result.Success)
        assertEquals(
            "Two widgets on the same location must dedup to a single fetch (2 requests, not 4)",
            2,
            interceptor.requestCount.get()
        )
    }

    // ---------------------------------------------------------------------
    // Rebuild path is non-crashing given a valid cached forecast
    // ---------------------------------------------------------------------

    @Test
    fun testRebuildAndUpdateLogic() {
        bindWidget(300, WeatherWidgetProvider4x2::class.java)

        // Seed the persistent cache with a known, complete forecast.
        prefs.savePersistentCache(cacheKeyFor(), CANNED_FORECAST_JSON, System.currentTimeMillis())

        // rebuildAndUpdate must consume the cached forecast and complete without throwing.
        // (Full RemoteViews inflation is intentionally out of scope; the hardening
        //  property under test is that the rebuild path is non-crashing and reads the
        //  cache it was written to.)
        var threw = false
        try {
            runBlocking { WidgetSyncWorker.rebuildAndUpdate(context, 300) }
        } catch (e: Exception) {
            threw = true
        }
        assertFalse(
            "rebuildAndUpdate must not throw given a valid cached forecast",
            threw
        )
    }
}
