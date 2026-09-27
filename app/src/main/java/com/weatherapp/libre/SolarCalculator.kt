package com.weatherapp.libre

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

data class SolarTimesResult(
    val sunrise: LocalTime,
    val sunset: LocalTime,
    val daylightDurationMinutes: Long,
    val daylightDurationText: String,
    val sunriseAzimuthDegrees: Int,
    val sunsetAzimuthDegrees: Int,
    val sunriseCompassDirection: String,
    val sunsetCompassDirection: String,
    val morningGoldenHourStart: LocalTime,
    val morningGoldenHourEnd: LocalTime,
    val eveningGoldenHourStart: LocalTime,
    val eveningGoldenHourEnd: LocalTime,
    val morningBlueHourStart: LocalTime,
    val morningBlueHourEnd: LocalTime,
    val eveningBlueHourStart: LocalTime,
    val eveningBlueHourEnd: LocalTime,
    val moonriseAzimuthDegrees: Int,
    val moonsetAzimuthDegrees: Int,
    val moonriseCompassDirection: String,
    val moonsetCompassDirection: String,
    val moonPhase: MoonPhaseInfo,
    val moonCyclePercentage: Int,
    val solarNoonElevationDegrees: Int
)

data class MoonPhaseInfo(
    val name: String,
    val emoji: String
)

data class MoonPhaseInfoAndPercentage(
    val name: String,
    val emoji: String,
    val percentage: Int
)

data class SolarPhaseDetail(
    val stepIndex: Int,
    val name: String,
    val iconEmoji: String,
    val timeRangeText: String,
    val durationText: String,
    val elevationDescription: String
)

object SolarCalculator {

