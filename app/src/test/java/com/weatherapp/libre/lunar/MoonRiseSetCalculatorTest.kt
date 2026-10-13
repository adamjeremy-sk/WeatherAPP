package com.weatherapp.libre.lunar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MoonRiseSetCalculatorTest {

    @Test
    fun testPre1970Robustness() {
        val observer = Observer(latitudeDegrees = 45.0, longitudeDegrees = -75.0)
        val startUtc = Instant.parse("1969-07-20T00:00:00Z")
        val endUtc = Instant.parse("1969-07-22T00:00:00Z")

        val events = MoonRiseSetCalculator.findRiseSetEventsInInterval(startUtc, endUtc, observer)
        assertTrue("Events in July 1969 interval must not be empty", events.isNotEmpty())

        for (event in events) {
            assertTrue("Event instant nanoseconds ${event.instantUtc.nano} must be non-negative", event.instantUtc.nano >= 0)
        }
    }

    @Test
    fun testPerformanceSanity() {
        val observer = Observer(latitudeDegrees = 45.0, longitudeDegrees = -75.0)
        val localDate = LocalDate.of(2026, 6, 21)
        val zoneId = ZoneId.of("America/Toronto")

        val startTimeMs = System.currentTimeMillis()
        val events = MoonRiseSetCalculator.moonEventsForLocalDate(localDate, zoneId, observer)
        val durationMs = System.currentTimeMillis() - startTimeMs

        assertTrue("moonEventsForLocalDate took $durationMs ms, expected < 500 ms", durationMs < 500)
        assertTrue("Expected events for local date", events.isNotEmpty())
    }

    @Test
    fun testRefractionSanity() {
        val observer = Observer(latitudeDegrees = 45.0, longitudeDegrees = -75.0)
        val localDate = LocalDate.of(2026, 6, 21)
        val zoneId = ZoneId.of("America/Toronto")

        val events = MoonRiseSetCalculator.moonEventsForLocalDate(localDate, zoneId, observer)
        val moonrise = events.find { it.type == MoonEventType.MOONRISE }
        assertTrue("Moonrise event must be found", moonrise != null)

        val eventInstant = moonrise!!.instantUtc
        val jdUtc = AstroTime.instantToJdUtc(eventInstant)
        val jdeTT = AstroTime.jdUtcToJdeTT(jdUtc)
        val lunar = LunarPosition.compute(jdeTT)

        // Returned altitudeDegrees is the apparent altitude, which should match targetAlt = -semidiameterDeg
        val expectedAlt = -lunar.semidiameterDeg
        assertEquals(expectedAlt, moonrise.altitudeDegrees, 0.01)
    }
}
