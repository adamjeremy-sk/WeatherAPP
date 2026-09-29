package com.weatherapp.libre.solar

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SolarLightingEngineTest {

    @Test
    fun testEquatorAtEquinox() {
        val quitoObserver = Observer(
            latitudeDegrees = -0.1807,
            longitudeDegrees = -78.4678,
            elevationMeters = 2850.0
        )
        val localDate = LocalDate.of(2026, 3, 20) // Vernal Equinox
        val zoneId = ZoneId.of("America/Guayaquil")

        val result = SolarLightingEngine.calculateSolarLightingPhases(localDate, zoneId, quitoObserver)

        assertNotNull(result.solarNoon)
        assertNotNull(result.sunrise)
        assertNotNull(result.sunset)

        // Sunrise should be near 06:00 local time
        val sunriseZdt = result.sunrise!!.instantUtc.atZone(zoneId)
        assertEquals(6, sunriseZdt.hour)

        // Sunset should be near 18:00 local time
        val sunsetZdt = result.sunset!!.instantUtc.atZone(zoneId)
        assertEquals(18, sunsetZdt.hour)

        // Azimuths: Sunrise near East (90°), Sunset near West (270°)
        assertTrue(result.sunrise!!.azimuthDegrees in 80.0..100.0)
        assertTrue(result.sunset!!.azimuthDegrees in 260.0..280.0)
    }

    @Test
    fun testHighLatitudeSummer() {
        val reykjavikObserver = Observer(
            latitudeDegrees = 64.1466,
            longitudeDegrees = -21.9426
        )
        val localDate = LocalDate.of(2026, 6, 21) // Summer Solstice (Midnight Sun)
        val zoneId = ZoneId.of("Atlantic/Reykjavik")

        val result = SolarLightingEngine.calculateSolarLightingPhases(localDate, zoneId, reykjavikObserver)

        // In Reykjavik on summer solstice, Sun elevation stays above horizon (+2.4°) all night (Midnight Sun)
        assertNull(result.sunrise)
        assertNull(result.sunset)
        assertNull(result.civilTwilight)
        assertNull(result.astronomicalTwilight)
    }

    @Test
    fun testHighElevationHorizonDip() {
        val seaLevelObserver = Observer(
            latitudeDegrees = 27.9881,
            longitudeDegrees = 86.9250,
            elevationMeters = 0.0
        )
        val everestObserver = Observer(
            latitudeDegrees = 27.9881,
            longitudeDegrees = 86.9250,
            elevationMeters = 8848.0
        )
        val localDate = LocalDate.of(2026, 3, 20)
        val zoneId = ZoneId.of("Asia/Kathmandu")

        val resSea = SolarLightingEngine.calculateSolarLightingPhases(localDate, zoneId, seaLevelObserver)
        val resEverest = SolarLightingEngine.calculateSolarLightingPhases(localDate, zoneId, everestObserver)

        assertNotNull(resSea.sunrise)
        assertNotNull(resEverest.sunrise)

        // At 8848m, horizon dip is ~1.65° so sunrise happens earlier on Everest than at sea level
        assertTrue(resEverest.sunrise!!.instantUtc.isBefore(resSea.sunrise!!.instantUtc))
        // And sunset happens later on Everest than at sea level
        assertTrue(resEverest.sunset!!.instantUtc.isAfter(resSea.sunset!!.instantUtc))
    }

    @Test
    fun testExtremeColdPressureRefraction() {
        val stdObserver = Observer(
            latitudeDegrees = -77.85,
            longitudeDegrees = 166.67,
            temperatureCelsius = 10.0,
            pressureHpa = 1013.25
        )
        val antarcticaObserver = Observer(
            latitudeDegrees = -77.85,
            longitudeDegrees = 166.67,
            temperatureCelsius = -40.0,
            pressureHpa = 1030.0
        )
        val localDate = LocalDate.of(2026, 10, 20)
        val zoneId = ZoneId.of("Antarctica/McMurdo")

        val resStd = SolarLightingEngine.calculateSolarLightingPhases(localDate, zoneId, stdObserver)
        val resAnt = SolarLightingEngine.calculateSolarLightingPhases(localDate, zoneId, antarcticaObserver)

        assertNotNull(resStd.sunrise)
        assertNotNull(resAnt.sunrise)

        // Extreme cold increases atmospheric density & refraction, making Sun appear earlier
        assertTrue(resAnt.sunrise!!.instantUtc.isBefore(resStd.sunrise!!.instantUtc))
    }

    @Test
    fun testSouthernHemisphereAzimuths() {
        val aucklandObserver = Observer(
            latitudeDegrees = -36.8485,
            longitudeDegrees = 174.7633
        )
        val localDate = LocalDate.of(2026, 9, 22)
        val zoneId = ZoneId.of("Pacific/Auckland")

        val result = SolarLightingEngine.calculateSolarLightingPhases(localDate, zoneId, aucklandObserver)

        assertNotNull(result.solarNoon)
        assertNotNull(result.sunrise)
        assertNotNull(result.sunset)

        // In southern hemisphere, Solar Noon occurs in the NORTH (azimuth ~ 0° / 360°)
        val noonAz = result.solarNoon!!.azimuthDegrees
        assertTrue(noonAz < 30.0 || noonAz > 330.0)
    }
}
