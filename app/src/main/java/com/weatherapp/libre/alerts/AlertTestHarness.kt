package com.weatherapp.libre.alerts

import android.content.Context
import android.util.Log
import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.LocationPreferences
import com.weatherapp.libre.alerts.models.CapAlert
import java.io.InputStream
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
        location: LocationItem,
        timezoneStr: String?,
        alertManager: AlertManager
    ): CapAlert? {
        val assetPath = "cap_fixtures/${fixture.filename}"
        val rawXml = try {
            context.assets.open(assetPath).use { it.bufferedReader().readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read test fixture $assetPath", e)
            return null
        }

        // Re-stamp fixture area circle centered on active location coordinates (radius 50km)
        val restampedXml = restampFixtureArea(rawXml, location)

        // Parse XML with CapXmlParser
        val parsedAlert = CapXmlParser.parse(restampedXml, isTestFixture = true)
        if (parsedAlert == null) {
            Log.e(TAG, "Failed to parse test fixture XML for ${fixture.displayName}")
            return null
        }

        // Run through real AlertMatcher
        val matchResult = AlertMatcher.matchesLocation(parsedAlert, location)
        Log.d(TAG, "Test Harness Matcher result for ${fixture.displayName}: matched=${matchResult.isMatched}, rule=${matchResult.rule}")

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

            // Process through AlertManager pipeline (updates in-memory state and saves to AlertLogRepository)
            alertManager.processCapAlert(
                alert = syntheticAlert,
                location = location,
                timezoneStr = timezoneStr,
                isForegroundActiveLocation = true
            )
            
            // Write directly to AlertCache with future expiry (at least +1 hour in the future)
            with(AlertCache) {
                val cached = syntheticAlert.toCachedAlert(location, isSyntheticTest = true)
                if (cached != null) {
                    val nowMs = System.currentTimeMillis()
                    val expiryOffsetMs = if (fixture == TestFixture.SHORT_EXPIRY_TEST) 120000L else 3600000L
                    val futureExpiresMs = maxOf(cached.expiresUtcMs ?: (nowMs + expiryOffsetMs), nowMs + expiryOffsetMs)
                    val futureCached = cached.copy(
                        expiresUtcMs = futureExpiresMs,
                        isSyntheticTest = true
                    )
                    AlertCache.writeCacheForLocation(context, location, listOf(futureCached))
                }
            }
            return syntheticAlert
        }

        return parsedAlert
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
