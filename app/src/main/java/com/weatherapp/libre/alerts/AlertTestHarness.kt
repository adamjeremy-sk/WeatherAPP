package com.weatherapp.libre.alerts

import android.content.Context
import android.util.Log
import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.alerts.cache.GlobalAlertCache
import com.weatherapp.libre.alerts.models.AlertSeverity
import com.weatherapp.libre.alerts.models.AlertSource
import com.weatherapp.libre.alerts.models.CapAlert
import com.weatherapp.libre.alerts.models.UnifiedAlert
import java.util.Locale

object AlertTestHarness {

    private const val TAG = "AlertTestHarness"

    enum class TestFixture(val filename: String, val displayName: String) {
        TORNADO_WARNING("tornado_warning.xml", "1. Tornado Warning (Extreme / Immediate)"),
        THUNDERSTORM_WARNING("thunderstorm_warning.xml", "2. Severe Thunderstorm Warning (Severe)"),
        WINTER_STORM_WARNING("winter_storm_warning.xml", "3. Winter Storm Warning (Severe / Expected)"),
        SPECIAL_STATEMENT("special_weather_statement.xml", "4. Special Weather Statement (Moderate)"),
        TORNADO_UPDATE("tornado_update.xml", "5. Update Message (Supersedes Tornado Warning)"),
        TORNADO_CANCEL("tornado_cancel.xml", "6. Cancel Message (Dismisses Tornado Warning)"),
        SHORT_EXPIRY_TEST("tornado_warning.xml", "7. Short Expiry Test Alert (2 Minutes)")
    }

    fun injectTestFixture(
        context: Context,
        fixture: TestFixture,
        locations: List<LocationItem>,
        timezoneStr: String?,
        alertManager: AlertManager
    ): CapAlert? {
        val eligibleLocations = locations.filter { loc ->
            loc.countryCode.equals("CA", ignoreCase = true) ||
                    loc.country.equals("Canada", ignoreCase = true)
        }

        if (eligibleLocations.isEmpty()) {
            Log.w(TAG, "Aborting ECCC test alert injection: No eligible Canadian locations found in list.")
            return null
        }

        val assetPath = "cap_fixtures/${fixture.filename}"
        val rawXml = try {
            context.assets.open(assetPath).use { it.bufferedReader().readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read test fixture $assetPath", e)
            return null
        }

        var lastInjectedAlert: CapAlert? = null

        for (location in eligibleLocations) {
            // Re-stamp fixture area circle centered on location coordinates (radius 50km)
            val restampedXml = restampFixtureArea(rawXml, location)

            // Parse XML with CapXmlParser
            val parsedAlert = CapXmlParser.parse(restampedXml, isTestFixture = true)
            if (parsedAlert == null) {
                Log.e(TAG, "Failed to parse test fixture XML for ${fixture.displayName} at ${location.displayName}")
                continue
            }

            // Run through real AlertMatcher
            val matchResult = AlertMatcher.matchesLocation(parsedAlert, location)
            Log.d(TAG, "Test Harness Matcher result for ${fixture.displayName} at ${location.displayName}: matched=${matchResult.isMatched}, rule=${matchResult.rule}")

            if (matchResult.isMatched) {
                val now = java.time.Instant.now()
                val expirySeconds = if (fixture == TestFixture.SHORT_EXPIRY_TEST) 120L else 3600L
                val futureExpiry = now.plusSeconds(expirySeconds)
                val updatedInfoList = parsedAlert.infoList.map { info ->
                    info.copy(expires = futureExpiry)
                }
                val syntheticAlert = parsedAlert.copy(
                    isSyntheticTest = true,
                    infoList = updatedInfoList
                )

                val unifiedAlert = mapCapAlertToUnifiedAlert(
                    capAlert = syntheticAlert,
                    location = location,
                    matchedAreaDesc = matchResult.matchedAreaDesc
                )

                if (unifiedAlert != null) {
                    val existing = GlobalAlertCache.readAlertsForLocation(context, location).toMutableList()
                    existing.removeAll { it.id.startsWith("eccc_test_") }
                    existing.add(unifiedAlert)
                    GlobalAlertCache.writeAlertsForLocation(context, location, existing)

                    AlertNotificationHelper.postUnifiedAlertNotification(
                        context = context,
                        alert = unifiedAlert,
                        location = location,
                        timezoneStr = timezoneStr,
                        isTestFixture = true
                    )

                    Log.d(TAG, "Injected ECCC test alert into GlobalAlertCache for ${location.displayName}")
                    lastInjectedAlert = syntheticAlert
                }
            } else {
                lastInjectedAlert = parsedAlert
            }
        }

        return lastInjectedAlert
    }

    private fun mapCapAlertToUnifiedAlert(
        capAlert: CapAlert,
        location: LocationItem,
        matchedAreaDesc: String?
    ): UnifiedAlert? {
        val info = capAlert.infoList.firstOrNull() ?: return null
        val severity = when (info.severity.lowercase(Locale.US).trim()) {
            "extreme" -> AlertSeverity.RED
            "severe" -> AlertSeverity.ORANGE
            "moderate" -> AlertSeverity.YELLOW
            "minor" -> AlertSeverity.UNKNOWN
            else -> AlertSeverity.UNKNOWN
        }

        val eventName = info.event.ifBlank { info.headline ?: "Weather Alert" }
        val headlineText = info.headline ?: "$eventName in effect for ${matchedAreaDesc ?: location.displayName}"
        val uniqueId = "eccc_test_${capAlert.identifier}_${System.currentTimeMillis()}"

        return UnifiedAlert(
            id = uniqueId,
            source = AlertSource.ECCC,
            locationLat = location.latitude,
            locationLon = location.longitude,
            severity = severity,
            eventNative = eventName,
            headline = headlineText,
            description = info.description,
            instruction = info.instruction,
            onset = info.onset?.toString(),
            expires = info.expires?.toString(),
            areaDesc = matchedAreaDesc ?: info.areaList.firstOrNull()?.areaDesc ?: location.displayName
        )
    }

    fun restampFixtureArea(xmlString: String, location: LocationItem): String {
        val circleRegex = Regex("""<circle>[^<]+</circle>""", RegexOption.IGNORE_CASE)
        val newCircle = String.format(Locale.US, "<circle>%.4f,%.4f 50.0</circle>", location.latitude, location.longitude)
        var updated = circleRegex.replace(xmlString, newCircle)

        val areaDescRegex = Regex("""<areaDesc>[^<]+</areaDesc>""", RegexOption.IGNORE_CASE)
        val newAreaDesc = "<areaDesc>${location.displayName} Region (Test)</areaDesc>"
        updated = areaDescRegex.replace(updated, newAreaDesc)

        return updated
    }
}
