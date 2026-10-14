package com.weatherapp.libre.lunar

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import kotlin.math.abs

class LunarEclipseCalculatorTest {

    private fun assertWithinMinutes(expectedIso: String, actual: Instant?, maxDiffMinutes: Long = 25) {
        assertNotNull("Timestamp should not be null", actual)
        val expected = Instant.parse(expectedIso)
        val diffSec = abs(expected.epochSecond - actual!!.epochSecond)
        val diffMin = diffSec / 60.0
        assertTrue(
            "Expected $expectedIso but got $actual (diff: ${String.format("%.2f", diffMin)} mins, allowed: $maxDiffMinutes mins)",
            diffSec <= maxDiffMinutes * 60
        )
    }

    private fun calculateEclipseInfo(instant: Instant): LunarEclipseInfo? {
        return when (val res = LunarEclipseCalculator.calculateLunarEclipse(instant)) {
            is EclipseCalculationResult.Success -> res.eclipse
            else -> null
        }
    }

    @Test
    fun testKnownBloodMoon_March2025() {
        // Full Moon of March 14, 2025 Total Lunar Eclipse (~06:54 UTC)
        val startUtc = Instant.parse("2025-03-13T00:00:00Z")
        val endUtc = Instant.parse("2025-03-15T00:00:00Z")
        val events = MoonPhaseCalculator.phaseEventsBetween(startUtc, endUtc)

        val fullMoonEvent = events.find { it.type == MoonPhaseName.FULL_MOON }
        assertNotNull("Full Moon event should be found in March 2025 range", fullMoonEvent)

        val eclipse = calculateEclipseInfo(fullMoonEvent!!.instantUtc)
        assertNotNull("Eclipse should be detected for March 14, 2025 Full Moon", eclipse)

        assertEquals(EclipseType.TOTAL, eclipse!!.type)
        assertTrue("Magnitude should be >= 1.0 for Total Eclipse (was ${eclipse.magnitude})", eclipse.magnitude >= 1.0)

        // NASA published peak / greatest eclipse: 06:58:43 UTC
        assertWithinMinutes("2025-03-14T06:58:43Z", eclipse.peakInstant, maxDiffMinutes = 10)

        // NASA published contact times:
        // P1: 03:57:28 UTC, U1: 05:09:33 UTC, U2: 06:26:00 UTC, U3: 07:31:25 UTC, U4: 08:47:52 UTC, P4: 09:59:58 UTC
        assertWithinMinutes("2025-03-14T03:57:28Z", eclipse.contacts.p1, maxDiffMinutes = 10)
        assertWithinMinutes("2025-03-14T05:09:33Z", eclipse.contacts.u1, maxDiffMinutes = 10)
        assertWithinMinutes("2025-03-14T06:26:00Z", eclipse.contacts.u2, maxDiffMinutes = 10)
        assertWithinMinutes("2025-03-14T07:31:25Z", eclipse.contacts.u3, maxDiffMinutes = 10)
        assertWithinMinutes("2025-03-14T08:47:52Z", eclipse.contacts.u4, maxDiffMinutes = 10)
        assertWithinMinutes("2025-03-14T09:59:58Z", eclipse.contacts.p4, maxDiffMinutes = 10)
    }

