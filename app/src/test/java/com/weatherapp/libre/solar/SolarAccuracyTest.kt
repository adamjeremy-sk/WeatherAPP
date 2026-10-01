package com.weatherapp.libre.solar

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class SolarAccuracyTest {

    @Test
    fun testSaskatoonAccuracyAtEquinox() {
        // Saskatoon: 52.1332 N, 106.6700 W, Elevation 481m
        val saskatoonObserver = Observer(
            latitudeDegrees = 52.1332,
            longitudeDegrees = -106.6700,
            elevationMeters = 481.0
        )
        val date = LocalDate.of(2026, 9, 29)
        val zoneId = ZoneId.of("America/Regina")

        val result = SolarLightingEngine.calculateSolarLightingPhases(date, zoneId, saskatoonObserver)

        assertNotNull(result.sunrise)
        assertNotNull(result.sunset)

        val sunriseZdt = result.sunrise!!.instantUtc.atZone(zoneId)
        val sunsetZdt = result.sunset!!.instantUtc.atZone(zoneId)

        // Time & Date reference for Saskatoon on Sep 29, 2026 (CST / UTC-6):
        // Sunrise ~ 06:55 AM local time
        // Sunset ~ 18:57 PM local time
        val sunriseDiffMins = abs(ChronoUnit.MINUTES.between(
            sunriseZdt.toLocalTime(),
            java.time.LocalTime.of(6, 55)
        ))
        val sunsetDiffMins = abs(ChronoUnit.MINUTES.between(
            sunsetZdt.toLocalTime(),
            java.time.LocalTime.of(18, 57)
        ))

        assertTrue("Sunrise $sunriseZdt should be within 1 min of 06:55", sunriseDiffMins <= 1)
        assertTrue("Sunset $sunsetZdt should be within 1 min of 18:57", sunsetDiffMins <= 1)
    }

    @Test
    fun testSeaLevelHalifaxAccuracy() {
        // Halifax: 44.6488 N, 63.5752 W, Elevation 0m
        val halifaxObserver = Observer(
            latitudeDegrees = 44.6488,
            longitudeDegrees = -63.5752,
            elevationMeters = 0.0
        )
        val date = LocalDate.of(2026, 9, 29)
        val zoneId = ZoneId.of("America/Halifax")

        val result = SolarLightingEngine.calculateSolarLightingPhases(date, zoneId, halifaxObserver)

        assertNotNull(result.sunrise)
        assertNotNull(result.sunset)

        val sunriseZdt = result.sunrise!!.instantUtc.atZone(zoneId)
        val sunsetZdt = result.sunset!!.instantUtc.atZone(zoneId)

        // Halifax Sep 29, 2026 (ADT / UTC-3) reference: Sunrise ~07:05 AM, Sunset ~ 19:02 PM
        val sunriseDiffMins = abs(ChronoUnit.MINUTES.between(
            sunriseZdt.toLocalTime(),
            java.time.LocalTime.of(7, 5)
        ))
        val sunsetDiffMins = abs(ChronoUnit.MINUTES.between(
            sunsetZdt.toLocalTime(),
            java.time.LocalTime.of(19, 2)
        ))

        assertTrue("Halifax Sunrise $sunriseZdt should be within 1 min of 07:05", sunriseDiffMins <= 1)
        assertTrue("Halifax Sunset $sunsetZdt should be within 1 min of 19:02", sunsetDiffMins <= 1)
    }

    @Test
    fun testHighElevationDenverHorizonDip() {
        // Denver: 39.7392 N, 104.9903 W
        val seaLevelDenver = Observer(latitudeDegrees = 39.7392, longitudeDegrees = -104.9903, elevationMeters = 0.0)
        val denverObserver = Observer(latitudeDegrees = 39.7392, longitudeDegrees = -104.9903, elevationMeters = 1609.0)

        val date = LocalDate.of(2026, 9, 29)
        val zoneId = ZoneId.of("America/Denver")

        val resSea = SolarLightingEngine.calculateSolarLightingPhases(date, zoneId, seaLevelDenver)
        val resDenver = SolarLightingEngine.calculateSolarLightingPhases(date, zoneId, denverObserver)

        assertNotNull(resSea.sunrise)
        assertNotNull(resDenver.sunrise)

        // Elevation ~1600m increases daylight duration (sunrise earlier, sunset later)
        val seaDaylightMins = ChronoUnit.MINUTES.between(resSea.sunrise!!.instantUtc, resSea.sunset!!.instantUtc)
        val denverDaylightMins = ChronoUnit.MINUTES.between(resDenver.sunrise!!.instantUtc, resDenver.sunset!!.instantUtc)

        assertTrue("Denver daylight at 1609m ($denverDaylightMins m) must be longer than sea level ($seaDaylightMins m)", denverDaylightMins > seaDaylightMins)
    }

    @Test
    fun testGoldenAndBlueHourContinuity() {
        val observer = Observer(latitudeDegrees = 52.1332, longitudeDegrees = -106.6700, elevationMeters = 481.0)
        val date = LocalDate.of(2026, 9, 29)
        val zoneId = ZoneId.of("America/Regina")

        val result = SolarLightingEngine.calculateSolarLightingPhases(date, zoneId, observer)

        val morningBlue = result.morningBlueHour
        val morningGolden = result.morningGoldenHour

        assertNotNull(morningBlue)
        assertNotNull(morningGolden)

        // Morning Blue Hour ends at -4.0° and Morning Golden Hour starts at -4.0° (same instant)
        val diffSecs = abs(ChronoUnit.SECONDS.between(morningBlue!!.endInstantUtc, morningGolden!!.startInstantUtc))
        assertTrue("Blue Hour end and Golden Hour start must transition seamlessly (diff $diffSecs s)", diffSecs <= 1)
    }
}
