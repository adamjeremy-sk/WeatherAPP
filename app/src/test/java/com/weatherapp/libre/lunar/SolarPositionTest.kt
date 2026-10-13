package com.weatherapp.libre.lunar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SolarPositionTest {

    @Test
    fun testNormalizationSanityFutureDate() {
        val futureInstant = Instant.parse("2500-01-01T00:00:00Z")
        val jdUtc = AstroTime.instantToJdUtc(futureInstant)
        val jdeTT = AstroTime.jdUtcToJdeTT(jdUtc)
        val coords = SolarPosition.compute(jdeTT)

        assertTrue(
            "Apparent longitude ${coords.apparentLongitudeDeg}° out of bounds [0, 360)",
            (coords.apparentLongitudeDeg >= 0.0) && (coords.apparentLongitudeDeg < 360.0),
        )
        assertTrue(
            "Solar distance ${coords.distanceAu} AU out of physical bounds [0.98, 1.02]",
            (coords.distanceAu >= 0.98) && (coords.distanceAu <= 1.02),
        )
        assertEquals(0.0, coords.latitudeDeg, 1e-12)
    }

    @Test
    fun testTimescaleMigrationCurrentEpoch() {
        val now = Instant.parse("2026-06-21T12:00:00Z")
        val jdUtc = AstroTime.instantToJdUtc(now)
        val jdeTT = AstroTime.jdUtcToJdeTT(jdUtc)
        val coords = SolarPosition.compute(jdeTT)

        assertTrue(
            "Apparent longitude ${coords.apparentLongitudeDeg}° out of bounds [0, 360)",
            (coords.apparentLongitudeDeg >= 0.0) && (coords.apparentLongitudeDeg < 360.0),
        )
        assertTrue(
            "Solar distance ${coords.distanceAu} AU out of physical bounds [0.98, 1.02]",
            (coords.distanceAu >= 0.98) && (coords.distanceAu <= 1.02),
        )
    }
}
