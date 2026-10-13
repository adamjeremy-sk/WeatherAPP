package com.weatherapp.libre.lunar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AtmosphericRefractionTest {

    @Test
    fun testEarlyReturnForAltitudesBelowCutoff() {
        val belowCutoffAltitudes = doubleArrayOf(-1.0 - 1e-9, -5.0, -20.0, -90.0)
        for (alt in belowCutoffAltitudes) {
            val refraction = AtmosphericRefraction.calculateRefractionDegrees(alt)
            assertEquals("Refraction for altitude $alt must be 0.0", 0.0, refraction, 0.0)
        }
    }

    @Test
    fun testHorizonSanity() {
        val rHorizon = AtmosphericRefraction.calculateRefractionDegrees(0.0)
        assertTrue("Horizon refraction $rHorizon out of range [0.40, 0.55]", (rHorizon >= 0.40) && (rHorizon <= 0.55))
    }

    @Test
    fun testNearCutoffSanity() {
        val rNearCutoff = AtmosphericRefraction.calculateRefractionDegrees(-0.9)
        assertTrue("Near-cutoff refraction $rNearCutoff out of range [0.55, 0.75]", (rNearCutoff >= 0.55) && (rNearCutoff <= 0.75))
    }

    @Test
    fun testMonotonicDecreaseAndZenithZero() {
        val r0 = AtmosphericRefraction.calculateRefractionDegrees(0.0)
        val r10 = AtmosphericRefraction.calculateRefractionDegrees(10.0)
        val r30 = AtmosphericRefraction.calculateRefractionDegrees(30.0)
        val r60 = AtmosphericRefraction.calculateRefractionDegrees(60.0)
        val r90 = AtmosphericRefraction.calculateRefractionDegrees(90.0)

        assertTrue("Expected R(0) > R(10)", r0 > r10)
        assertTrue("Expected R(10) > R(30)", r10 > r30)
        assertTrue("Expected R(30) > R(60)", r30 > r60)
        assertTrue("Expected R(60) > R(90)", r60 > r90)

        assertEquals("Refraction at zenith (90°) must equal 0.0 due to defensive clamp", 0.0, r90, 0.0)
    }

    @Test
    fun testPressureScaling() {
        val rHighP = AtmosphericRefraction.calculateRefractionDegrees(10.0, pressureHpa = 1030.0, temperatureCelsius = 10.0)
        val rLowP = AtmosphericRefraction.calculateRefractionDegrees(10.0, pressureHpa = 990.0, temperatureCelsius = 10.0)
        assertTrue("Higher pressure should yield higher refraction", rHighP > rLowP)
    }

    @Test
    fun testTemperatureScaling() {
        val rCold = AtmosphericRefraction.calculateRefractionDegrees(10.0, pressureHpa = 1013.25, temperatureCelsius = -20.0)
        val rWarm = AtmosphericRefraction.calculateRefractionDegrees(10.0, pressureHpa = 1013.25, temperatureCelsius = 30.0)
        assertTrue("Lower temperature (denser air) should yield higher refraction", rCold > rWarm)
    }

    @Test
    fun testUpperBound() {
        var alt = -1.0
        while (alt <= 90.0) {
            val refraction = AtmosphericRefraction.calculateRefractionDegrees(alt)
            assertTrue("Refraction $refraction at altitude $alt exceeds upper bound 1.0 degree", refraction <= 1.0)
            alt += 0.5
        }
    }
}
