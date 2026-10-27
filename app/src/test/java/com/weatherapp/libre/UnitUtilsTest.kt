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

    @Test
    fun testFormatPrecipitation_NullReturnsEmDash() {
        assertEquals("——", UnitUtils.formatPrecipitation(null, UnitSystem.METRIC))
        assertEquals("——", UnitUtils.formatPrecipitation(null, UnitSystem.US_CUSTOMARY))
    }

    @Test
    fun testFormatPrecipitation_ZeroReturnsEmDash() {
        // Declare as Double? so the nullable overload (null/zero/trace handler) is selected
        val zero: Double? = 0.0
        assertEquals("——", UnitUtils.formatPrecipitation(zero, UnitSystem.METRIC))
        assertEquals("——", UnitUtils.formatPrecipitation(zero, UnitSystem.US_CUSTOMARY))
    }

    @Test
    fun testFormatPrecipitation_TraceThreshold() {
        // 0.05 mm is below the 0.1 mm trace threshold
        // Declare as Double? so the nullable overload (null/zero/trace handler) is selected
        val trace: Double? = 0.05
        assertEquals("Trace", UnitUtils.formatPrecipitation(trace, UnitSystem.METRIC))
        assertEquals("Trace", UnitUtils.formatPrecipitation(trace, UnitSystem.US_CUSTOMARY))
    }

    @Test
    fun testFormatPrecipitation_MetricNormal() {
        // 2.54 mm -> "%.1f mm" -> "2.5 mm"
        assertEquals("2.5 mm", UnitUtils.formatPrecipitation(2.54, UnitSystem.METRIC))
    }

    @Test
    fun testFormatPrecipitation_ImperialNormal() {
        // 25.4 mm / 25.4 = 1.0 -> "%.2f in" -> "1.00 in"
        assertEquals("1.00 in", UnitUtils.formatPrecipitation(25.4, UnitSystem.US_CUSTOMARY))
    }

    @Test
    fun testFormatPrecipitation_ImperialSmall() {
        // 1.27 mm / 25.4 = 0.05 -> "%.2f in" -> "0.05 in"
        assertEquals("0.05 in", UnitUtils.formatPrecipitation(1.27, UnitSystem.US_CUSTOMARY))
    }
}
