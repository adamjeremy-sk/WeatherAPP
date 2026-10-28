package com.weatherapp.libre.alerts

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.alerts.cache.GlobalAlertCache
import com.weatherapp.libre.alerts.models.AlertSeverity
import com.weatherapp.libre.alerts.models.AlertSource
import com.weatherapp.libre.alerts.models.UnifiedAlert
import com.weatherapp.libre.alerts.providers.MeteoAlarmProvider
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateApi
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateDetailedCap
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateFeature
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateFeatureCollection
import com.weatherapp.libre.alerts.providers.meteoalarm.MeteoGateLink
import com.weatherapp.libre.alerts.routing.AlertProviderRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale

object GlobalAlertTestHarness {

    private const val TAG = "GlobalAlertTestHarness"
    private val gson = Gson()

    enum class EcccTestFixture(val filename: String, val displayName: String) {
        TORNADO_WARNING("tornado_warning.xml", "1. Tornado Warning (Extreme / Immediate)"),
        THUNDERSTORM_WARNING("thunderstorm_warning.xml", "2. Severe Thunderstorm Warning (Severe)"),
        WINTER_STORM_WARNING("winter_storm_warning.xml", "3. Winter Storm Warning (Severe / Expected)"),
        SPECIAL_STATEMENT("special_weather_statement.xml", "4. Special Weather Statement (Moderate)"),
        TORNADO_UPDATE("tornado_update.xml", "5. Update Message (Supersedes Tornado Warning)"),
        TORNADO_CANCEL("tornado_cancel.xml", "6. Cancel Message (Dismisses Tornado Warning)"),
        SHORT_EXPIRY_TEST("tornado_warning.xml", "7. Short Expiry Test Alert (2 Minutes)")
    }

    enum class GlobalTestFixture(val filename: String, val displayName: String) {
        RED_WIND("eu_red_wind.json", "EU 1. Extreme Wind Warning (Red)"),
        ORANGE_RAIN("eu_orange_rain.json", "EU 2. Severe Heavy Rain Warning (Orange)"),
        YELLOW_FROST("eu_yellow_frost.json", "EU 3. Moderate Frost Advisory (Yellow)")
    }

