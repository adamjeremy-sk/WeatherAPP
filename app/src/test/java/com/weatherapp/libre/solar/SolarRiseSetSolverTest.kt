package com.weatherapp.libre.solar

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SolarRiseSetSolverTest {

    @Test
    fun testApparentAltitudeShiftMidLatitude() {
        val date = LocalDate.of(2026, 6, 21)
        val observer = Observer(45.0, -75.0)
        val zoneId = ZoneId.of("America/Toronto")

        val result = SolarRiseSetSolver.solve(date, zoneId, observer)
        assertNotNull("Sunrise event must not be null", result.sunrise)

        val localTime = result.sunrise!!.instantUtc.atZone(zoneId).toLocalTime()
        assertTrue("Sunrise local time $localTime should be between 04:00 and 08:00", localTime.hour in 4..7)
    }

    @Test
    fun testHorizonDipEffect() {
        val date = LocalDate.of(2026, 6, 21)
        val zoneId = ZoneId.of("America/Toronto")
        val observerSeaLevel = Observer(45.0, -75.0, elevationMeters = 0.0)
        val observerHighElev = Observer(45.0, -75.0, elevationMeters = 1000.0)

        val res0m = SolarRiseSetSolver.solve(date, zoneId, observerSeaLevel)
        val res1000m = SolarRiseSetSolver.solve(date, zoneId, observerHighElev)

        assertNotNull(res0m.sunrise)
        assertNotNull(res1000m.sunrise)

        val sr0m = res0m.sunrise!!.instantUtc
        val sr1000m = res1000m.sunrise!!.instantUtc

        assertTrue("1000m sunrise ($sr1000m) must be strictly EARLIER than 0m sunrise ($sr0m)", sr1000m.isBefore(sr0m))
    }

    @Test
    fun testNegativeEpochRobustness() {
        val date = LocalDate.of(1969, 7, 20)
        val observer = Observer(45.0, -75.0)
        val zoneId = ZoneId.of("America/Toronto")

        val result = SolarRiseSetSolver.solve(date, zoneId, observer)
        assertNotNull(result)

        if (result.sunrise != null) {
            assertTrue("Sunrise nano must be >= 0", result.sunrise!!.instantUtc.nano >= 0)
        }
        if (result.sunset != null) {
            assertTrue("Sunset nano must be >= 0", result.sunset!!.instantUtc.nano >= 0)
        }
    }

    @Test
    fun testArcticEdgeCaseMidnightSun() {
        val date = LocalDate.of(2026, 6, 21)
        val observer = Observer(70.0, 19.0)
        val zoneId = ZoneId.of("Europe/Oslo")

        val result = SolarRiseSetSolver.solve(date, zoneId, observer)
        assertNotNull("Result must not be null for midnight sun", result)
    }
}
