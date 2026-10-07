package com.weatherapp.libre

import com.weatherapp.libre.lunar.LunarCalculator
import com.weatherapp.libre.lunar.Observer as LunarObserver
import com.weatherapp.libre.solar.Observer as SolarObserver
import com.weatherapp.libre.solar.SolarLightingEngine
import com.weatherapp.libre.solar.SolarPositionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

class DarkSkyManager {

    suspend fun calculateDarkSkyWindows(
        dates: List<LocalDate>,
        zoneId: ZoneId,
        location: LocationItem
    ): List<DarkSkyDaySummary> = withContext(Dispatchers.Default) {
        val solarObserver = SolarObserver(
            latitudeDegrees = location.latitude,
            longitudeDegrees = location.longitude,
            elevationMeters = location.elevationMeters
        )
        val lunarObserver = LunarObserver(
            latitudeDegrees = location.latitude,
            longitudeDegrees = location.longitude,
            elevationMeters = location.elevationMeters
        )

        dates.map { date ->
            calculateDayDarkSky(date, zoneId, solarObserver, lunarObserver)
        }
    }

    private fun calculateDayDarkSky(
        date: LocalDate,
        zoneId: ZoneId,
        solarObserver: SolarObserver,
        lunarObserver: LunarObserver
    ): DarkSkyDaySummary {
        val lightingToday = SolarLightingEngine.calculateSolarLightingPhases(date, zoneId, solarObserver)
        val nextDate = date.plusDays(1)
        val lightingTomorrow = SolarLightingEngine.calculateSolarLightingPhases(nextDate, zoneId, solarObserver)

        // Night interval: Sunset today to Sunrise tomorrow
        val nightStart = lightingToday.sunset?.instantUtc
        val nightEnd = lightingTomorrow.sunrise?.instantUtc

        if (nightStart == null && nightEnd == null) {
            val isMidnightSun = (lightingToday.solarNoon?.apparentAltitudeDegrees ?: 0.0) > 0.0
            val note = if (isMidnightSun) {
                "Continuous daylight (Sun does not set) • No night sky darkness"
            } else {
                "Polar night • Continuous winter darkness"
            }
            return DarkSkyDaySummary(date, emptyList(), note)
        }

        // Astronomical darkness interval: evening astro twilight end today to morning astro twilight start tomorrow
        val astroNightStart = lightingToday.eveningAstronomicalTwilight?.endInstantUtc
        val astroNightEnd = lightingTomorrow.astronomicalTwilight?.startInstantUtc

        if (astroNightStart == null || astroNightEnd == null || !astroNightStart.isBefore(astroNightEnd)) {
            return DarkSkyDaySummary(
                date,
                emptyList(),
                "No true astronomical darkness (Sun remains above -18° twilight boundary)"
            )
        }

        // Find moonrise / moonset events during astronomical darkness
        val moonEventsToday = LunarCalculator.moonEventsForLocalDate(date, zoneId, lunarObserver)
        val moonEventsTomorrow = LunarCalculator.moonEventsForLocalDate(nextDate, zoneId, lunarObserver)
        val allMoonEvents = (moonEventsToday + moonEventsTomorrow).distinctBy { it.instantUtc }

        val internalEvents = allMoonEvents.filter {
            it.instantUtc.isAfter(astroNightStart) && it.instantUtc.isBefore(astroNightEnd)
        }.map { it.instantUtc }

        val boundaries = (listOf(astroNightStart) + internalEvents + listOf(astroNightEnd))
            .distinct()
            .sorted()

        val windows = mutableListOf<DarkSkyWindow>()

        for (i in 0 until boundaries.size - 1) {
            val wStart = boundaries[i]
            val wEnd = boundaries[i + 1]
            val durationMin = Duration.between(wStart, wEnd).toMinutes().toInt()

            if (durationMin < 1) continue

            // Evaluate Moon Interference Index across window
            val steps = (durationMin.coerceAtMost(60)).coerceAtLeast(2)
            val stepSecs = Duration.between(wStart, wEnd).seconds / steps.toDouble()

            var sumInterference = 0.0
            var allMoonBelow = true

            for (s in 0..steps) {
                val sampleInstant = wStart.plusSeconds((s * stepSecs).toLong())
                val lunarState = LunarCalculator.topocentricHorizontalState(sampleInstant, lunarObserver)
                val moonAlt = lunarState.altitudeDegrees
                val illum = LunarCalculator.phaseAt(sampleInstant, lunarObserver).illuminationFraction

                if (moonAlt > 0.0) {
                    allMoonBelow = false
                    val altFactor = (moonAlt / 30.0).coerceIn(0.0, 1.0)
                    val smoothAlt = altFactor * altFactor * (3.0 - 2.0 * altFactor)
                    sumInterference += illum * smoothAlt * 100.0
                }
            }

            val avgInterference = if (allMoonBelow) 0 else (sumInterference / (steps + 1)).roundToInt().coerceIn(0, 100)

            val midInstant = wStart.plusSeconds(Duration.between(wStart, wEnd).seconds / 2)
            val midLunarState = LunarCalculator.topocentricHorizontalState(midInstant, lunarObserver)
            val midIllum = LunarCalculator.phaseAt(midInstant, lunarObserver).illuminationFraction
            val illumPct = (midIllum * 100).roundToInt()

            val moonStatus = if (midLunarState.altitudeDegrees <= 0.0) {
                "Moon below horizon"
            } else {
                "Moon above horizon ($illumPct% ill.)"
            }

            val qualityLabel = when {
                allMoonBelow || avgInterference < 10 -> {
                    if (durationMin < 15 && !allMoonBelow) "Good"
                    else if (durationMin < 5) "Good"
                    else "Excellent"
                }
                avgInterference < 25 -> "Good"
                avgInterference < 50 -> "Moderate"
                else -> "Poor"
            }

            windows.add(
                DarkSkyWindow(
                    startUtc = wStart,
                    endUtc = wEnd,
                    durationMinutes = durationMin,
                    qualityLabel = qualityLabel,
                    interferenceIndex = avgInterference,
                    moonStatus = moonStatus
                )
            )
        }

        return DarkSkyDaySummary(date, windows)
    }
}