    fun calculateSolarTimes(
        lat: Double,
        lon: Double,
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): SolarTimesResult {
        val dayOfYear = date.dayOfYear
        val gamma = (2.0 * Math.PI / 365.0) * (dayOfYear - 1)

        // Equation of Time in minutes
        val eqTime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) - 0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))

        // Solar Declination in radians
        val declinationRad = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) - 0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) - 0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
        val declinationDeg = Math.toDegrees(declinationRad)

        val latRad = Math.toRadians(lat)

        // Timezone offset in minutes for the location's specific ZoneId
        val tzOffsetMinutes = (zoneId.rules.getOffset(date.atStartOfDay()).totalSeconds / 60)

        // Solar Noon in minutes from midnight local time
        val solarNoonMinutes = 720.0 - (4.0 * lon) - eqTime + tzOffsetMinutes

        // Helper to convert minutes from midnight to LocalTime
        fun minutesToLocalTime(mins: Double): LocalTime {
            val totalSeconds = (mins * 60).toLong().coerceIn(0, 86399)
            return LocalTime.ofSecondOfDay(totalSeconds)
        }

        // Calculates Hour Angle (in degrees) for a target solar elevation angle (alpha)
        fun calculateHourAngle(alphaDeg: Double): Double? {
            val alphaRad = Math.toRadians(alphaDeg)
            val cosH = (sin(alphaRad) - sin(latRad) * sin(declinationRad)) / (cos(latRad) * cos(declinationRad))
            return when {
                cosH <= -1.0 -> 180.0 // Sun remains above elevation alpha all 24h
                cosH >= 1.0 -> 0.0   // Sun remains below elevation alpha all 24h
                else -> Math.toDegrees(acos(cosH))
            }
        }

        // Sunrise / Sunset elevation: -0.8333 degrees (accounting for atmospheric refraction)
        val haSunrise = calculateHourAngle(-0.8333)

        val (sunrise, sunset, durationMinutes, durationText) = when {
            haSunrise == 180.0 -> {
                // 24-hour Midnight Sun
                Tuple4(LocalTime.MIN, LocalTime.MAX, 1440L, "24h 00m (Midnight Sun)")
            }
            haSunrise == 0.0 -> {
                // 24-hour Polar Night
                Tuple4(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT, 0L, "0h 00m (Polar Night)")
            }
            else -> {
                val offsetMins = (haSunrise ?: 90.0) * 4.0
                val sr = minutesToLocalTime(solarNoonMinutes - offsetMins)
                val ss = minutesToLocalTime(solarNoonMinutes + offsetMins)
                val dur = (offsetMins * 2).toLong().coerceIn(0, 1440)
                val h = dur / 60
                val m = dur % 60
                Tuple4(sr, ss, dur, "${h}h ${String.format(Locale.getDefault(), "%02dm", m)}")
            }
        }

        // Solar Azimuth Angle at Sunrise and Sunset
        val cosAz = (sin(declinationRad) - sin(latRad) * sin(Math.toRadians(-0.8333))) / (cos(latRad) * cos(Math.toRadians(-0.8333)))
        val sunriseAzimuth = if (cosAz in -1.0..1.0) Math.toDegrees(acos(cosAz)).toInt() else 90
        val sunsetAzimuth = (360 - sunriseAzimuth) % 360

        val sunriseDir = UnitUtils.getDegreesToCompass(sunriseAzimuth.toDouble())
        val sunsetDir = UnitUtils.getDegreesToCompass(sunsetAzimuth.toDouble())

        // Golden Hour: Sun elevation between -4.0° and +6.0°
        val haGoldLow = calculateHourAngle(-4.0) ?: 90.0
        val haGoldHigh = calculateHourAngle(6.0) ?: 0.0

        val morningGoldenStart = minutesToLocalTime(solarNoonMinutes - (haGoldLow * 4.0))
        val morningGoldenEnd = minutesToLocalTime(solarNoonMinutes - (haGoldHigh * 4.0))

        val eveningGoldenStart = minutesToLocalTime(solarNoonMinutes + (haGoldHigh * 4.0))
        val eveningGoldenEnd = minutesToLocalTime(solarNoonMinutes + (haGoldLow * 4.0))

        // Blue Hour: Sun elevation between -6.0° and -4.0°
        val haBlueLow = calculateHourAngle(-6.0) ?: 90.0
        val haBlueHigh = calculateHourAngle(-4.0) ?: 90.0

        val morningBlueStart = minutesToLocalTime(solarNoonMinutes - (haBlueLow * 4.0))
        val morningBlueEnd = minutesToLocalTime(solarNoonMinutes - (haBlueHigh * 4.0))

        val eveningBlueStart = minutesToLocalTime(solarNoonMinutes + (haBlueHigh * 4.0))
        val eveningBlueEnd = minutesToLocalTime(solarNoonMinutes + (haBlueLow * 4.0))

        // Moon Calculations
        val epochDay = date.toEpochDay()
        val moonAgeDays = ((epochDay - 18000) % 29.530588 + 29.530588) % 29.530588
        val cyclePct = ((moonAgeDays / 29.530588) * 100).toInt().coerceIn(0, 100)

        val moonPhase = getMoonPhaseInfo(moonAgeDays)

        val moonriseAzimuth = (60 + (moonAgeDays * 8) % 120).toInt()
        val moonsetAzimuth = (240 + (moonAgeDays * 8) % 120).toInt()

        val solarNoonElevation = (90.0 - lat + declinationDeg).toInt().coerceIn(-90, 90)

        return SolarTimesResult(
            sunrise = sunrise,
            sunset = sunset,
            daylightDurationMinutes = durationMinutes,
            daylightDurationText = durationText,
            sunriseAzimuthDegrees = sunriseAzimuth,
            sunsetAzimuthDegrees = sunsetAzimuth,
            sunriseCompassDirection = sunriseDir,
            sunsetCompassDirection = sunsetDir,
            morningGoldenHourStart = morningGoldenStart,
            morningGoldenHourEnd = morningGoldenEnd,
            eveningGoldenHourStart = eveningGoldenStart,
            eveningGoldenHourEnd = eveningGoldenEnd,
            morningBlueHourStart = morningBlueStart,
            morningBlueHourEnd = morningBlueEnd,
            eveningBlueHourStart = eveningBlueStart,
            eveningBlueHourEnd = eveningBlueEnd,
            moonriseAzimuthDegrees = moonriseAzimuth,
            moonsetAzimuthDegrees = moonsetAzimuth,
            moonriseCompassDirection = UnitUtils.getDegreesToCompass(moonriseAzimuth.toDouble()),
            moonsetCompassDirection = UnitUtils.getDegreesToCompass(moonsetAzimuth.toDouble()),
            moonPhase = moonPhase,
            moonCyclePercentage = cyclePct,
            solarNoonElevationDegrees = solarNoonElevation
        )
    }

    private data class Tuple4<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    /**
     * Calculates the 15 sequential daily solar transitions including Sunrise and Sunset (AM to PM)
     */
    fun calculate13ChronologicalPhases(
        lat: Double,
        lon: Double,
        date: LocalDate,
        is24Hour: Boolean = false,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<SolarPhaseDetail> {
        val solar = calculateSolarTimes(lat, lon, date, zoneId)
        val sr = solar.sunrise
        val ss = solar.sunset

        val dayOfYear = date.dayOfYear
        val gamma = (2.0 * Math.PI / 365.0) * (dayOfYear - 1)
        val eqTime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) - 0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))
        val declinationRad = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) - 0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) - 0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
        val latRad = Math.toRadians(lat)
        val tzOffsetMinutes = (zoneId.rules.getOffset(date.atStartOfDay()).totalSeconds / 60)
        val solarNoonMinutes = 720.0 - (4.0 * lon) - eqTime + tzOffsetMinutes

        fun getElevationTime(alphaDeg: Double, isMorning: Boolean): LocalTime {
            val alphaRad = Math.toRadians(alphaDeg)
            val cosH = (sin(alphaRad) - sin(latRad) * sin(declinationRad)) / (cos(latRad) * cos(declinationRad))
            val ha = when {
                cosH <= -1.0 -> 180.0
                cosH >= 1.0 -> 0.0
                else -> Math.toDegrees(acos(cosH))
            }
            val offsetMins = ha * 4.0
            val targetMins = if (isMorning) solarNoonMinutes - offsetMins else solarNoonMinutes + offsetMins
            val totalSecs = (targetMins * 60).toLong().coerceIn(0, 86399)
            return LocalTime.ofSecondOfDay(totalSecs)
        }

        val t0 = LocalTime.of(0, 0)
        val tAstroMStart = getElevationTime(-18.0, true)
        val tNautMStart = getElevationTime(-12.0, true)
        val tCivilMStart = getElevationTime(-6.0, true)
        val tBlueMStart = getElevationTime(-6.0, true)
        val tBlueMEnd = getElevationTime(-4.0, true)
        val tGoldMStart = getElevationTime(-4.0, true)
        val tGoldMEnd = getElevationTime(6.0, true)

        val tGoldEStart = getElevationTime(6.0, false)
        val tGoldEEnd = getElevationTime(-4.0, false)
        val tCivilEEnd = getElevationTime(-6.0, false)
        val tBlueEStart = getElevationTime(-4.0, false)
        val tBlueEEnd = getElevationTime(-6.0, false)
        val tNautEEnd = getElevationTime(-12.0, false)
        val tAstroEEnd = getElevationTime(-18.0, false)
        val tEnd = LocalTime.of(23, 59)

        fun fmt(t: LocalTime) = formatTime(t, is24Hour)
        fun dur(st: LocalTime, en: LocalTime): String {
            val mins = ChronoUnit.MINUTES.between(st, en)
            return if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m"
        }

        return listOf(
            SolarPhaseDetail(
                1, "Night", "",
                "${fmt(t0)} – ${fmt(tAstroMStart)}",
                dur(t0, tAstroMStart),
                "Sun elevation < -18° • Complete darkness"
            ),
            SolarPhaseDetail(
                2, "Astronomical Twilight", "",
                "${fmt(tAstroMStart)} – ${fmt(tNautMStart)}",
                dur(tAstroMStart, tNautMStart),
                "Sun elevation -18° to -12° • Sky begins illuminating"
            ),
            SolarPhaseDetail(
                3, "Nautical Twilight", "",
                "${fmt(tNautMStart)} – ${fmt(tCivilMStart)}",
                dur(tNautMStart, tCivilMStart),
                "Sun elevation -12° to -6° • Horizon line becomes visible"
            ),
            SolarPhaseDetail(
                4, "Blue Hour", "",
                "${fmt(tBlueMStart)} – ${fmt(tBlueMEnd)}",
                dur(tBlueMStart, tBlueMEnd),
                "Sun elevation -6° to -4° • Deep cobalt blue sky glow"
            ),
            SolarPhaseDetail(
                5, "Civil Twilight", "",
                "${fmt(tCivilMStart)} – ${fmt(sr)}",
                dur(tCivilMStart, sr),
                "Sun elevation -6° to 0° • Daylight without direct sun"
            ),
            SolarPhaseDetail(
                6, "Sunrise", "",
                fmt(sr),
                "Event",
                "Sun elevation 0° • Rising at ${solar.sunriseAzimuthDegrees}° (${solar.sunriseCompassDirection})"
            ),
            SolarPhaseDetail(
                7, "Golden Hour", "",
                "${fmt(tGoldMStart)} – ${fmt(tGoldMEnd)}",
                dur(tGoldMStart, tGoldMEnd),
                "Sun elevation -4° to +6° • Warm golden sunlight window"
            ),
            SolarPhaseDetail(
                8, "Day", "",
                "${fmt(tGoldMEnd)} – ${fmt(tGoldEStart)}",
                dur(tGoldMEnd, tGoldEStart),
                "Sun elevation > +6° • Full daytime solar intensity"
            ),
            SolarPhaseDetail(
                9, "Golden Hour", "",
                "${fmt(tGoldEStart)} – ${fmt(tGoldEEnd)}",
                dur(tGoldEStart, tGoldEEnd),
                "Sun elevation +6° to -4° • Soft warm evening photography window"
            ),
            SolarPhaseDetail(
                10, "Sunset", "",
                fmt(ss),
                "Event",
                "Sun elevation 0° • Setting at ${solar.sunsetAzimuthDegrees}° (${solar.sunsetCompassDirection})"
            ),
            SolarPhaseDetail(
                11, "Civil Twilight", "",
                "${fmt(ss)} – ${fmt(tCivilEEnd)}",
                dur(ss, tCivilEEnd),
                "Sun elevation 0° to -6° • Post-sunset twilight illumination"
            ),
            SolarPhaseDetail(
                12, "Blue Hour", "",
                "${fmt(tBlueEStart)} – ${fmt(tBlueEEnd)}",
                dur(tBlueEStart, tBlueEEnd),
                "Sun elevation -4° to -6° • Rich indigo blue twilight"
            ),
            SolarPhaseDetail(
                13, "Nautical Twilight", "",
                "${fmt(tCivilEEnd)} – ${fmt(tNautEEnd)}",
                dur(tCivilEEnd, tNautEEnd),
                "Sun elevation -6° to -12° • First bright navigation stars appear"
            ),
            SolarPhaseDetail(
                14, "Astronomical Twilight", "",
                "${fmt(tNautEEnd)} – ${fmt(tAstroEEnd)}",
                dur(tNautEEnd, tAstroEEnd),
                "Sun elevation -12° to -18° • Faint solar atmospheric glow"
            ),
            SolarPhaseDetail(
                15, "Night", "",
                "${fmt(tAstroEEnd)} – ${fmt(tEnd)}",
                dur(tAstroEEnd, tEnd),
                "Sun elevation < -18° • Complete night sky darkness"
            )
        )
    }

    private fun getMoonPhaseInfo(ageDays: Double): MoonPhaseInfo {
        return when {
            ageDays < 1.8456 -> MoonPhaseInfo("New Moon", "🌑")
            ageDays < 5.5369 -> MoonPhaseInfo("Waxing Crescent", "🌒")
            ageDays < 9.2283 -> MoonPhaseInfo("First Quarter", "🌓")
            ageDays < 12.9196 -> MoonPhaseInfo("Waxing Gibbous", "🌔")
            ageDays < 16.6110 -> MoonPhaseInfo("Full Moon", "🌕")
            ageDays < 20.3023 -> MoonPhaseInfo("Waning Gibbous", "🌖")
            ageDays < 23.9936 -> MoonPhaseInfo("Third Quarter", "🌗")
            ageDays < 27.6849 -> MoonPhaseInfo("Waning Crescent", "🌘")
            else -> MoonPhaseInfo("New Moon", "🌑")
        }
    }

    fun formatTime(time: LocalTime, is24Hour: Boolean = false): String {
        val pattern = if (is24Hour) "HH:mm" else "h:mm a"
        return time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }
}
