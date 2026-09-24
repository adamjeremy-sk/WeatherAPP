package com.weatherapp.libre

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.ZoneId

class SolarCalculatorTest {
    @Test
    fun testSolarTimesCalculation() {
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        val solar = SolarCalculator.calculateSolarTimes(45.4215, -75.6972, today, zoneId)

        assertNotNull(solar.sunrise)
        assertNotNull(solar.sunset)
        assertTrue(solar.sunriseAzimuthDegrees in 0..360)
        assertTrue(solar.sunsetAzimuthDegrees in 0..360)
    }
}
