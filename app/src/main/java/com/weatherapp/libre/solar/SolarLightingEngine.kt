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

        val civilStart = lighting.morningCivilTwilight?.startInstantUtc
        val nautStart = lighting.morningNauticalTwilight?.startInstantUtc
        val astroStart = lighting.morningAstronomicalTwilight?.startInstantUtc

        val ecEnd = lighting.eveningCivilTwilight?.endInstantUtc
        val enEnd = lighting.eveningNauticalTwilight?.endInstantUtc
        val eaEnd = lighting.eveningAstronomicalTwilight?.endInstantUtc

        val dayStartInstant = date.atStartOfDay(zoneId).toInstant()
        val dayEndInstant = date.plusDays(1).atStartOfDay(zoneId).toInstant()

        val firstMorningEvent = astroStart ?: nautStart ?: civilStart ?: srInstant
        val lastEveningEvent = eaEnd ?: enEnd ?: ecEnd ?: ssInstant

        val rawPhases = mutableListOf<SolarPhaseDetail>()
        var stepIndex = 1

        if (firstMorningEvent != null) {
            rawPhases.add(
                SolarPhaseDetail(
                    stepIndex = stepIndex++,
                    name = "Night",
                    iconEmoji = "🌌",
                    timeRangeText = "Midnight – ${fmt(firstMorningEvent)}",
                    durationText = dur(dayStartInstant, firstMorningEvent),
                    elevationDescription = "Sun elevation < -18° • Complete astronomical darkness",
                    isCurrentActivePhase = false,
                    startInstant = dayStartInstant,
                    endInstant = firstMorningEvent
                )
            )
        }

        if (astroStart != null && nautStart != null) {
            rawPhases.add(
                SolarPhaseDetail(
                    stepIndex = stepIndex++,
                    name = "Astronomical Twilight",
                    iconEmoji = "🌌",
                    timeRangeText = "${fmt(astroStart)} – ${fmt(nautStart)}",
                    durationText = dur(astroStart, nautStart),
                    elevationDescription = "Sun elevation -18° to -12° • Sky begins subtle morning glow",
                    isCurrentActivePhase = false,
                    startInstant = astroStart,
                    endInstant = nautStart
                )
            )
        }

        if (nautStart != null && civilStart != null) {
            rawPhases.add(
                SolarPhaseDetail(
                    stepIndex = stepIndex++,
                    name = "Nautical Twilight",
                    iconEmoji = "⛵",
                    timeRangeText = "${fmt(nautStart)} – ${fmt(civilStart)}",
                    durationText = dur(nautStart, civilStart),
                    elevationDescription = "Sun elevation -12° to -6° • Horizon clearly visible at sea",
                    isCurrentActivePhase = false,
                    startInstant = nautStart,
                    endInstant = civilStart
                )
            )
        }

        if (mbStart != null && mbEnd != null) {
            rawPhases.add(
                SolarPhaseDetail(
                    stepIndex = stepIndex++,
                    name = "Blue Hour",
                    iconEmoji = "🟦",
                    timeRangeText = "${fmt(mbStart)} – ${fmt(mbEnd)}",
                    durationText = dur(mbStart, mbEnd),
                    elevationDescription = "Sun elevation -8° to -4° • Extended deep blue sky for photography",
                    isCurrentActivePhase = false,
                    startInstant = mbStart,
                    endInstant = mbEnd
                )
            )
        }

        if (civilStart != null && srInstant != null) {
            rawPhases.add(
                SolarPhaseDetail(
                    stepIndex = stepIndex++,
                    name = "Civil Twilight",
                    iconEmoji = "🌅",
                    timeRangeText = "${fmt(civilStart)} – ${fmt(srInstant)}",
                    durationText = dur(civilStart, srInstant),
                    elevationDescription = "Sun elevation -6° to -0.83° • Natural outdoor light, no lamps needed",
                    isCurrentActivePhase = false,
                    startInstant = civilStart,
                    endInstant = srInstant
                )
            )
        }

        if (srInstant != null) {
            rawPhases.add(
                SolarPhaseDetail(
                    stepIndex = stepIndex++,
                    name = "Sunrise",
                    iconEmoji = "☀️",
                    timeRangeText = fmt(srInstant),
                    durationText = "Event",
                    elevationDescription = "Sun upper limb crosses eastern horizon (elevation -0.83°)",
                    isCurrentActivePhase = false,
                    startInstant = srInstant,
                    endInstant = srInstant
                )
            )
        }

        if (mgStart != null && mgEnd != null) {
            rawPhases.add(
                SolarPhaseDetail(
                    stepIndex = stepIndex++,
                    name = "Golden Hour",
                    iconEmoji = "🟧",
                    timeRangeText = "${fmt(mgStart)} – ${fmt(mgEnd)}",
                    durationText = dur(mgStart, mgEnd),
                    elevationDescription = "Sun elevation -4° to +6° • Warm golden hues, soft long shadows",
                    isCurrentActivePhase = false,
                    startInstant = mgStart,
                    endInstant = mgEnd
                )
            )
        }

        if (srInstant != null && ssInstant != null) {
            rawPhases.add(
                SolarPhaseDetail(
                    stepIndex = stepIndex++,
                    name = "Daytime",
                    iconEmoji = "☀️",
                    timeRangeText = "${fmt(srInstant)} – ${fmt(ssInstant)}",
                    durationText = dur(srInstant, ssInstant),
                    elevationDescription = "Sun elevation > -0.83° • Daylight from end of morning civil twilight to start of evening civil twilight",
                    isCurrentActivePhase = false,
                    startInstant = srInstant,
                    endInstant = ssInstant
                )
            )
        }

        if (egStart != null && egEnd != null) {
            rawPhases.add(
                SolarPhaseDetail(
                    stepIndex = stepIndex++,
                    name = "Golden Hour",
                    iconEmoji = "🟧",
                    timeRangeText = "${fmt(egStart)} – ${fmt(egEnd)}",
                    durationText = dur(egStart, egEnd),
                    elevationDescription = "Sun elevation +6° to -4° • Warm magical evening photography light",
                    isCurrentActivePhase = false,
                    startInstant = egStart,
                    endInstant = egEnd
                )
            )
        }

        if (ssInstant != null) {
            rawPhases.add(
                SolarPhaseDetail(
                    stepIndex = stepIndex++,
                    name = "Sunset",
                    iconEmoji = "🌇",
                    timeRangeText = fmt(ssInstant),
                    durationText = "Event",
                    elevationDescription = "Sun upper limb disappears below western horizon (elevation -0.83°)",
                    isCurrentActivePhase = false,
                    startInstant = ssInstant,
                    endInstant = ssInstant
                )
            )
        }

        if (ssInstant != null && ecEnd != null) {
            rawPhases.add(
                SolarPhaseDetail(
                    stepIndex = stepIndex++,
                    name = "Civil Twilight",
                    iconEmoji = "🌆",
                    timeRangeText = "${fmt(ssInstant)} – ${fmt(ecEnd)}",
                    durationText = dur(ssInstant, ecEnd),
                    elevationDescription = "Sun elevation -0.83° to -6° • Dusk light, city lamps turn on",
                    isCurrentActivePhase = false,
                    startInstant = ssInstant,
                    endInstant = ecEnd
                )
            )
        }

        if (ebStart != null && ebEnd != null) {
            rawPhases.add(
                SolarPhaseDetail(
                    stepIndex = stepIndex++,
                    name = "Blue Hour",
                    iconEmoji = "🟦",
                    timeRangeText = "${fmt(ebStart)} – ${fmt(ebEnd)}",
                    durationText = dur(ebStart, ebEnd),
                    elevationDescription = "Sun elevation -4° to -8° • Extended deep cobalt evening sky",
                    isCurrentActivePhase = false,
                    startInstant = ebStart,
                    endInstant = ebEnd
                )
            )
        }

        if (ecEnd != null && enEnd != null) {
            rawPhases.add(
                SolarPhaseDetail(
                    stepIndex = stepIndex++,
                    name = "Nautical Twilight",
                    iconEmoji = "⛵",
                    timeRangeText = "${fmt(ecEnd)} – ${fmt(enEnd)}",
                    durationText = dur(ecEnd, enEnd),
                    elevationDescription = "Sun elevation -6° to -12° • Stars appear, marine navigation horizon",
                    isCurrentActivePhase = false,
                    startInstant = ecEnd,
                    endInstant = enEnd
                )
            )
        }

        if (enEnd != null && eaEnd != null) {
            rawPhases.add(
                SolarPhaseDetail(
                    stepIndex = stepIndex++,
                    name = "Astronomical Twilight",
                    iconEmoji = "🌌",
                    timeRangeText = "${fmt(enEnd)} – ${fmt(eaEnd)}",
                    durationText = dur(enEnd, eaEnd),
                    elevationDescription = "Sun elevation -12° to -18° • Faint stars and galaxies visible",
                    isCurrentActivePhase = false,
                    startInstant = enEnd,
                    endInstant = eaEnd
                )
            )
        }

        if (lastEveningEvent != null) {
            rawPhases.add(
                SolarPhaseDetail(
                    stepIndex = stepIndex++,
                    name = "Night",
                    iconEmoji = "🌌",
                    timeRangeText = "${fmt(lastEveningEvent)} – ${fmt(dayEndInstant)}",
                    durationText = dur(lastEveningEvent, dayEndInstant),
                    elevationDescription = "Sun elevation < -18° • Complete night sky darkness",
                    isCurrentActivePhase = false,
                    startInstant = lastEveningEvent,
                    endInstant = dayEndInstant
                )
            )
        }

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
