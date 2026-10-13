package com.weatherapp.libre.solar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SolarPositionEngineTest {

    @Test
    fun testRefractionEarlyReturn() {
        // At night when trueAltitudeDegrees < -1.0 (e.g. -2.0)
        val observer = Observer(45.0, -75.0)
        val midnightInstant = Instant.parse("2026-06-21T04:00:00Z") // Midnight local time in Toronto

        val topo = SolarPositionEngine.computeTopocentric(midnightInstant, observer)
        assertTrue("Expected trueAltitudeDegrees < -1.0", topo.trueAltitudeDegrees < -1.0)
        assertEquals("Apparent altitude should equal true altitude below -1.0°", topo.trueAltitudeDegrees, topo.apparentAltitudeDegrees, 1e-12)
    }

    @Test
    fun testRefractionHorizonSanity() {
        val observer = Observer(45.0, -75.0)
        // Find instant near true altitude 0.0° (around 09:30 UTC in Toronto on June 21)
        var bestInstant = Instant.parse("2026-06-21T09:30:00Z")
        var minAbsAlt = 90.0

        for (sec in 0..1800 step 30) {
            val inst = Instant.parse("2026-06-21T09:20:00Z").plusSeconds(sec.toLong())
            val topo = SolarPositionEngine.computeTopocentric(inst, observer)
            val absAlt = kotlin.math.abs(topo.trueAltitudeDegrees)
            if (absAlt < minAbsAlt) {
                minAbsAlt = absAlt
                bestInstant = inst
            }
        }

        val topo = SolarPositionEngine.computeTopocentric(bestInstant, observer)
        val refraction = topo.apparentAltitudeDegrees - topo.trueAltitudeDegrees
        // At true altitude ~ 0.0°, Bennett formula gives ~0.48°
        assertEquals(0.48, refraction, 0.05)
    }

    @Test
    fun testAtan2StabilityNorthPole() {
        val northPoleObserver = Observer(90.0, 0.0)
        val now = Instant.parse("2026-06-21T12:00:00Z")

        val topo = SolarPositionEngine.computeTopocentric(now, northPoleObserver)

        assertNotNull(topo)
        assertFalse("azimuthDegrees must not be NaN", topo.azimuthDegrees.isNaN())
        assertFalse("trueAltitudeDegrees must not be NaN", topo.trueAltitudeDegrees.isNaN())
        assertFalse("apparentAltitudeDegrees must not be NaN", topo.apparentAltitudeDegrees.isNaN())
    }
}
