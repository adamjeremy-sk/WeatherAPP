package com.weatherapp.libre.almanac

import com.weatherapp.libre.LocationItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

object LunarCalendarManager {

    /**
     * Aggregates a 7-day week of Moon Almanac data starting from startDate.
     */
    fun getWeeklyForecast(
        startDate: LocalDate,
        location: LocationItem,
        hourlyCloudCoverMap: Map<Instant, Double>? = null,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<MoonAlmanacDay> {
        val rawDays = MoonAlmanacManager.getMoonAlmanac(startDate, 7, location, zoneId)

        return rawDays.map { day ->
            val scores = calculateViewingScores(day, hourlyCloudCoverMap, zoneId)
            day.copy(
                deepSkyScore = scores?.first,
                lunarViewingScore = scores?.second
            )
        }
    }

    /**
     * Aggregates a full month of Moon Almanac data for the specified year, month, and location.
     */
    fun getMonthlyForecast(
        year: Int,
        month: Int,
        location: LocationItem,
        hourlyCloudCoverMap: Map<Instant, Double>? = null,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<MoonAlmanacDay> {
        val startDate = LocalDate.of(year, month, 1)
        val daysInMonth = startDate.lengthOfMonth()

        val rawDays = MoonAlmanacManager.getMoonAlmanac(startDate, daysInMonth, location, zoneId)

        return rawDays.map { day ->
            val scores = calculateViewingScores(day, hourlyCloudCoverMap, zoneId)
            day.copy(
                deepSkyScore = scores?.first,
                lunarViewingScore = scores?.second
            )
        }
    }

    /**
     * Calculates Astrophotography / Viewing Scores (0-100) based on
     * sky clarity (cloud cover) and illumination.
     *
     * Returns Pair(deepSkyScore, lunarViewingScore).
     * Hides / returns null for dates beyond a 7-day forecast horizon.
     */
    private fun calculateViewingScores(
        day: MoonAlmanacDay,
        hourlyCloudCoverMap: Map<Instant, Double>?,
        zoneId: ZoneId,
    ): Pair<Double, Double>? {
        val today = LocalDate.now(zoneId)
        val maxForecastDate = today.plusDays(7)
        if (day.date.isAfter(maxForecastDate)) {
            return null
        }

        val nightStart = day.date.atTime(21, 0).atZone(zoneId).toInstant()
        val nightEnd = day.date.plusDays(1).atTime(4, 0).atZone(zoneId).toInstant()

        val cloudCoverFraction = getAverageCloudCover(nightStart, nightEnd, hourlyCloudCoverMap)
        val illumination = day.primaryPhase.illuminationFraction

        val skyClarityScore = if (cloudCoverFraction != null) {
            (1.0 - cloudCoverFraction) * 100.0
        } else {
            80.0 // Default good sky clarity if forecast map is unavailable
        }

        val darknessScore = (1.0 - 0.5 * illumination) * 100.0

        val deepSkyScore = (skyClarityScore * 0.7 + darknessScore * 0.3).roundToInt().toDouble().coerceIn(0.0, 100.0)
        val lunarViewingScore = (skyClarityScore * 0.5 + (illumination * 100.0) * 0.5).roundToInt().toDouble().coerceIn(0.0, 100.0)

        return Pair(deepSkyScore, lunarViewingScore)
    }

    private fun getAverageCloudCover(
        startUtc: Instant,
        endUtc: Instant,
        hourlyCloudCoverMap: Map<Instant, Double>?,
    ): Double? {
        if (hourlyCloudCoverMap.isNullOrEmpty()) return null

        val samples = hourlyCloudCoverMap.filter { (time, _) ->
            !time.isBefore(startUtc) && time.isBefore(endUtc)
        }.values

        return if (samples.isNotEmpty()) {
            samples.average().coerceIn(0.0, 1.0)
        } else {
            null
        }
    }
}
