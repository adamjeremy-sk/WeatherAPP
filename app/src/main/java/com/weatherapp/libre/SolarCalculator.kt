package com.weatherapp.libre

import com.weatherapp.libre.solar.Observer
import com.weatherapp.libre.solar.SolarLightingEngine
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

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
    val moonriseAzimuthDegrees: Int = 0,
    val moonsetAzimuthDegrees: Int = 0,
    val moonriseCompassDirection: String = "--",
    val moonsetCompassDirection: String = "--",
    val moonPhase: MoonPhaseInfo = MoonPhaseInfo("--", ""),
    val moonCyclePercentage: Int = 0,
    val solarNoonElevationDegrees: Int = 45
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
        val observer = Observer(lat, lon)
        val lighting = SolarLightingEngine.calculateSolarLightingPhases(date, zoneId, observer)

        val srTime = lighting.sunrise?.instantUtc?.atZone(zoneId)?.toLocalTime() ?: LocalTime.of(6, 0)
        val ssTime = lighting.sunset?.instantUtc?.atZone(zoneId)?.toLocalTime() ?: LocalTime.of(18, 0)

        val durationMinutes = ChronoUnit.MINUTES.between(srTime, ssTime).coerceAtLeast(0L)
        val h = durationMinutes / 60
        val m = durationMinutes % 60
        val durationText = "${h}h ${String.format(Locale.getDefault(), "%02dm", m)}"

        val srAz = lighting.sunrise?.azimuthDegrees?.toInt() ?: 90
        val ssAz = lighting.sunset?.azimuthDegrees?.toInt() ?: 270

        val mgStart = lighting.morningGoldenHour?.startInstantUtc?.atZone(zoneId)?.toLocalTime() ?: srTime.minusMinutes(30)
        val mgEnd = lighting.morningGoldenHour?.endInstantUtc?.atZone(zoneId)?.toLocalTime() ?: srTime.plusMinutes(30)

        val egStart = lighting.eveningGoldenHour?.startInstantUtc?.atZone(zoneId)?.toLocalTime() ?: ssTime.minusMinutes(30)
        val egEnd = lighting.eveningGoldenHour?.endInstantUtc?.atZone(zoneId)?.toLocalTime() ?: ssTime.plusMinutes(30)

        val mbStart = lighting.morningBlueHour?.startInstantUtc?.atZone(zoneId)?.toLocalTime() ?: srTime.minusMinutes(45)
        val mbEnd = lighting.morningBlueHour?.endInstantUtc?.atZone(zoneId)?.toLocalTime() ?: srTime.minusMinutes(15)

        val ebStart = lighting.eveningBlueHour?.startInstantUtc?.atZone(zoneId)?.toLocalTime() ?: ssTime.plusMinutes(15)
        val ebEnd = lighting.eveningBlueHour?.endInstantUtc?.atZone(zoneId)?.toLocalTime() ?: ssTime.plusMinutes(45)

        val noonElevation = lighting.solarNoon?.apparentAltitudeDegrees?.toInt() ?: 45

        return SolarTimesResult(
            sunrise = srTime,
            sunset = ssTime,
            daylightDurationMinutes = durationMinutes,
            daylightDurationText = durationText,
            sunriseAzimuthDegrees = srAz,
            sunsetAzimuthDegrees = ssAz,
            sunriseCompassDirection = UnitUtils.getDegreesToCompass(srAz.toDouble()),
            sunsetCompassDirection = UnitUtils.getDegreesToCompass(ssAz.toDouble()),
            morningGoldenHourStart = mgStart,
            morningGoldenHourEnd = mgEnd,
            eveningGoldenHourStart = egStart,
            eveningGoldenHourEnd = egEnd,
            morningBlueHourStart = mbStart,
            morningBlueHourEnd = mbEnd,
            eveningBlueHourStart = ebStart,
            eveningBlueHourEnd = ebEnd,
            solarNoonElevationDegrees = noonElevation
        )
    }

    /**
     * Calculates the 15 sequential daily solar transitions including Sunrise and Sunset (AM to PM)
     */
    fun calculate13ChronologicalPhases(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        is24Hour: Boolean = false,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<SolarPhaseDetail> {
        val observer = Observer(latitude, longitude)
        val lighting = SolarLightingEngine.calculateSolarLightingPhases(date, zoneId, observer)

        fun fmt(instant: java.time.Instant?): String {
            if (instant == null) return "--"
            val lt = instant.atZone(zoneId).toLocalTime()
            return formatTime(lt, is24Hour)
        }

        fun dur(instantStart: java.time.Instant?, instantEnd: java.time.Instant?): String {
            if (instantStart == null || instantEnd == null) return "--"
            val mins = ChronoUnit.MINUTES.between(instantStart, instantEnd)
            val positiveMins = if (mins < 0) mins + 1440 else mins
            val h = positiveMins / 60
            val m = positiveMins % 60
            return if (h > 0) "${h}h ${m}m" else "${m}m"
        }

        val srInstant = lighting.sunrise?.instantUtc
        val ssInstant = lighting.sunset?.instantUtc

        val mgStart = lighting.morningGoldenHour?.startInstantUtc
        val mgEnd = lighting.morningGoldenHour?.endInstantUtc

        val egStart = lighting.eveningGoldenHour?.startInstantUtc
        val egEnd = lighting.eveningGoldenHour?.endInstantUtc

        val mbStart = lighting.morningBlueHour?.startInstantUtc
        val mbEnd = lighting.morningBlueHour?.endInstantUtc

        val ebStart = lighting.eveningBlueHour?.startInstantUtc
        val ebEnd = lighting.eveningBlueHour?.endInstantUtc

        val civilStart = lighting.civilTwilight?.startInstantUtc
        val civilEnd = lighting.civilTwilight?.endInstantUtc

        val nautStart = lighting.nauticalTwilight?.startInstantUtc
        val nautEnd = lighting.nauticalTwilight?.endInstantUtc

        val astroStart = lighting.astronomicalTwilight?.startInstantUtc
        val astroEnd = lighting.astronomicalTwilight?.endInstantUtc

        return listOf(
            SolarPhaseDetail(1, "Night", "🌌", "Midnight – ${fmt(astroStart)}", dur(date.atStartOfDay(zoneId).toInstant(), astroStart), "Sun elevation < -18° • Complete astronomical darkness"),
            SolarPhaseDetail(2, "Astronomical Twilight", "🌌", "${fmt(astroStart)} – ${fmt(nautStart)}", dur(astroStart, nautStart), "Sun elevation -18° to -12° • Sky begins subtle morning glow"),
            SolarPhaseDetail(3, "Nautical Twilight", "⛵", "${fmt(nautStart)} – ${fmt(civilStart)}", dur(nautStart, civilStart), "Sun elevation -12° to -6° • Horizon clearly visible at sea"),
            SolarPhaseDetail(4, "Blue Hour", "🟦", "${fmt(mbStart)} – ${fmt(mbEnd)}", dur(mbStart, mbEnd), "Sun elevation -6° to -4° • Deep electric blue sky for photography"),
            SolarPhaseDetail(5, "Civil Twilight", "🌅", "${fmt(civilStart)} – ${fmt(srInstant)}", dur(civilStart, srInstant), "Sun elevation -6° to -0.83° • Natural outdoor light, no lamps needed"),
            SolarPhaseDetail(6, "Sunrise", "☀️", fmt(srInstant), "Event", "Sun upper limb crosses eastern horizon (elevation -0.83°)"),
            SolarPhaseDetail(7, "Golden Hour", "🟧", "${fmt(mgStart)} – ${fmt(mgEnd)}", dur(mgStart, mgEnd), "Sun elevation -4° to +6° • Warm golden hues, soft long shadows"),
            SolarPhaseDetail(8, "Day", "☀️", "${fmt(mgEnd)} – ${fmt(egStart)}", dur(mgEnd, egStart), "Sun elevation > +6° • Full daylight spectrum"),
            SolarPhaseDetail(9, "Golden Hour", "🟧", "${fmt(egStart)} – ${fmt(egEnd)}", dur(egStart, egEnd), "Sun elevation +6° to -4° • Warm magical evening photography light"),
            SolarPhaseDetail(10, "Sunset", "🌇", fmt(ssInstant), "Event", "Sun upper limb disappears below western horizon (elevation -0.83°)"),
            SolarPhaseDetail(11, "Civil Twilight", "🌆", "${fmt(ssInstant)} – ${fmt(civilEnd)}", dur(ssInstant, civilEnd), "Sun elevation -0.83° to -6° • Dusk light, city lamps turn on"),
            SolarPhaseDetail(12, "Blue Hour", "🟦", "${fmt(ebStart)} – ${fmt(ebEnd)}", dur(ebStart, ebEnd), "Sun elevation -4° to -6° • Deep cobalt evening sky"),
            SolarPhaseDetail(13, "Nautical Twilight", "⛵", "${fmt(ebEnd)} – ${fmt(nautEnd)}", dur(ebEnd, nautEnd), "Sun elevation -6° to -12° • Stars appear, marine navigation horizon"),
            SolarPhaseDetail(14, "Astronomical Twilight", "🌌", "${fmt(nautEnd)} – ${fmt(astroEnd)}", dur(nautEnd, astroEnd), "Sun elevation -12° to -18° • Faint stars and galaxies visible"),
            SolarPhaseDetail(15, "Night", "🌌", "${fmt(astroEnd)} – 23:59", dur(astroEnd, date.plusDays(1).atStartOfDay(zoneId).toInstant()), "Sun elevation < -18° • Complete night sky darkness")
        )
    }

    fun formatTime(time: LocalTime, is24Hour: Boolean = false): String {
        val pattern = if (is24Hour) "HH:mm" else "h:mm a"
        return time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }
}
