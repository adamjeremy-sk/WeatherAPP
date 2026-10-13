package com.weatherapp.libre.solar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SolarLightingEngineTest {

    @Test
    fun testDynamicTimelineMidLatitude() {
        val date = LocalDate.of(2026, 3, 20)
        val observer = Observer(45.0, -75.0)
        val zoneId = ZoneId.of("America/Toronto")

        val phases = SolarLightingEngine.calculateChronologicalSolarPhases(
            date = date,
            zoneId = zoneId,
            observer = observer
        )

        assertEquals("Expected exactly 15 phases for mid-latitude equinox", 15, phases.size)

        val dayStartInstant = date.atStartOfDay(zoneId).toInstant()
        val dayEndInstant = date.plusDays(1).atStartOfDay(zoneId).toInstant()

        assertEquals("First phase must start at dayStartInstant", dayStartInstant, phases.first().startInstant)
        assertEquals("Last phase must end at dayEndInstant", dayEndInstant, phases.last().endInstant)
    }

    @Test
    fun testPolarEdgeCaseNoAstroTwilight() {
        val date = LocalDate.of(2026, 6, 21)
        val observer = Observer(65.0, -18.0)
        val zoneId = ZoneId.of("Atlantic/Reykjavik")

        val phases = SolarLightingEngine.calculateChronologicalSolarPhases(
            date = date,
            zoneId = zoneId,
            observer = observer
        )

        for (phase in phases) {
            assertFalse("Phase ${phase.name} should not have '——' in timeRangeText", phase.timeRangeText.contains("——"))
            assertFalse("Phase ${phase.name} should not have '——' in durationText", phase.durationText.contains("——"))
        }

        val dayStartInstant = date.atStartOfDay(zoneId).toInstant()
        val dayEndInstant = date.plusDays(1).atStartOfDay(zoneId).toInstant()

        assertEquals("First phase must start at dayStartInstant", dayStartInstant, phases.first().startInstant)
        assertEquals("Last phase must end at dayEndInstant", dayEndInstant, phases.last().endInstant)
    }

    @Test
    fun test2359BugFix() {
        val date = LocalDate.of(2026, 3, 20)
        val observer = Observer(45.0, -75.0)
        val zoneId = ZoneId.of("America/Toronto")

        val phases = SolarLightingEngine.calculateChronologicalSolarPhases(
            date = date,
            zoneId = zoneId,
            observer = observer
        )

        val lastPhase = phases.last()
        assertFalse("Final phase timeRangeText '${lastPhase.timeRangeText}' must NOT contain '23:59'", lastPhase.timeRangeText.contains("23:59"))
    }
}
