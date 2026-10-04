package com.weatherapp.libre.solar

import com.weatherapp.libre.SolarPhaseDetail
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object SolarLightingEngine {

    /**
     * Main entry point: calculates all exact solar lighting phases and events
     * for a given LocalDate, ZoneId, and Observer.
     */
    fun calculateSolarLightingPhases(
        localDate: LocalDate,
        zoneId: ZoneId,
        observer: Observer,
        lightingDefinitions: LightingDefinitions = LightingDefinitions.DEFAULT
    ): SolarLightingResult {
        return SolarRiseSetSolver.solve(localDate, zoneId, observer, lightingDefinitions)
    }

    fun calculateChronologicalSolarPhases(
        date: LocalDate,
        zoneId: ZoneId,
        observer: Observer,
        is24Hour: Boolean = true,
        nowInstant: Instant = Instant.now()
    ): List<SolarPhaseDetail> {
        val lighting = calculateSolarLightingPhases(date, zoneId, observer)

        val isToday = date.isEqual(nowInstant.atZone(zoneId).toLocalDate())

        fun fmt(instant: Instant?): String {
            if (instant == null) return "——"
            val lt = instant.atZone(zoneId).toLocalTime()
            val pattern = if (is24Hour) "HH:mm" else "h:mm a"
            return lt.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
        }

        fun dur(instantStart: Instant?, instantEnd: Instant?): String {
            if (instantStart == null || instantEnd == null) return "——"
            val mins = ChronoUnit.MINUTES.between(instantStart, instantEnd)
            val positiveMins = if (mins < 0) mins + 1440 else mins
            val h = positiveMins / 60
            val m = positiveMins % 60
            return if (h > 0) "${h}h ${m}m" else "${m}m"
        }

        val srInstant = lighting.sunrise?.instantUtc
        val ssInstant = lighting.sunset?.instantUtc

        if (srInstant == null && ssInstant == null) {
            val isMidnightSun = (lighting.solarNoon?.apparentAltitudeDegrees ?: 0.0) > 0.0
            val label = if (isMidnightSun) "Continuous Daylight (Sun does not set)" else "Polar Night (Sun does not rise)"
            val emoji = if (isMidnightSun) "☀️" else "🌌"
            val desc = if (isMidnightSun) "24 hours of continuous daylight • Sun remains above the horizon" else "24 hours of polar night • Sun remains below the horizon"

            return listOf(
                SolarPhaseDetail(
                    stepIndex = 1,
                    name = if (isMidnightSun) "Midnight Sun" else "Polar Night",
                    iconEmoji = emoji,
                    timeRangeText = label,
                    durationText = "24h 00m",
                    elevationDescription = desc,
                    isCurrentActivePhase = isToday,
                    startInstant = date.atStartOfDay(zoneId).toInstant(),
                    endInstant = date.plusDays(1).atStartOfDay(zoneId).toInstant()
                )
            )
        }

        val mgStart = lighting.morningGoldenHour?.startInstantUtc
        val mgEnd = lighting.morningGoldenHour?.endInstantUtc

        val egStart = lighting.eveningGoldenHour?.startInstantUtc
        val egEnd = lighting.eveningGoldenHour?.endInstantUtc

        val mbStart = lighting.morningBlueHour?.startInstantUtc
        val mbEnd = lighting.morningBlueHour?.endInstantUtc

        val ebStart = lighting.eveningBlueHour?.startInstantUtc
        val ebEnd = lighting.eveningBlueHour?.endInstantUtc

        val civilStart = lighting.civilTwilight?.startInstantUtc
        val nautStart = lighting.nauticalTwilight?.startInstantUtc
        val astroStart = lighting.astronomicalTwilight?.startInstantUtc

        val ecEnd = lighting.eveningCivilTwilight?.endInstantUtc
        val enEnd = lighting.eveningNauticalTwilight?.endInstantUtc
        val eaEnd = lighting.eveningAstronomicalTwilight?.endInstantUtc

        val dayStartInstant = date.atStartOfDay(zoneId).toInstant()
        val dayEndInstant = date.plusDays(1).atStartOfDay(zoneId).toInstant()

        val rawPhases = listOf(
            SolarPhaseDetail(1, "Night", "🌌", "Midnight – ${fmt(astroStart)}", dur(dayStartInstant, astroStart), "Sun elevation < -18° • Complete astronomical darkness", false, dayStartInstant, astroStart),
            SolarPhaseDetail(2, "Astronomical Twilight", "🌌", "${fmt(astroStart)} – ${fmt(nautStart)}", dur(astroStart, nautStart), "Sun elevation -18° to -12° • Sky begins subtle morning glow", false, astroStart, nautStart),
            SolarPhaseDetail(3, "Nautical Twilight", "⛵", "${fmt(nautStart)} – ${fmt(civilStart)}", dur(nautStart, civilStart), "Sun elevation -12° to -6° • Horizon clearly visible at sea", false, nautStart, civilStart),
            SolarPhaseDetail(4, "Blue Hour", "🟦", "${fmt(mbStart)} – ${fmt(mbEnd)}", dur(mbStart, mbEnd), "Sun elevation -6° to -4° • Deep electric blue sky for photography", false, mbStart, mbEnd),
            SolarPhaseDetail(5, "Civil Twilight", "🌅", "${fmt(civilStart)} – ${fmt(srInstant)}", dur(civilStart, srInstant), "Sun elevation -6° to -0.83° • Natural outdoor light, no lamps needed", false, civilStart, srInstant),
            SolarPhaseDetail(6, "Sunrise", "☀️", fmt(srInstant), "Event", "Sun upper limb crosses eastern horizon (elevation -0.83°)", false, srInstant, srInstant),
            SolarPhaseDetail(7, "Golden Hour", "🟧", "${fmt(mgStart)} – ${fmt(mgEnd)}", dur(mgStart, mgEnd), "Sun elevation -4° to +6° • Warm golden hues, soft long shadows", false, mgStart, mgEnd),
            SolarPhaseDetail(8, "Daytime", "☀️", "${fmt(srInstant)} – ${fmt(ssInstant)}", dur(srInstant, ssInstant), "Sun elevation > -0.83° • Daylight from end of morning civil twilight to start of evening civil twilight", false, srInstant, ssInstant),
            SolarPhaseDetail(9, "Golden Hour", "🟧", "${fmt(egStart)} – ${fmt(egEnd)}", dur(egStart, egEnd), "Sun elevation +6° to -4° • Warm magical evening photography light", false, egStart, egEnd),
            SolarPhaseDetail(10, "Sunset", "🌇", fmt(ssInstant), "Event", "Sun upper limb disappears below western horizon (elevation -0.83°)", false, ssInstant, ssInstant),
            SolarPhaseDetail(11, "Civil Twilight", "🌆", "${fmt(ssInstant)} – ${fmt(ecEnd)}", dur(ssInstant, ecEnd), "Sun elevation -0.83° to -6° • Dusk light, city lamps turn on", false, ssInstant, ecEnd),
            SolarPhaseDetail(12, "Blue Hour", "🟦", "${fmt(ebStart)} – ${fmt(ebEnd)}", dur(ebStart, ebEnd), "Sun elevation -4° to -6° • Deep cobalt evening sky", false, ebStart, ebEnd),
            SolarPhaseDetail(13, "Nautical Twilight", "⛵", "${fmt(ecEnd)} – ${fmt(enEnd)}", dur(ecEnd, enEnd), "Sun elevation -6° to -12° • Stars appear, marine navigation horizon", false, ecEnd, enEnd),
            SolarPhaseDetail(14, "Astronomical Twilight", "🌌", "${fmt(enEnd)} – ${fmt(eaEnd)}", dur(enEnd, eaEnd), "Sun elevation -12° to -18° • Faint stars and galaxies visible", false, enEnd, eaEnd),
            SolarPhaseDetail(15, "Night", "🌌", "${fmt(eaEnd)} – 23:59", dur(eaEnd, dayEndInstant), "Sun elevation < -18° • Complete night sky darkness", false, eaEnd, dayEndInstant)
        )

        if (!isToday) {
            return rawPhases
        }

        // Evaluate interval-containment test per phase for today
        return rawPhases.map { item ->
            val s = item.startInstant
            val e = item.endInstant
            val isActive = if (s == null || e == null) {
                false
            } else if (item.name == "Sunrise" || item.name == "Sunset") {
                !nowInstant.isBefore(s.minusSeconds(180)) && nowInstant.isBefore(s.plusSeconds(180))
            } else {
                s.isBefore(e) && !nowInstant.isBefore(s) && nowInstant.isBefore(e)
            }
            item.copy(isCurrentActivePhase = isActive)
        }
    }
}
