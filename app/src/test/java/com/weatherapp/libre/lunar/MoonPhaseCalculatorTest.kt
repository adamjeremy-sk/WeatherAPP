package com.weatherapp.libre.lunar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MoonPhaseCalculatorTest {

    @Test
    fun testQuarterPhaseWCorrection() {
        // k = 0.25 corresponds to First Quarter of Jan 2000
        val jdeFirstQuarter = MoonPhaseCalculator.computeMeeusPhaseJde(0.25)
        // J2000 New Moon is 2451550.09766, so First Quarter ~ +7.38 days = 2451557.48
        assertEquals(2451557.48, jdeFirstQuarter, 1.0)

        // k = 0.75 corresponds to Last Quarter of Jan 2000
        val jdeLastQuarter = MoonPhaseCalculator.computeMeeusPhaseJde(0.75)
        assertEquals(2451572.22, jdeLastQuarter, 1.0)
    }

    @Test
    fun testPrecedingNewMoonRobustness() {
        val startUtc = Instant.parse("2026-06-01T00:00:00Z")
        val endUtc = Instant.parse("2026-07-01T00:00:00Z")
        val phaseEvents = MoonPhaseCalculator.phaseEventsBetween(startUtc, endUtc)

        val newMoonEvent = phaseEvents.find { it.type == MoonPhaseName.NEW_MOON }
        assertTrue("New Moon event must be present in June 2026", newMoonEvent != null)

        val newMoonInstant = newMoonEvent!!.instantUtc

        // 1 second after New Moon -> age should be ~0 days
        val infoAfter = MoonPhaseCalculator.phaseAt(newMoonInstant.plusSeconds(1))
        assertTrue("Age 1 sec after New Moon (${infoAfter.ageDays}) should be ~0.0 days", infoAfter.ageDays < 0.05)

        // 1 second before New Moon -> age should be ~29.53 days (falls back to previous synodic month)
        val infoBefore = MoonPhaseCalculator.phaseAt(newMoonInstant.minusSeconds(1))
        assertTrue("Age 1 sec before New Moon (${infoBefore.ageDays}) should be near ~29.5 days", (infoBefore.ageDays >= 29.0) && (infoBefore.ageDays <= 30.0))
    }
}