    @Test
    fun testPartialEclipse_September2024() {
        // Full Moon of September 18, 2024 Partial Lunar Eclipse (~02:34 UTC)
        val startUtc = Instant.parse("2024-09-17T00:00:00Z")
        val endUtc = Instant.parse("2024-09-19T00:00:00Z")
        val events = MoonPhaseCalculator.phaseEventsBetween(startUtc, endUtc)

        val fullMoonEvent = events.find { it.type == MoonPhaseName.FULL_MOON }
        assertNotNull("Full Moon event should be found in Sept 2024 range", fullMoonEvent)

        val eclipse = calculateEclipseInfo(fullMoonEvent!!.instantUtc)
        assertNotNull("Eclipse should be detected for Sept 18, 2024 Full Moon", eclipse)

        assertEquals(EclipseType.PARTIAL, eclipse!!.type)
        assertTrue("Magnitude should be between 0 and 1 for Partial Eclipse", eclipse.magnitude > 0.0 && eclipse.magnitude < 1.0)

        // NASA published peak: 02:44:18 UTC
        assertWithinMinutes("2024-09-18T02:44:18Z", eclipse.peakInstant, maxDiffMinutes = 30)

        // NASA published contacts:
        // P1: 00:41:07 UTC, U1: 02:12:48 UTC, U4: 03:15:41 UTC, P4: 04:47:27 UTC
        assertWithinMinutes("2024-09-18T00:41:07Z", eclipse.contacts.p1, maxDiffMinutes = 30)
        assertWithinMinutes("2024-09-18T02:12:48Z", eclipse.contacts.u1, maxDiffMinutes = 30)
        assertNull("U2 must be null for Partial Eclipse", eclipse.contacts.u2)
        assertNull("U3 must be null for Partial Eclipse", eclipse.contacts.u3)
        assertWithinMinutes("2024-09-18T03:15:41Z", eclipse.contacts.u4, maxDiffMinutes = 30)
        assertWithinMinutes("2024-09-18T04:47:27Z", eclipse.contacts.p4, maxDiffMinutes = 30)
    }

    @Test
    fun testPenumbralEclipse_March2024() {
        // Full Moon of March 25, 2024 Penumbral Lunar Eclipse (~07:00 UTC)
        val startUtc = Instant.parse("2024-03-24T00:00:00Z")
        val endUtc = Instant.parse("2024-03-26T00:00:00Z")
        val events = MoonPhaseCalculator.phaseEventsBetween(startUtc, endUtc)

        val fullMoonEvent = events.find { it.type == MoonPhaseName.FULL_MOON }
        assertNotNull("Full Moon event should be found in March 2024 range", fullMoonEvent)

        val eclipse = calculateEclipseInfo(fullMoonEvent!!.instantUtc)
        assertNotNull("Eclipse should be detected for March 25, 2024 Full Moon", eclipse)

        assertEquals(EclipseType.PENUMBRAL, eclipse!!.type)

        // NASA published peak: 07:12:51 UTC
        assertWithinMinutes("2024-03-25T07:12:51Z", eclipse.peakInstant, maxDiffMinutes = 30)

        // NASA published contacts:
        // P1: 04:53:16 UTC, P4: 09:32:18 UTC
        assertWithinMinutes("2024-03-25T04:53:16Z", eclipse.contacts.p1, maxDiffMinutes = 30)
        assertNull("U1 must be null for Penumbral Eclipse", eclipse.contacts.u1)
        assertNull("U2 must be null for Penumbral Eclipse", eclipse.contacts.u2)
        assertNull("U3 must be null for Penumbral Eclipse", eclipse.contacts.u3)
        assertNull("U4 must be null for Penumbral Eclipse", eclipse.contacts.u4)
        assertWithinMinutes("2024-03-25T09:32:18Z", eclipse.contacts.p4, maxDiffMinutes = 30)
    }

    @Test
    fun testGrazingEclipse() {
        // Shallow/grazing Partial Lunar Eclipse of October 28, 2023 (mag ~0.12)
        val startUtc = Instant.parse("2023-10-27T00:00:00Z")
        val endUtc = Instant.parse("2023-10-29T00:00:00Z")
        val events = MoonPhaseCalculator.phaseEventsBetween(startUtc, endUtc)

        val fullMoonEvent = events.find { it.type == MoonPhaseName.FULL_MOON }
        assertNotNull("Full Moon event should be found for Oct 2023", fullMoonEvent)

        val eclipse = calculateEclipseInfo(fullMoonEvent!!.instantUtc)
        assertNotNull("Grazing eclipse should be detected for Oct 28, 2023", eclipse)

        assertEquals(EclipseType.PARTIAL, eclipse!!.type)
        assertTrue("Grazing magnitude should be small (>0 and < 0.3)", eclipse.magnitude > 0.0 && eclipse.magnitude < 0.3)
        assertNotNull(eclipse.contacts.p1)
        assertNotNull(eclipse.contacts.u1)
        assertNull("U2 must be null for grazing partial eclipse", eclipse.contacts.u2)
        assertNull("U3 must be null for grazing partial eclipse", eclipse.contacts.u3)
        assertNotNull(eclipse.contacts.u4)
        assertNotNull(eclipse.contacts.p4)
    }

