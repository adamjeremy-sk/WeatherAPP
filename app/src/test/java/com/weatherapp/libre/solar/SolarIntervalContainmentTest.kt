package com.weatherapp.libre.solar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SolarIntervalContainmentTest {

    @Test
    fun testSaskatoonOverlapHighlights() {
        val date = LocalDate.of(2026, 10, 2)
        val zoneId = ZoneId.of("America/Regina")
        val observer = Observer(52.13, -106.67)

        val fullPhases = SolarLightingEngine.calculateChronologicalSolarPhases(date, zoneId, observer)

        // We want to test overlap between Civil Twilight and Golden Hour.
        // Let's pick an instant inside that overlap based on the dynamic calculation
        val civilStart = fullPhases.find { it.name == "Civil Twilight" }?.startInstant!!
        val goldenStart = fullPhases.find { it.name == "Golden Hour" }?.startInstant!!
        // The overlap starts at max(civilStart, goldenStart)
        val overlapStart = if (civilStart.isAfter(goldenStart)) civilStart else goldenStart
        val morningOverlapInstant = overlapStart.plusSeconds(60)

        val morningPhases = SolarLightingEngine.calculateChronologicalSolarPhases(date, zoneId, observer, nowInstant = morningOverlapInstant)
        val activeMorningNames = morningPhases.filter { it.isCurrentActivePhase }.map { it.name }.toSet()
        assertEquals(setOf("Civil Twilight", "Golden Hour"), activeMorningNames)

        // 2. Midnight (00:00) - Night
        val midnightInstant = date.atTime(0, 0).atZone(zoneId).toInstant()
        val midnightPhases = SolarLightingEngine.calculateChronologicalSolarPhases(date, zoneId, observer, nowInstant = midnightInstant)
        val activeMidnightNames = midnightPhases.filter { it.isCurrentActivePhase }.map { it.name }.toSet()
        assertEquals(setOf("Night"), activeMidnightNames)

        // 3. Midday (12:00) - Daytime
        val middayInstant = date.atTime(12, 0).atZone(zoneId).toInstant()
        val middayPhases = SolarLightingEngine.calculateChronologicalSolarPhases(date, zoneId, observer, nowInstant = middayInstant)
        val activeMiddayNames = middayPhases.filter { it.isCurrentActivePhase }.map { it.name }.toSet()
        assertEquals(setOf("Daytime"), activeMiddayNames)

        // 4. Boundary Test: Instant EQUAL to phase end (e.g. End of Civil Twilight == Sunrise)
        // Find Sunrise exact instant
        val srInstant = morningPhases.find { it.name == "Sunrise" }!!.startInstant!!
        val boundaryPhases = SolarLightingEngine.calculateChronologicalSolarPhases(date, zoneId, observer, nowInstant = srInstant)
        val boundaryNames = boundaryPhases.filter { it.isCurrentActivePhase }.map { it.name }.toSet()
        
        // Civil Twilight is [start, end). So at Sunrise, Civil Twilight is NOT active.
        assertFalse(boundaryNames.contains("Civil Twilight"))
        // Sunrise is an event, and Daytime is [Sunrise, Sunset).
        assertTrue(boundaryNames.contains("Sunrise"))
        assertTrue(boundaryNames.contains("Daytime"))
        assertTrue(boundaryNames.contains("Golden Hour"))

        // 5. Event window edge (-181s and -180s from Sunrise)
        val minus181Phases = SolarLightingEngine.calculateChronologicalSolarPhases(date, zoneId, observer, nowInstant = srInstant.minusSeconds(181))
        assertFalse(minus181Phases.find { it.name == "Sunrise" }!!.isCurrentActivePhase)

        val minus180Phases = SolarLightingEngine.calculateChronologicalSolarPhases(date, zoneId, observer, nowInstant = srInstant.minusSeconds(180))
        assertTrue(minus180Phases.find { it.name == "Sunrise" }!!.isCurrentActivePhase)
    }
}
