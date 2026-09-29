package com.weatherapp.libre.solar

import java.time.LocalDate
import java.time.ZoneId

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
}
