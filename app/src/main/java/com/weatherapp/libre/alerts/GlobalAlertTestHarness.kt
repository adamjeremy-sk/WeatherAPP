package com.weatherapp.libre.alerts

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.alerts.cache.GlobalAlertCache
import com.weatherapp.libre.alerts.models.UnifiedAlert
import com.weatherapp.libre.alerts.providers.MeteoAlarmProvider
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateApi
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateFeatureCollection
import com.weatherapp.libre.alerts.routing.AlertProviderRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale

object GlobalAlertTestHarness {

    private const val TAG = "GlobalAlertTestHarness"
    private val gson = Gson()

    enum class GlobalTestFixture(val filename: String, val displayName: String) {
        RED_WIND("eu_red_wind.json", "EU 1. Extreme Wind Warning (Red)"),
        ORANGE_RAIN("eu_orange_rain.json", "EU 2. Severe Heavy Rain Warning (Orange)"),
        YELLOW_FROST("eu_yellow_frost.json", "EU 3. Moderate Frost Advisory (Yellow)")
    }

    suspend fun injectGlobalTestFixture(
        context: Context,
        fixture: GlobalTestFixture,
        locations: List<LocationItem>
    ): List<UnifiedAlert> = withContext(Dispatchers.IO) {
        val eligibleLocations = locations.filter { loc ->
            val countryCode = loc.countryCode?.uppercase(Locale.US) ?: ""
            AlertProviderRouter.METEOALARM_COUNTRY_CODES.contains(countryCode)
        }

        if (eligibleLocations.isEmpty()) {
            Log.w(TAG, "Aborting Global test alert injection: No eligible MeteoAlarm locations found in list.")
            return@withContext emptyList()
        }

        val assetPath = "meteoalarm_fixtures/${fixture.filename}"
        val jsonString = try {
            context.assets.open(assetPath).use { it.bufferedReader().readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read global test fixture $assetPath", e)
            return@withContext emptyList()
        }

        val mockApi = object : MeteoGateApi {
            override suspend fun getWarningsForPosition(coords: String): MeteoGateFeatureCollection {
                return try {
                    gson.fromJson(jsonString, MeteoGateFeatureCollection::class.java)
                        ?: MeteoGateFeatureCollection()
                } catch (e: Exception) {
                    MeteoGateFeatureCollection()
                }
            }

            override suspend fun getWarningsItems(bbox: String?): MeteoGateFeatureCollection {
                return getWarningsForPosition("")
            }
        }

        val provider = MeteoAlarmProvider(api = mockApi)
        val allInjectedAlerts = mutableListOf<UnifiedAlert>()

        for (location in eligibleLocations) {
            val fetchedAlerts = provider.fetchAlertsForLocation(location)
            val futureExpiryStr = Instant.now().plus(7, ChronoUnit.DAYS).toString()
            val restampedAlerts = fetchedAlerts.map { alert ->
                val uniqueId = "${alert.id}_${System.currentTimeMillis()}"
                alert.copy(
                    id = uniqueId,
                    onset = Instant.now().toString(),
                    expires = futureExpiryStr,
                    areaDesc = "${location.displayName} Region (Test)"
                )
            }

            GlobalAlertCache.writeAlertsForLocation(context, location, restampedAlerts)
            Log.d(TAG, "Injected ${restampedAlerts.size} global test alerts for location ${location.displayName}")
            allInjectedAlerts.addAll(restampedAlerts)
        }

        return@withContext allInjectedAlerts
    }
}
