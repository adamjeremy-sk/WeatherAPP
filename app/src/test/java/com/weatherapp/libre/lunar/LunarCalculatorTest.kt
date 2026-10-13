package com.weatherapp.libre.lunar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class LunarCalculatorTest {

    @Test
    fun testCyclePercentageSemantics() {
        val now = Instant.now()

        // First Quarter: phaseAngleDegrees = 90.0, illumination = 0.5
        val firstQuarterInfo = MoonPhaseInfo(
            instantUtc = now,
            phaseAngleDegrees = 90.0,
            illuminationFraction = 0.5,
            ageDays = 7.38,
            phaseCategory = LunarPhaseCategory.FIRST_QUARTER,
            geocentricDistanceKm = 384400.0,
        )
        val firstQuarterFormatted = LunarCalculator.formatPhaseDisplay(firstQuarterInfo)
        assertEquals(25, firstQuarterFormatted.cyclePercentage)

        // Last Quarter: phaseAngleDegrees = 270.0, illumination = 0.5
        val lastQuarterInfo = MoonPhaseInfo(
            instantUtc = now,
            phaseAngleDegrees = 270.0,
            illuminationFraction = 0.5,
            ageDays = 22.14,
            phaseCategory = LunarPhaseCategory.LAST_QUARTER,
            geocentricDistanceKm = 384400.0,
        )
        val lastQuarterFormatted = LunarCalculator.formatPhaseDisplay(lastQuarterInfo)
        assertEquals(75, lastQuarterFormatted.cyclePercentage)
    }

    @Test
    fun testIlluminationRounding() {
        val now = Instant.now()

        val info996 = MoonPhaseInfo(
            instantUtc = now,
            phaseAngleDegrees = 180.0,
            illuminationFraction = 0.996,
            ageDays = 14.76,
            phaseCategory = LunarPhaseCategory.FULL,
            geocentricDistanceKm = 384400.0,
        )
        val formatted996 = LunarCalculator.formatPhaseDisplay(info996)
        assertTrue("Illumination text '${formatted996.illuminationText}' must contain '100%'", formatted996.illuminationText.contains("100%"))

        val info994 = MoonPhaseInfo(
            instantUtc = now,
            phaseAngleDegrees = 175.0,
            illuminationFraction = 0.994,
            ageDays = 14.50,
            phaseCategory = LunarPhaseCategory.FULL,
            geocentricDistanceKm = 384400.0,
        )
        val formatted994 = LunarCalculator.formatPhaseDisplay(info994)
        assertTrue("Illumination text '${formatted994.illuminationText}' must contain '99%'", formatted994.illuminationText.contains("99%"))
    }

    @Test
    fun testApiCleanupPhaseAtNoObserver() {
        val now = Instant.now()
        val phaseInfo = LunarCalculator.phaseAt(now)
        assertNotNull(phaseInfo)
        assertNotNull(phaseInfo.phaseCategory)
    }

    @Test
    fun testPhaseCategorySemantics() {
        fun getCategory(elongation: Double): LunarPhaseCategory {
            return when {
                (elongation < 22.5) || (elongation >= 337.5) -> LunarPhaseCategory.NEW
                elongation < 67.5 -> LunarPhaseCategory.WAXING_CRESCENT
                elongation < 112.5 -> LunarPhaseCategory.FIRST_QUARTER
                elongation < 157.5 -> LunarPhaseCategory.WAXING_GIBBOUS
                elongation < 202.5 -> LunarPhaseCategory.FULL
                elongation < 247.5 -> LunarPhaseCategory.WANING_GIBBOUS
                elongation < 292.5 -> LunarPhaseCategory.LAST_QUARTER
                else -> LunarPhaseCategory.WANING_CRESCENT
            }
        }

        assertEquals(LunarPhaseCategory.NEW, getCategory(10.0))
        assertEquals(LunarPhaseCategory.WAXING_CRESCENT, getCategory(45.0))
        assertEquals(LunarPhaseCategory.FIRST_QUARTER, getCategory(90.0))
        assertEquals(LunarPhaseCategory.WAXING_GIBBOUS, getCategory(135.0))
        assertEquals(LunarPhaseCategory.FULL, getCategory(180.0))
        assertEquals(LunarPhaseCategory.WANING_GIBBOUS, getCategory(225.0))
        assertEquals(LunarPhaseCategory.LAST_QUARTER, getCategory(270.0))
        assertEquals(LunarPhaseCategory.WANING_CRESCENT, getCategory(315.0))
    }

    @Test
    fun testEventTypeSemantics() {
        val startUtc = Instant.parse("2026-06-01T00:00:00Z")
        val endUtc = startUtc.plus(35, ChronoUnit.DAYS)

        val events = MoonPhaseCalculator.phaseEventsBetween(startUtc, endUtc)
        assertTrue("Expected phase events in 35-day window", events.isNotEmpty())

        val valid4States = setOf(
            MoonPhaseName.NEW_MOON,
            MoonPhaseName.FIRST_QUARTER,
            MoonPhaseName.FULL_MOON,
            MoonPhaseName.LAST_QUARTER,
        )

        for (event in events) {
            assertTrue("Event type ${event.type} must be strictly one of the 4 major quarter phase states", valid4States.contains(event.type))
        }
    }

    @Test
    fun testTypeSafetyGeocentricDistanceKm() {
        val now = Instant.now()
        val phaseInfo = LunarCalculator.phaseAt(now)
        val distanceKm: Double = phaseInfo.geocentricDistanceKm
        assertTrue("Geocentric distance $distanceKm km must be positive", distanceKm > 300000.0)
    }
}