    suspend fun injectEcccTestFixture(
        context: Context,
        fixture: EcccTestFixture,
        locations: List<LocationItem>,
        timezoneStr: String?
    ): List<UnifiedAlert> = withContext(Dispatchers.IO) {
        val eligibleLocations = locations.filter { loc ->
            loc.countryCode.equals("CA", ignoreCase = true) ||
                    loc.country.equals("Canada", ignoreCase = true)
        }

        if (eligibleLocations.isEmpty()) {
            Log.w(TAG, "Aborting ECCC test alert injection: No eligible Canadian locations found in list.")
            return@withContext emptyList()
        }

        val assetPath = "cap_fixtures/${fixture.filename}"
        val rawXml = try {
            context.assets.open(assetPath).use { it.bufferedReader().readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read test fixture $assetPath", e)
            return@withContext emptyList()
        }

        val injectedAlerts = mutableListOf<UnifiedAlert>()

        for (location in eligibleLocations) {
            val restampedXml = restampFixtureArea(rawXml, location)
            val parsedAlert = CapXmlParser.parse(restampedXml, isTestFixture = true) ?: continue

            val matchResult = AlertMatcher.matchesLocation(parsedAlert, location)
            if (matchResult.isMatched) {
                val now = Instant.now()
                val expirySeconds = if (fixture == EcccTestFixture.SHORT_EXPIRY_TEST) 120L else 7 * 24 * 3600L
                val futureExpiryStr = now.plusSeconds(expirySeconds).toString()

                val info = parsedAlert.infoList.firstOrNull() ?: continue
                val severity = when (info.severity.lowercase(Locale.US).trim()) {
                    "extreme" -> AlertSeverity.RED
                    "severe" -> AlertSeverity.ORANGE
                    "moderate" -> AlertSeverity.YELLOW
                    "minor" -> AlertSeverity.UNKNOWN
                    else -> AlertSeverity.UNKNOWN
                }

                val eventName = info.event.ifBlank { info.headline ?: "Weather Alert" }
                val headlineText = info.headline ?: "$eventName in effect for ${matchResult.matchedAreaDesc ?: location.displayName}"
                val uniqueId = "eccc_test_${parsedAlert.identifier}_${System.currentTimeMillis()}"

                val referencedIds = parsedAlert.references?.trim()?.split("\\s+".toRegex())?.mapNotNull { token ->
                    val parts = token.split(",")
                    if (parts.size >= 2 && parts[1].isNotBlank()) parts[1].trim()
                    else if (token.isNotBlank()) token.trim()
                    else null
                }?.distinct() ?: emptyList()

                val unifiedAlert = UnifiedAlert(
                    id = uniqueId,
                    source = AlertSource.ECCC,
                    locationLat = location.latitude,
                    locationLon = location.longitude,
                    severity = severity,
                    eventNative = eventName,
                    headline = headlineText,
                    description = info.description,
                    instruction = info.instruction,
                    onset = now.toString(),
                    expires = futureExpiryStr,
                    areaDesc = matchResult.matchedAreaDesc ?: info.areaList.firstOrNull()?.areaDesc ?: location.displayName,
                    msgType = parsedAlert.msgType,
                    references = referencedIds,
                    sent = parsedAlert.sent.toString(),
                    isTest = true
                )

                GlobalAlertCache.writeAlertsForLocation(context, location, listOf(unifiedAlert))
                AlertNotificationHelper.postUnifiedAlertNotification(
                    context = context,
                    alert = unifiedAlert,
                    location = location,
                    timezoneStr = timezoneStr,
                    isTestFixture = true
                )
                injectedAlerts.add(unifiedAlert)

                val alertTimestamp = try {
                    java.time.Instant.parse(unifiedAlert.sent ?: "").toEpochMilli()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }

                AlertLogRepository.saveEntry(
                    context,
                    AlertLogEntry(
                        id = System.currentTimeMillis(),
                        timestampUtc = alertTimestamp,
                        locationName = location.displayName,
                        alertEvent = unifiedAlert.eventNative,
                        severity = unifiedAlert.severity.name,
                        isTest = true
                    )
                )
            }
        }

        return@withContext injectedAlerts
    }

    suspend fun injectGlobalTestFixture(
        context: Context,
        fixture: GlobalTestFixture,
        locations: List<LocationItem>,
        timezoneStr: String? = null
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

        var currentDetailedCap = MeteoGateDetailedCap()

        val mockApi = object : MeteoGateApi {
            override suspend fun getWarningsForCountry(countryCode: String, datetime: String): MeteoGateFeatureCollection {
                val syntheticFeature = MeteoGateFeature(
                    id = "test_feature_1",
                    links = listOf(
                        MeteoGateLink(
                            rel = "json",
                            type = "application/json",
                            href = "mock://tier2/fixture"
                        )
                    )
                )
                return MeteoGateFeatureCollection(features = listOf(syntheticFeature))
            }

            override suspend fun getDetailedCapJson(url: String): MeteoGateDetailedCap {
                if (url == "mock://tier2/fixture") {
                    return currentDetailedCap
                }
                return MeteoGateDetailedCap()
            }
        }

        val provider = MeteoAlarmProvider(api = mockApi)
        val allInjectedAlerts = mutableListOf<UnifiedAlert>()

        for (location in eligibleLocations) {
            val restampedJson = restampFixtureJson(jsonString, location)
            currentDetailedCap = try {
                gson.fromJson(restampedJson, MeteoGateDetailedCap::class.java)
                    ?: MeteoGateDetailedCap()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse restamped global fixture for ${location.displayName}", e)
                MeteoGateDetailedCap()
            }

            val fetchedAlerts = when (val result = provider.fetchAlertsForLocation(location)) {
                is com.weatherapp.libre.alerts.models.AlertFetchResult.Success -> result.alerts
                is com.weatherapp.libre.alerts.models.AlertFetchResult.Failure -> emptyList()
            }
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

            for (alert in restampedAlerts) {
                AlertNotificationHelper.postUnifiedAlertNotification(
                    context = context,
                    alert = alert,
                    location = location,
                    timezoneStr = timezoneStr,
                    isTestFixture = true
                )

                val alertTimestamp = try {
                    java.time.Instant.parse(alert.sent ?: "").toEpochMilli()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }

                AlertLogRepository.saveEntry(
                    context,
                    AlertLogEntry(
                        id = System.currentTimeMillis(),
                        timestampUtc = alertTimestamp,
                        locationName = location.displayName,
                        alertEvent = alert.eventNative,
                        severity = alert.severity.name,
                        isTest = true
                    )
                )
            }
        }

        return@withContext allInjectedAlerts
    }

    private fun restampFixtureArea(xmlString: String, location: LocationItem): String {
        val circleRegex = Regex("""<circle>[^<]+</circle>""", RegexOption.IGNORE_CASE)
        val newCircle = String.format(Locale.US, "<circle>%.4f,%.4f 50.0</circle>", location.latitude, location.longitude)
        var updated = circleRegex.replace(xmlString, newCircle)

        val areaDescRegex = Regex("""<areaDesc>[^<]+</areaDesc>""", RegexOption.IGNORE_CASE)
        val newAreaDesc = "<areaDesc>${location.displayName} Region (Test)</areaDesc>"
        updated = areaDescRegex.replace(updated, newAreaDesc)

        return updated
    }

    private fun restampFixtureJson(rawJson: String, location: LocationItem): String {
        val lat = location.latitude
        val lon = location.longitude
        val polyStr = String.format(
            Locale.US,
            "%.4f,%.4f %.4f,%.4f %.4f,%.4f %.4f,%.4f %.4f,%.4f",
            lat - 0.5, lon - 0.5,
            lat - 0.5, lon + 0.5,
            lat + 0.5, lon + 0.5,
            lat + 0.5, lon - 0.5,
            lat - 0.5, lon - 0.5
        )

        var updated = rawJson

        val areaDescRegex = Regex(""""areaDesc"\s*:\s*"[^"]*"""")
        updated = areaDescRegex.replace(updated, """"areaDesc": "${location.displayName} Region (Test)"""")

        val polygonRegex = Regex(""""polygon"\s*:\s*\[\s*"[^"]*"\s*]""")
        updated = polygonRegex.replace(updated, """"polygon": [ "$polyStr" ]""")

        return updated
    }
}