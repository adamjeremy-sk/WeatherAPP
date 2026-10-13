package com.weatherapp.libre.lunar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.Instant

class TopocentricConverterTest {

    @Test
    fun testHorizonDipIntegration() {
        val observer = Observer(latitudeDegrees = 45.0, longitudeDegrees = -75.0, elevationMeters = 1000.0)
        val now = Instant.parse("2026-06-21T12:00:00Z")

        val state = TopocentricConverter.topocentricHorizontalState(now, observer)
        val jdUtc = AstroTime.instantToJdUtc(now)
        val jdeTT = AstroTime.jdUtcToJdeTT(jdUtc)
        val lunar = LunarPosition.compute(jdeTT)

        val dip = observer.effectiveHorizonDipDegrees
        val expectedH0 = -lunar.semidiameterDeg - dip

        assertEquals(expectedH0, state.horizonThresholdDegrees, 0.01)
    }

    @Test
    fun testAtan2StabilityNorthPole() {
        val northPoleObserver = Observer(latitudeDegrees = 90.0, longitudeDegrees = 0.0)
        val now = Instant.parse("2026-06-21T12:00:00Z")
        val jdUtc = AstroTime.instantToJdUtc(now)
        val jdeTT = AstroTime.jdUtcToJdeTT(jdUtc)
        val lunar = LunarPosition.compute(jdeTT)

        val topo = TopocentricConverter.computeTopocentric(lunar, northPoleObserver, jdUtc, jdeTT)

        assertNotNull(topo)
        assertFalse("topocentricDecDeg must not be NaN", topo.topocentricDecDeg.isNaN())
        assertFalse("topocentricRaDeg must not be NaN", topo.topocentricRaDeg.isNaN())
        assertFalse("trueAltitudeDegrees must not be NaN", topo.trueAltitudeDegrees.isNaN())
        assertFalse("apparentAltitudeDegrees must not be NaN", topo.apparentAltitudeDegrees.isNaN())
    }
}
