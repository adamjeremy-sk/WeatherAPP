package com.weatherapp.libre.lunar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class LunarPositionTest {

    @Test
    fun testRaDecExposure() {
        val now = Instant.parse("2026-06-21T12:00:00Z")
        val state = LunarPosition.geocentricState(now)

        assertTrue(
            "Right ascension ${state.rightAscensionDegrees}° out of bounds [0, 360)",
            (state.rightAscensionDegrees >= 0.0) && (state.rightAscensionDegrees < 360.0),
        )
        assertTrue(
            "Declination ${state.declinationDegrees}° out of bounds [-90, 90]",
            (state.declinationDegrees >= -90.0) && (state.declinationDegrees <= 90.0),
        )
    }

    @Test
    fun testSemidiameterSanity() {
        val now = Instant.parse("2026-06-21T12:00:00Z")
        val jdUtc = AstroTime.instantToJdUtc(now)
        val jdeTT = AstroTime.jdUtcToJdeTT(jdUtc)
        val coords = LunarPosition.compute(jdeTT)

        assertTrue("Semidiameter ${coords.semidiameterDeg}° must be strictly positive", coords.semidiameterDeg > 0.0)
        assertEquals("Semidiameter should be approx 0.259°", 0.259, coords.semidiameterDeg, 0.01)
    }

    @Test
    fun testNormalizationSanityFutureDate() {
        val futureInstant = Instant.parse("2500-01-01T00:00:00Z")
        val jdUtc = AstroTime.instantToJdUtc(futureInstant)
        val jdeTT = AstroTime.jdUtcToJdeTT(jdUtc)
        val coords = LunarPosition.compute(jdeTT)

        assertTrue(
            "Distance ${coords.distanceKm} km out of physical bounds [350000, 420000]",
            (coords.distanceKm >= 350000.0) && (coords.distanceKm <= 420000.0),
        )
        assertTrue(
            "Apparent longitude ${coords.apparentLongitudeDeg}° out of bounds [0, 360)",
            (coords.apparentLongitudeDeg >= 0.0) && (coords.apparentLongitudeDeg < 360.0),
        )
    }
}
