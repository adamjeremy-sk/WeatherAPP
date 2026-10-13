package com.weatherapp.libre.solar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SolarPhaseMetadataTest {

    @Test
    fun testPhaseMetadata() {
        val date = LocalDate.of(2026, 3, 20)
        val observer = Observer(45.0, -75.0)
        val zoneId = ZoneId.of("America/Toronto")
        val result = SolarRiseSetSolver.solve(date, zoneId, observer)

        val defs = LightingDefinitions.DEFAULT

        // Morning Astronomical Twilight
        val mat = result.morningAstronomicalTwilight
        assertNotNull("morningAstronomicalTwilight must be present", mat)
        assertEquals(defs.astronomicalTwilightDegrees, mat!!.lowerAltitudeDegrees!!, 1e-9)
        assertEquals(defs.nauticalTwilightDegrees, mat.upperAltitudeDegrees!!, 1e-9)

        // Morning Nautical Twilight
        val mnt = result.morningNauticalTwilight
        assertNotNull("morningNauticalTwilight must be present", mnt)
        assertEquals(defs.nauticalTwilightDegrees, mnt!!.lowerAltitudeDegrees!!, 1e-9)
        assertEquals(defs.civilTwilightDegrees, mnt.upperAltitudeDegrees!!, 1e-9)

        // Morning Civil Twilight
        val mct = result.morningCivilTwilight
        assertNotNull("morningCivilTwilight must be present", mct)
        assertEquals(defs.civilTwilightDegrees, mct!!.lowerAltitudeDegrees!!, 1e-9)
        assertEquals(defs.sunriseSunsetAltitudeDegrees, mct.upperAltitudeDegrees!!, 1e-9)

        // Morning Blue Hour
        val mbh = result.morningBlueHour
        assertNotNull("morningBlueHour must be present", mbh)
        assertEquals(defs.blueHourLowerBoundDegrees, mbh!!.lowerAltitudeDegrees!!, 1e-9)
        assertEquals(defs.blueHourUpperBoundDegrees, mbh.upperAltitudeDegrees!!, 1e-9)

        // Morning Golden Hour
        val mgh = result.morningGoldenHour
        assertNotNull("morningGoldenHour must be present", mgh)
        assertEquals(defs.goldenHourLowerBoundDegrees, mgh!!.lowerAltitudeDegrees!!, 1e-9)
        assertEquals(defs.goldenHourUpperBoundDegrees, mgh.upperAltitudeDegrees!!, 1e-9)

        // Evening Astronomical Twilight
        val eat = result.eveningAstronomicalTwilight
        assertNotNull("eveningAstronomicalTwilight must be present", eat)
        assertEquals(defs.astronomicalTwilightDegrees, eat!!.lowerAltitudeDegrees!!, 1e-9)
        assertEquals(defs.nauticalTwilightDegrees, eat.upperAltitudeDegrees!!, 1e-9)

        // Evening Nautical Twilight
        val ent = result.eveningNauticalTwilight
        assertNotNull("eveningNauticalTwilight must be present", ent)
        assertEquals(defs.nauticalTwilightDegrees, ent!!.lowerAltitudeDegrees!!, 1e-9)
        assertEquals(defs.civilTwilightDegrees, ent.upperAltitudeDegrees!!, 1e-9)

        // Evening Civil Twilight
        val ect = result.eveningCivilTwilight
        assertNotNull("eveningCivilTwilight must be present", ect)
        assertEquals(defs.civilTwilightDegrees, ect!!.lowerAltitudeDegrees!!, 1e-9)
        assertEquals(defs.sunriseSunsetAltitudeDegrees, ect.upperAltitudeDegrees!!, 1e-9)

        // Evening Blue Hour
        val ebh = result.eveningBlueHour
        assertNotNull("eveningBlueHour must be present", ebh)
        assertEquals(defs.blueHourLowerBoundDegrees, ebh!!.lowerAltitudeDegrees!!, 1e-9)
        assertEquals(defs.blueHourUpperBoundDegrees, ebh.upperAltitudeDegrees!!, 1e-9)

        // Evening Golden Hour
        val egh = result.eveningGoldenHour
        assertNotNull("eveningGoldenHour must be present", egh)
        assertEquals(defs.goldenHourLowerBoundDegrees, egh!!.lowerAltitudeDegrees!!, 1e-9)
        assertEquals(defs.goldenHourUpperBoundDegrees, egh.upperAltitudeDegrees!!, 1e-9)
    }

    @Test
    fun testNamingSymmetry() {
        val date = LocalDate.of(2026, 3, 20)
        val observer = Observer(45.0, -75.0)
        val zoneId = ZoneId.of("America/Toronto")
        val result = SolarRiseSetSolver.solve(date, zoneId, observer)

        // Civil twilight pair
        val mct = result.morningCivilTwilight!!
        val ect = result.eveningCivilTwilight!!
        assertTrue(
            "morningCivilTwilight start (${mct.startInstantUtc}) must be strictly before eveningCivilTwilight start (${ect.startInstantUtc})",
            mct.startInstantUtc.isBefore(ect.startInstantUtc)
        )

        // Nautical twilight pair
        val mnt = result.morningNauticalTwilight!!
        val ent = result.eveningNauticalTwilight!!
        assertTrue(
            "morningNauticalTwilight start (${mnt.startInstantUtc}) must be strictly before eveningNauticalTwilight start (${ent.startInstantUtc})",
            mnt.startInstantUtc.isBefore(ent.startInstantUtc)
        )

        // Astronomical twilight pair
        val mat = result.morningAstronomicalTwilight!!
        val eat = result.eveningAstronomicalTwilight!!
        assertTrue(
            "morningAstronomicalTwilight start (${mat.startInstantUtc}) must be strictly before eveningAstronomicalTwilight start (${eat.startInstantUtc})",
            mat.startInstantUtc.isBefore(eat.startInstantUtc)
        )
    }
}
