package com.weatherapp.libre.almanac

import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.lunar.EclipseCalculationResult
import com.weatherapp.libre.lunar.EclipseType
import com.weatherapp.libre.lunar.LunarCalculator
import com.weatherapp.libre.lunar.LunarEclipseCalculator
import com.weatherapp.libre.lunar.LunarEclipseInfo
import com.weatherapp.libre.lunar.MoonEventType
import com.weatherapp.libre.lunar.MoonPhaseCalculator
import com.weatherapp.libre.lunar.MoonPhaseInfo
import com.weatherapp.libre.lunar.MoonPhaseName
import com.weatherapp.libre.lunar.Observer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

data class MoonAlmanacDay(
    val date: LocalDate,
    val primaryPhase: MoonPhaseInfo,
    val distanceKm: Double,
    val eclipseInfo: LunarEclipseInfo?,
    val moonrise: Instant?,
    val moonset: Instant?,
    val cloudCoverScore: Double? = null,
    val isSupermoon: Boolean = false,
    val isBloodMoon: Boolean = false,
    val isBlueMoon: Boolean = false,
    val isHarvestMoon: Boolean = false,
    val isMicromoon: Boolean = false,
    val isLunarMoonbow: Boolean = false,
    val isLunarHalo: Boolean = false,
    val isLunarCorona: Boolean = false,
    val isLunarParselene: Boolean = false,
    val isLunarGlory: Boolean = false,
    val moonbowProbability: Double = 0.0,
    val haloProbability: Double = 0.0,
    val coronaProbability: Double = 0.0,
    val parseleneProbability: Double = 0.0,
    val moonAgeDays: Double = 0.0,
    val transitAltitudeDeg: Double = 0.0,
    val transitAzimuthDeg: Double = 0.0,
    val nextMajorPhaseName: String = "Full Moon",
    val daysUntilNextPhase: Int = 0,
    val moonriseAzimuthDeg: Double? = null,
    val moonsetAzimuthDeg: Double? = null,
    val exactPhasePeakTime: Instant? = null,
)

object MoonAlmanacManager {

