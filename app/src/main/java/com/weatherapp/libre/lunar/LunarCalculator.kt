package com.weatherapp.libre.lunar

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object LunarCalculator {

    /**
     * Calculates MoonPhaseInfo for a given Instant UTC.
     */
    fun phaseAt(instant: Instant, observer: Observer? = null): MoonPhaseInfo {
        return MoonPhaseCalculator.phaseAt(instant, observer)
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
