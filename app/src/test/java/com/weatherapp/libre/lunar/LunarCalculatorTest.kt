package com.weatherapp.libre.lunar

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class LunarCalculatorTest {

    @Test
    fun testKnownPhaseInstants() {
        val observer = Observer(43.6532, -79.3832) // Toronto

        // Full Moon: 2026-01-03
        val instantJan3 = ZonedDateTime.of(2026, 1, 3, 12, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        val phaseJan3 = LunarCalculator.phaseAt(instantJan3, observer)
        assertEquals(MoonPhaseName.FULL_MOON, phaseJan3.phaseName)
        assertTrue(phaseJan3.illuminationFraction > 0.90)

        // New Moon: 2026-01-18
        val instantJan18 = ZonedDateTime.of(2026, 1, 18, 12, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        val phaseJan18 = LunarCalculator.phaseAt(instantJan18, observer)
        assertEquals(MoonPhaseName.NEW_MOON, phaseJan18.phaseName)
        assertTrue(phaseJan18.illuminationFraction < 0.10)
    }

    @Test
    fun testPhaseEventsBetween() {
        val startUtc = Instant.parse("2026-01-01T00:00:00Z")
        val endUtc = Instant.parse("2026-01-31T23:59:59Z")

        val events = LunarCalculator.phaseEventsBetween(startUtc, endUtc)
        assertFalse(events.isEmpty())

        // Check phase events sequence order and illumination property
        for (i in 0 until events.size - 1) {
            assertTrue(events[i].instantUtc.isBefore(events[i + 1].instantUtc))
        }
    }

    @Test
    fun testMoonRiseSetForToronto() {
        val torontoObserver = Observer(
            latitudeDegrees = 43.6532,
            longitudeDegrees = -79.3832,
            elevationMeters = 76.0
        )
        val localDate = LocalDate.of(2026, 9, 26)
        val zoneId = ZoneId.of("America/Toronto")

        val events = LunarCalculator.moonEventsForLocalDate(localDate, zoneId, torontoObserver)

        assertNotNull(events)
        for (event in events) {
            assertTrue(event.azimuthDegrees in 0.0..360.0)
            val eventZdt = event.instantUtc.atZone(zoneId)
            assertEquals(localDate, eventZdt.toLocalDate())
        }
    }

    @Test
    fun testPropertyInvariants() {
        val instant = Instant.now()
        val phase = LunarCalculator.phaseAt(instant)

        assertTrue(phase.illuminationFraction in 0.0..1.0)
        assertTrue(phase.phaseAngleDegrees in 0.0..360.0)
        assertTrue(phase.ageDays >= 0.0 && phase.ageDays <= 31.0)
        assertNotNull(phase.geocentricDistanceKm)
        assertTrue(phase.geocentricDistanceKm!! in 350000.0..410000.0)
    }

    @Test
    fun testHighLatitudeNoSingularity() {
        val reykjavikObserver = Observer(
            latitudeDegrees = 64.1466,
            longitudeDegrees = -21.9426
        )
        val localDate = LocalDate.of(2026, 6, 21)
        val zoneId = ZoneId.of("Atlantic/Reykjavik")

        val events = LunarCalculator.moonEventsForLocalDate(localDate, zoneId, reykjavikObserver)
        assertNotNull(events)
    }
}
