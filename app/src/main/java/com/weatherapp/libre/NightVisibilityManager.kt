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
import kotlin.math.max
import kotlin.math.roundToInt

class NightVisibilityManager {

    suspend fun calculateVisibilityPeriods(
        dates: List<LocalDate>,
        zoneId: ZoneId,
        location: LocationItem,
        hourlyCloudCoverMap: Map<Instant, Double>? = null
    ): List<NightVisibilityDaySummary> = withContext(Dispatchers.Default) {
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
            calculateDayVisibility(date, zoneId, location, solarObserver, lunarObserver, hourlyCloudCoverMap)
        }
    }

    private fun calculateDayVisibility(
        date: LocalDate,
        zoneId: ZoneId,
        location: LocationItem,
        solarObserver: SolarObserver,
        lunarObserver: LunarObserver,
        hourlyCloudCoverMap: Map<Instant, Double>?
    ): NightVisibilityDaySummary {
        val lightingToday = SolarLightingEngine.calculateSolarLightingPhases(date, zoneId, solarObserver)
        val nextDate = date.plusDays(1)
        val lightingTomorrow = SolarLightingEngine.calculateSolarLightingPhases(nextDate, zoneId, solarObserver)

        val nightStart = lightingToday.sunset?.instantUtc ?: date.atStartOfDay(zoneId).toInstant()
        val nightEnd = lightingTomorrow.sunrise?.instantUtc ?: nextDate.atStartOfDay(zoneId).toInstant()

        if (lightingToday.sunset?.instantUtc == null && lightingTomorrow.sunrise?.instantUtc == null) {
            val isMidnightSun = (lightingToday.solarNoon?.apparentAltitudeDegrees ?: 0.0) > 0.0
            val note = if (isMidnightSun) {
                "Continuous daylight (Sun does not set)"
            } else {
                "Polar night (Sun remains below horizon)"
            }
            return NightVisibilityDaySummary(date, emptyList(), cloudCoverAvailable = (hourlyCloudCoverMap != null), note = note)
        }

        // Collect key event boundaries
        val events = mutableSetOf<Instant>()
        events.add(nightStart)
        events.add(nightEnd)

        lightingToday.eveningCivilTwilight?.endInstantUtc?.let { if (it.isAfter(nightStart) && it.isBefore(nightEnd)) events.add(it) }
        lightingToday.eveningNauticalTwilight?.endInstantUtc?.let { if (it.isAfter(nightStart) && it.isBefore(nightEnd)) events.add(it) }
        lightingToday.eveningAstronomicalTwilight?.endInstantUtc?.let { if (it.isAfter(nightStart) && it.isBefore(nightEnd)) events.add(it) }

        lightingTomorrow.astronomicalTwilight?.startInstantUtc?.let { if (it.isAfter(nightStart) && it.isBefore(nightEnd)) events.add(it) }
        lightingTomorrow.nauticalTwilight?.startInstantUtc?.let { if (it.isAfter(nightStart) && it.isBefore(nightEnd)) events.add(it) }
        lightingTomorrow.civilTwilight?.startInstantUtc?.let { if (it.isAfter(nightStart) && it.isBefore(nightEnd)) events.add(it) }

        val moonEventsToday = LunarCalculator.moonEventsForLocalDate(date, zoneId, lunarObserver)
        val moonEventsTomorrow = LunarCalculator.moonEventsForLocalDate(nextDate, zoneId, lunarObserver)
        (moonEventsToday + moonEventsTomorrow).forEach { ev ->
            if (ev.instantUtc.isAfter(nightStart) && ev.instantUtc.isBefore(nightEnd)) {
                events.add(ev.instantUtc)
            }
        }

        // Add top-of-hour boundaries for cloud cover alignment
        var curHour = nightStart.atZone(zoneId).truncatedTo(java.time.temporal.ChronoUnit.HOURS).plusHours(1).toInstant()
        while (curHour.isBefore(nightEnd)) {
            if (curHour.isAfter(nightStart)) events.add(curHour)
            curHour = curHour.plus(1, java.time.temporal.ChronoUnit.HOURS)
        }

        val sortedBoundaries = events.sorted()
        val rawPeriods = mutableListOf<VisibilityPeriod>()

        for (i in 0 until sortedBoundaries.size - 1) {
            val pStart = sortedBoundaries[i]
            val pEnd = sortedBoundaries[i + 1]

            if (Duration.between(pStart, pEnd).seconds < 60) continue

            val midInstant = pStart.plusSeconds(Duration.between(pStart, pEnd).seconds / 2)

            val solarPos = SolarPositionEngine.computeTopocentric(midInstant, solarObserver)
            val sunAlt = solarPos.apparentAltitudeDegrees

            val lunarState = LunarCalculator.topocentricHorizontalState(midInstant, lunarObserver)
            val moonAlt = lunarState.altitudeDegrees
            val moonIllum = LunarCalculator.phaseAt(midInstant, lunarObserver).illuminationFraction

            // Ambient Light Model
            val moonAltitudeFactor = (moonAlt / 30.0).coerceIn(0.0, 1.0)
            val smoothMoonFactor = moonAltitudeFactor * moonAltitudeFactor * (3.0 - 2.0 * moonAltitudeFactor)
            val lunarPotential = moonIllum * smoothMoonFactor

            val solarTwilightFactor = ((sunAlt + 18.0) / 18.0).coerceIn(0.0, 1.0)
            val smoothSolarFactor = solarTwilightFactor * solarTwilightFactor * (3.0 - 2.0 * solarTwilightFactor)

            val clearAmbientPotential = max(smoothSolarFactor, lunarPotential).coerceIn(0.0, 1.0)

            val cloudFraction = getCloudCoverFraction(midInstant, hourlyCloudCoverMap)
            val effectiveAmbientPotential = if (cloudFraction != null) {
                val cloudAttenuation = 0.85
                clearAmbientPotential * (1.0 - cloudAttenuation * cloudFraction)
            } else {
                clearAmbientPotential
            }

            val category = when {
                effectiveAmbientPotential < 0.02 -> "No Natural Light"
                effectiveAmbientPotential < 0.10 -> "Minimal Light"
                effectiveAmbientPotential < 0.30 -> "Low Light"
                effectiveAmbientPotential < 0.60 -> "Moderate Light"
                else -> "Bright Night"
            }

            val twilightStatus = when {
                sunAlt > -6.0 -> "Civil Twilight"
                sunAlt > -12.0 -> "Nautical Twilight"
                sunAlt > -18.0 -> "Astronomical Twilight"
                else -> "Astronomical Night"
            }

            val cloudCoverStatus = cloudFraction?.let { "${(it * 100).roundToInt()}% Cloud Cover" }

            rawPeriods.add(
                VisibilityPeriod(
                    startUtc = pStart,
                    endUtc = pEnd,
                    category = category,
                    moonIllumination = moonIllum,
                    moonAltitude = moonAlt,
                    twilightStatus = twilightStatus,
                    cloudCoverStatus = cloudCoverStatus
                )
            )
        }

        // Merge adjacent identical periods
        val mergedPeriods = mutableListOf<VisibilityPeriod>()
        for (p in rawPeriods) {
            val last = mergedPeriods.lastOrNull()
            if (last != null &&
                last.category == p.category &&
                last.twilightStatus == p.twilightStatus &&
                last.cloudCoverStatus == p.cloudCoverStatus
            ) {
                mergedPeriods[mergedPeriods.size - 1] = last.copy(endUtc = p.endUtc)
            } else {
                mergedPeriods.add(p)
            }
        }

        return NightVisibilityDaySummary(
            date = date,
            periods = mergedPeriods,
            cloudCoverAvailable = (hourlyCloudCoverMap != null && hourlyCloudCoverMap.isNotEmpty())
        )
    }

    private fun getCloudCoverFraction(instant: Instant, hourlyCloudCoverMap: Map<Instant, Double>?): Double? {
        if (hourlyCloudCoverMap == null || hourlyCloudCoverMap.isEmpty()) return null
        // Find nearest hour in map
        val closest = hourlyCloudCoverMap.entries.minByOrNull {
            Math.abs(Duration.between(it.key, instant).seconds)
        }
        return if (closest != null && Math.abs(Duration.between(closest.key, instant).seconds) <= 3600) {
            closest.value.coerceIn(0.0, 1.0)
        } else {
            null
        }
    }
}