    @Test
    fun testHistoricalEclipse() {
        // Historical Total Lunar Eclipse of November 7, 1938 (~22:26 UTC)
        val startUtc = Instant.parse("1938-11-06T00:00:00Z")
        val endUtc = Instant.parse("1938-11-08T23:59:59Z")
        val events = MoonPhaseCalculator.phaseEventsBetween(startUtc, endUtc)

        val fullMoonEvent = events.find { it.type == MoonPhaseName.FULL_MOON }
        assertNotNull("Full Moon event should be found for Nov 1938", fullMoonEvent)

        val eclipse = calculateEclipseInfo(fullMoonEvent!!.instantUtc)
        assertNotNull("Historical eclipse should be detected without exceptions", eclipse)
        assertEquals(EclipseType.TOTAL, eclipse!!.type)
        assertTrue("Historical eclipse magnitude >= 1.0", eclipse.magnitude >= 1.0)
    }

    @Test
    fun testChronologicalContactOrder() {
        // Total Lunar Eclipse of March 14, 2025
        val startUtc = Instant.parse("2025-03-13T00:00:00Z")
        val endUtc = Instant.parse("2025-03-15T00:00:00Z")
        val events = MoonPhaseCalculator.phaseEventsBetween(startUtc, endUtc)
        val fullMoonEvent = events.find { it.type == MoonPhaseName.FULL_MOON }!!

        val eclipse = calculateEclipseInfo(fullMoonEvent.instantUtc)!!
        val c = eclipse.contacts

        assertNotNull(c.p1)
        assertNotNull(c.u1)
        assertNotNull(c.u2)
        assertNotNull(eclipse.peakInstant)
        assertNotNull(c.u3)
        assertNotNull(c.u4)
        assertNotNull(c.p4)

        // Strict chronological order: P1 < U1 < U2 < Peak < U3 < U4 < P4
        assertTrue("P1 must be before U1", c.p1!!.isBefore(c.u1!!))
        assertTrue("U1 must be before U2", c.u1!!.isBefore(c.u2!!))
        assertTrue("U2 must be before Peak", c.u2!!.isBefore(eclipse.peakInstant))
        assertTrue("Peak must be before U3", eclipse.peakInstant.isBefore(c.u3!!))
        assertTrue("U3 must be before U4", c.u3!!.isBefore(c.u4!!))
        assertTrue("U4 must be before P4", c.u4!!.isBefore(c.p4!!))
    }

    @Test
    fun testNonFullMoonValidation() {
        // New Moon timestamp should return NoEclipse or Failure
        val newMoonInstant = Instant.parse("2025-03-29T10:48:00Z")
        val result = LunarEclipseCalculator.calculateLunarEclipse(newMoonInstant)
        assertTrue("Non-full-moon instant must return NoEclipse or Failure", result is EclipseCalculationResult.NoEclipse || result is EclipseCalculationResult.Failure)
    }

    @Test
    fun testNoEclipse_RegularFullMoon() {
        // Full Moon of February 12, 2025 (~13:53 UTC) has high ecliptic latitude and no eclipse
        val startUtc = Instant.parse("2025-02-11T00:00:00Z")
        val endUtc = Instant.parse("2025-02-13T23:59:59Z")
        val events = MoonPhaseCalculator.phaseEventsBetween(startUtc, endUtc)

        val fullMoonEvent = events.find { it.type == MoonPhaseName.FULL_MOON }
        assertNotNull("Full Moon event should be found in February 2025", fullMoonEvent)

        val result = LunarEclipseCalculator.calculateLunarEclipse(fullMoonEvent!!.instantUtc)
        assertTrue("No eclipse should be detected for February 2025 regular Full Moon", result is EclipseCalculationResult.NoEclipse)
    }
}
