package com.weatherapp.libre

import com.weatherapp.libre.lunar.LunarCalculator
import com.weatherapp.libre.lunar.MoonEventType
import com.weatherapp.libre.lunar.Observer as LunarObserver
import com.weatherapp.libre.solar.Observer as SolarObserver
import com.weatherapp.libre.solar.SolarLightingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.min
import kotlin.math.roundToInt

class SolunarManager {

    suspend fun calculateSolunarForecast(
        dates: List<LocalDate>,
        zoneId: ZoneId,
        location: LocationItem
    ): List<SolunarDaySummary> = withContext(Dispatchers.Default) {
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
            calculateDaySolunar(date, zoneId, solarObserver, lunarObserver)
        }
    }

    private fun calculateDaySolunar(
        date: LocalDate,
        zoneId: ZoneId,
        solarObserver: SolarObserver,
        lunarObserver: LunarObserver
    ): SolunarDaySummary {
        val noonInstant = date.atTime(12, 0).atZone(zoneId).toInstant()
        val phaseInfo = LunarCalculator.phaseAt(noonInstant)
        val formattedPhase = LunarCalculator.formatPhaseDisplay(phaseInfo)

        // Moonrise / Moonset for date
        val moonEvents = LunarCalculator.moonEventsForLocalDate(date, zoneId, lunarObserver)
        val moonrise = moonEvents.find { it.type == MoonEventType.MOONRISE }?.instantUtc
        val moonset = moonEvents.find { it.type == MoonEventType.MOONSET }?.instantUtc

        // Lunar Transit & Nadir calculation for date
        val dayStart = date.atStartOfDay(zoneId).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(zoneId).toInstant()

        val (transit, nadir) = findTransitAndNadir(dayStart, dayEnd, lunarObserver)

        val majorPeriods = mutableListOf<SolunarPeriod>()
        transit?.let {
            majorPeriods.add(
                SolunarPeriod(
                    startUtc = it.minusSeconds(3600),
                    endUtc = it.plusSeconds(3600),
                    type = "Major",
                    centerEvent = "Transit"
                )
            )
        }
        nadir?.let {
            majorPeriods.add(
                SolunarPeriod(
                    startUtc = it.minusSeconds(3600),
                    endUtc = it.plusSeconds(3600),
                    type = "Major",
                    centerEvent = "Nadir"
                )
            )
        }

        val minorPeriods = mutableListOf<SolunarPeriod>()
        moonrise?.let {
            minorPeriods.add(
                SolunarPeriod(
                    startUtc = it.minusSeconds(1800),
                    endUtc = it.plusSeconds(1800),
                    type = "Minor",
                    centerEvent = "Moonrise"
                )
            )
        }
        moonset?.let {
            minorPeriods.add(
                SolunarPeriod(
                    startUtc = it.minusSeconds(1800),
                    endUtc = it.plusSeconds(1800),
                    type = "Minor",
                    centerEvent = "Moonset"
                )
            )
        }

        // Solunar Rating Model
        val elongation = phaseInfo.phaseAngleDegrees
        val distFromPrimary = min(elongation % 180.0, 180.0 - (elongation % 180.0))
        val phaseScore = 100.0 - (30.0 * distFromPrimary / 90.0)

        val solarLighting = SolarLightingEngine.calculateSolarLightingPhases(date, zoneId, solarObserver)
        val sunrise = solarLighting.sunrise?.instantUtc
        val sunset = solarLighting.sunset?.instantUtc

        val allPeriods = majorPeriods + minorPeriods
        var bonus = 0

        if (sunrise != null && allPeriods.any { p ->
                !p.startUtc.isAfter(sunrise.plusSeconds(1800)) && !p.endUtc.isBefore(sunrise.minusSeconds(1800))
            }) {
            bonus += 10
        }

        if (sunset != null && allPeriods.any { p ->
                !p.startUtc.isAfter(sunset.plusSeconds(1800)) && !p.endUtc.isBefore(sunset.minusSeconds(1800))
            }) {
            bonus += 10
        }

        val ratingScore = (phaseScore + bonus.coerceAtMost(20)).roundToInt().coerceIn(0, 100)

        val ratingLabel = when {
            ratingScore < 40 -> "Poor"
            ratingScore < 60 -> "Fair"
            ratingScore < 80 -> "Good"
            else -> "Excellent"
        }

        return SolunarDaySummary(
            date = date,
            phaseName = formattedPhase.phaseNameWithEmoji,
            phaseCategory = phaseInfo.phaseCategory,
            illumination = phaseInfo.illuminationFraction,
            moonrise = moonrise,
            moonset = moonset,
            transit = transit,
            nadir = nadir,
            majorPeriods = majorPeriods,
            minorPeriods = minorPeriods,
            rating = ratingScore,
            ratingLabel = ratingLabel
        )
    }

    private fun findTransitAndNadir(
        startInstant: Instant,
        endInstant: Instant,
        lunarObserver: LunarObserver
    ): Pair<Instant?, Instant?> {
        val totalSecs = Duration.between(startInstant, endInstant).seconds
        if (totalSecs <= 0) return Pair(null, null)

        val stepSecs = 300L // 5 minute steps
        var bestTransitSec = 0L
        var maxAlt = -999.0

        var bestNadirSec = 0L
        var minAlt = 999.0

        var curSec = 0L
        while (curSec <= totalSecs) {
            val inst = startInstant.plusSeconds(curSec)
            val state = LunarCalculator.topocentricHorizontalState(inst, lunarObserver)
            val alt = state.altitudeDegrees

            if (alt > maxAlt) {
                maxAlt = alt
                bestTransitSec = curSec
            }
            if (alt < minAlt) {
                minAlt = alt
                bestNadirSec = curSec
            }

            curSec += stepSecs
        }

        // Refine Transit (Golden section search around bestTransitSec +/- 300s)
        val transitInstant = refineExtremum(
            startInstant.plusSeconds((bestTransitSec - 300).coerceAtLeast(0)),
            startInstant.plusSeconds((bestTransitSec + 300).coerceAtMost(totalSecs)),
            lunarObserver,
            isMax = true
        )

        // Refine Nadir (Golden section search around bestNadirSec +/- 300s)
        val nadirInstant = refineExtremum(
            startInstant.plusSeconds((bestNadirSec - 300).coerceAtLeast(0)),
            startInstant.plusSeconds((bestNadirSec + 300).coerceAtMost(totalSecs)),
            lunarObserver,
            isMax = false
        )

        return Pair(transitInstant, nadirInstant)
    }

    private fun refineExtremum(
        t1: Instant,
        t2: Instant,
        lunarObserver: LunarObserver,
        isMax: Boolean
    ): Instant {
        var a = t1.epochSecond.toDouble()
        var b = t2.epochSecond.toDouble()
        val phi = (1.0 + Math.sqrt(5.0)) / 2.0
        val resphi = 2.0 - phi

        var c = a + resphi * (b - a)
        var d = b - resphi * (b - a)

        fun getAlt(sec: Double): Double {
            val inst = Instant.ofEpochSecond(sec.toLong())
            return LunarCalculator.topocentricHorizontalState(inst, lunarObserver).altitudeDegrees
        }

        var fc = getAlt(c)
        var fd = getAlt(d)

        for (iter in 0..20) {
            if (if (isMax) fc > fd else fc < fd) {
                b = d
                d = c
                fd = fc
                c = a + resphi * (b - a)
                fc = getAlt(c)
            } else {
                a = c
                c = d
                fc = fd
                d = b - resphi * (b - a)
                fd = getAlt(d)
            }
            if (Math.abs(b - a) < 1.0) break
        }

        val bestSec = ((a + b) / 2.0).toLong()
        return Instant.ofEpochSecond(bestSec)
    }
}