    fun getMoonAlmanac(
        startDate: LocalDate,
        daysForward: Int,
        location: LocationItem,
        zoneId: ZoneId = ZoneId.systemDefault(),
        hourlyCloudCoverMap: Map<Instant, Double>? = null,
    ): List<MoonAlmanacDay> {
        val result = mutableListOf<MoonAlmanacDay>()
        val observer = Observer(location.latitude, location.longitude, location.elevationMeters)

        for (i in 0 until daysForward) {
            val date = startDate.plusDays(i.toLong())
            val localNoonInstant = date.atTime(12, 0).atZone(zoneId).toInstant()

            val dayStartUtc = date.atStartOfDay(zoneId).toInstant()
            val dayEndUtc = date.plusDays(1).atStartOfDay(zoneId).toInstant()

            val phaseInfo = MoonPhaseCalculator.phaseAt(localNoonInstant)
            val distanceKm = phaseInfo.geocentricDistanceKm

            val phaseEvents = MoonPhaseCalculator.phaseEventsBetween(dayStartUtc, dayEndUtc)
            val dayPhaseEvent = phaseEvents.firstOrNull()
            val exactPhasePeakTime = dayPhaseEvent?.instantUtc

            val fullMoonEvent = phaseEvents.firstOrNull { it.type == MoonPhaseName.FULL_MOON }

            var eclipseInfo: LunarEclipseInfo? = null
            var isSupermoon = false
            var isBloodMoon = false
            var isBlueMoon = false
            var isHarvestMoon = false
            var isMicromoon = false

            if (fullMoonEvent != null) {
                eclipseInfo = when (val res = LunarEclipseCalculator.calculateLunarEclipse(fullMoonEvent.instantUtc)) {
                    is EclipseCalculationResult.Success -> res.eclipse
                    is EclipseCalculationResult.NoEclipse -> null
                    is EclipseCalculationResult.Failure -> null
                }
                val fullMoonPhaseInfo = MoonPhaseCalculator.phaseAt(fullMoonEvent.instantUtc)
                isSupermoon = fullMoonPhaseInfo.geocentricDistanceKm <= 360000.0
                isMicromoon = fullMoonPhaseInfo.geocentricDistanceKm >= 404000.0
                if (eclipseInfo?.type == EclipseType.TOTAL) {
                    isBloodMoon = true
                }

                // Blue Moon check (2nd Full Moon in calendar month)
                val monthStartUtc = date.withDayOfMonth(1).atStartOfDay(zoneId).toInstant()
                val monthEvents = MoonPhaseCalculator.phaseEventsBetween(monthStartUtc, fullMoonEvent.instantUtc)
                val fullMoonCount = monthEvents.count { it.type == MoonPhaseName.FULL_MOON }
                if (fullMoonCount > 1) {
                    isBlueMoon = true
                }

                // Harvest Moon check (Full Moon nearest Autumnal Equinox)
                val equinoxDate = LocalDate.of(date.year, 9, 22)
                val distToEquinox = abs(ChronoUnit.DAYS.between(date, equinoxDate))
                if (date.monthValue in 9..10 && distToEquinox <= 15) {
                    isHarvestMoon = true
                }
            }

            // Atmospheric event probabilities
            val illumination = phaseInfo.illuminationFraction
            val avgCloud = getAverageCloudCover(dayStartUtc, dayEndUtc, hourlyCloudCoverMap) ?: 0.3
            val moonbowProb = if (illumination > 0.85 && avgCloud in 0.1..0.4) 0.85 else 0.0
            val haloProb = if (illumination > 0.50 && avgCloud in 0.2..0.8) 0.70 else 0.0
            val coronaProb = if (illumination > 0.50 && avgCloud in 0.1..0.6) 0.65 else 0.0
            val parseleneProb = if (illumination > 0.75 && avgCloud in 0.15..0.5) 0.60 else 0.0

            val moonEvents = LunarCalculator.moonEventsForLocalDate(date, zoneId, observer)
            val moonriseEvent = moonEvents.firstOrNull { it.type == MoonEventType.MOONRISE }
            val moonsetEvent = moonEvents.firstOrNull { it.type == MoonEventType.MOONSET }
            val moonrise = moonriseEvent?.instantUtc
            val moonset = moonsetEvent?.instantUtc
            val moonriseAz = moonriseEvent?.azimuthDegrees
            val moonsetAz = moonsetEvent?.azimuthDegrees

            // Transit peak altitude & azimuth calculation
            var maxAlt = -90.0
            var maxAz = 0.0
            var sampleInst = dayStartUtc
            while (sampleInst.isBefore(dayEndUtc)) {
                val topo = LunarCalculator.topocentricHorizontalState(sampleInst, observer)
                if (topo.altitudeDegrees > maxAlt) {
                    maxAlt = topo.altitudeDegrees
                    maxAz = topo.azimuthDegrees
                }
                sampleInst = sampleInst.plusSeconds(900) // 15 mins
            }

            // Next major phase event calculation
            val futureEvents = MoonPhaseCalculator.phaseEventsBetween(dayStartUtc, dayStartUtc.plusSeconds(35 * 86400L))
            val nextEvent = futureEvents.firstOrNull { it.instantUtc.isAfter(dayStartUtc) }
            val nextPhaseName = when (nextEvent?.type) {
                MoonPhaseName.NEW_MOON -> "New Moon"
                MoonPhaseName.FIRST_QUARTER -> "First Quarter"
                MoonPhaseName.FULL_MOON -> "Full Moon"
                MoonPhaseName.LAST_QUARTER -> "Last Quarter"
                null -> "New Moon"
            }
            val daysUntil = if (nextEvent != null) {
                val nextDate = nextEvent.instantUtc.atZone(zoneId).toLocalDate()
                ChronoUnit.DAYS.between(date, nextDate).toInt().coerceAtLeast(0)
            } else 0

            result.add(
                MoonAlmanacDay(
                    date = date,
                    primaryPhase = phaseInfo,
                    distanceKm = distanceKm,
                    eclipseInfo = eclipseInfo,
                    moonrise = moonrise,
                    moonset = moonset,
                    cloudCoverScore = null,
                    isSupermoon = isSupermoon,
                    isBloodMoon = isBloodMoon,
                    isBlueMoon = isBlueMoon,
                    isHarvestMoon = isHarvestMoon,
                    isMicromoon = isMicromoon,
                    isLunarMoonbow = moonbowProb > 0.7,
                    isLunarHalo = haloProb > 0.6,
                    isLunarCorona = coronaProb > 0.6,
                    isLunarParselene = parseleneProb > 0.5,
                    isLunarGlory = false,
                    moonbowProbability = moonbowProb,
                    haloProbability = haloProb,
                    coronaProbability = coronaProb,
                    parseleneProbability = parseleneProb,
                    moonAgeDays = phaseInfo.ageDays,
                    transitAltitudeDeg = maxAlt,
                    transitAzimuthDeg = maxAz,
                    nextMajorPhaseName = nextPhaseName,
                    daysUntilNextPhase = daysUntil,
                    moonriseAzimuthDeg = moonriseAz,
                    moonsetAzimuthDeg = moonsetAz,
                    exactPhasePeakTime = exactPhasePeakTime,
                ),
            )
        }

        return result
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
