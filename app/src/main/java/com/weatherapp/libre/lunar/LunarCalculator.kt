package com.weatherapp.libre.lunar

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class FormattedLunarPhase(
    val phaseNameWithEmoji: String,
    val illuminationText: String,
    val cyclePercentage: Int
)

object LunarCalculator {

    /**
     * Calculates MoonPhaseInfo for a given Instant UTC.
     */
    fun phaseAt(instant: Instant, observer: Observer? = null): MoonPhaseInfo {
        return MoonPhaseCalculator.phaseAt(instant, observer)
    }

    /**
     * Formats MoonPhaseInfo into user-friendly phase name with emoji and illumination percentage text.
     */
    fun formatPhaseDisplay(moonInfo: MoonPhaseInfo): FormattedLunarPhase {
        val (phaseName, emoji) = when {
            moonInfo.phaseAngleDegrees < 22.5 || moonInfo.phaseAngleDegrees >= 337.5 -> Pair("New Moon", "🌑")
            moonInfo.phaseAngleDegrees < 67.5 -> Pair("Waxing Crescent", "🌒")
            moonInfo.phaseAngleDegrees < 112.5 -> Pair("First Quarter", "🌓")
            moonInfo.phaseAngleDegrees < 157.5 -> Pair("Waxing Gibbous", "🌔")
            moonInfo.phaseAngleDegrees < 202.5 -> Pair("Full Moon", "🌕")
            moonInfo.phaseAngleDegrees < 247.5 -> Pair("Waning Gibbous", "🌖")
            moonInfo.phaseAngleDegrees < 292.5 -> Pair("Last Quarter", "🌗")
            else -> Pair("Waning Crescent", "🌘")
        }
        val pct = (moonInfo.illuminationFraction * 100).toInt().coerceIn(0, 100)
        return FormattedLunarPhase(
            phaseNameWithEmoji = "$phaseName $emoji",
            illuminationText = "$pct% illuminated",
            cyclePercentage = pct
        )
    }

    /**
     * Finds all exact MoonPhaseEvents (New Moon, First Quarter, Full Moon, Last Quarter)
     * occurring in UTC interval [startUtc, endUtc).
     */
    fun phaseEventsBetween(startUtc: Instant, endUtc: Instant): List<MoonPhaseEvent> {
        return MoonPhaseCalculator.phaseEventsBetween(startUtc, endUtc)
    }

    /**
     * Finds all Moonrise and Moonset events occurring during a local date for an observer.
     */
    fun moonEventsForLocalDate(
        localDate: LocalDate,
        zoneId: ZoneId,
        observer: Observer
    ): List<MoonEvent> {
        return MoonRiseSetCalculator.moonEventsForLocalDate(localDate, zoneId, observer)
    }
}
