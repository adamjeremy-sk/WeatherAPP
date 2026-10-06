package com.weatherapp.libre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitUtilsTest {

    @Test
    fun testShouldShowGusts_nullFalse() {
        assertFalse(UnitUtils.shouldShowGusts(10.0, null))
    }

    @Test
    fun testShouldShowGusts_equalToWindPlusThreeFalse() {
        assertFalse(UnitUtils.shouldShowGusts(10.0, 13.0))
    }

    @Test
    fun testShouldShowGusts_oneTenthAboveThresholdTrue() {
        assertTrue(UnitUtils.shouldShowGusts(10.0, 13.1))
    }

    @Test
    fun testShouldShowGusts_belowWindFalse() {
        assertFalse(UnitUtils.shouldShowGusts(10.0, 8.0))
    }

    @Test
    fun testFormatWindGusts_nullAndSuppressedCases() {
        assertNull(UnitUtils.formatWindGusts(10.0, null, UnitSystem.METRIC))
        assertNull(UnitUtils.formatWindGusts(10.0, 13.0, UnitSystem.METRIC))
        assertNull(UnitUtils.formatWindGusts(10.0, 8.0, UnitSystem.METRIC))
    }

    @Test
    fun testFormatWindGusts_presentCases() {
        // Metric
        assertEquals("30 km/h", UnitUtils.formatWindGusts(10.0, 30.0, UnitSystem.METRIC))
        // US Customary: 30 km/h * 0.621371 = 18.64113 -> rounded to 19 mph
        assertEquals("19 mph", UnitUtils.formatWindGusts(10.0, 30.0, UnitSystem.US_CUSTOMARY))
    }
}
