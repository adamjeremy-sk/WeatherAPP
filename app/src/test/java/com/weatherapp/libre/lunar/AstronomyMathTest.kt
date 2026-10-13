package com.weatherapp.libre.lunar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class AstronomyMathTest {

    @Test
    fun testNormalizeDegrees() {
        // Negative inputs
        assertEquals(270.0, AstronomyMath.normalizeDegrees(-90.0), 1e-12)
        assertEquals(0.0, AstronomyMath.normalizeDegrees(-360.0), 1e-12)
        assertEquals(270.0, AstronomyMath.normalizeDegrees(-450.0), 1e-12)

        // Inputs above 360
        assertEquals(10.0, AstronomyMath.normalizeDegrees(370.0), 1e-12)
        assertEquals(0.0, AstronomyMath.normalizeDegrees(720.0), 1e-12)
        assertEquals(0.0, AstronomyMath.normalizeDegrees(3.6e6), 1e-12)

        // Idempotence & range [0, 360) check across test suite inputs
        val sampleAngles = doubleArrayOf(-720.0, -450.0, -90.0, -0.0, 0.0, 45.0, 180.0, 359.99, 360.0, 720.0, 3.6e6)
        for (angle in sampleAngles) {
            val normalized = AstronomyMath.normalizeDegrees(angle)
            assertTrue("Normalized angle $normalized out of bounds [0, 360)", (normalized >= 0.0) && (normalized < 360.0))
            assertEquals(normalized, AstronomyMath.normalizeDegrees(normalized), 1e-12)
        }
    }

    @Test
    fun testTrigPrecisionWithLargeAngle() {
        val largeAngles = doubleArrayOf(3.6e6, 3.6e6 + 45.0, 3.6e6 + 123.456, -3.6e6 - 30.0)
        for (x in largeAngles) {
            val sinNormalized = sin(AstronomyMath.degToRad(AstronomyMath.normalizeDegrees(x)))
            val sinRaw = sin(AstronomyMath.degToRad(x))
            assertEquals("Sin value mismatch for angle $x", sinNormalized, sinRaw, 1e-9)
        }
    }

    @Test
    fun testGmstDegreesBoundsAndDayRate() {
        // Year 1900 JD ~2415020.5, Year 2100 JD ~2488070.5
        val startJd = 2415020.5
        val endJd = 2488070.5
        var jd = startJd
        while (jd <= endJd) {
            val gmst = AstronomyMath.gmstDegrees(jd)
            assertTrue("GMST $gmst out of bounds [0, 360) at JD $jd", (gmst >= 0.0) && (gmst < 360.0))
            jd += 365.25 * 5 // Sample every 5 years
        }

        // Day-rate check: (gmstDegrees(jd + 1) - gmstDegrees(jd)) mod 360 equals 0.985647 within 1e-4
        val sampleJd = 2451545.0 // J2000.0
        val gmstToday = AstronomyMath.gmstDegrees(sampleJd)
        val gmstTomorrow = AstronomyMath.gmstDegrees(sampleJd + 1.0)
        val rateMod360 = AstronomyMath.normalizeDegrees(gmstTomorrow - gmstToday)
        assertEquals(0.985647, rateMod360, 1e-4)
    }
}
