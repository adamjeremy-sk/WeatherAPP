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
        TORNADO_CANCEL("tornado_cancel.xml", "6. Cancel Message (Dismisses Tornado Warning)")
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
            // Process through AlertManager pipeline
            alertManager.processCapAlert(
                alert = parsedAlert,
                location = location,
                timezoneStr = timezoneStr,
                isForegroundActiveLocation = true
            )
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
