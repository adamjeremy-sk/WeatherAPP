package com.weatherapp.libre

import com.weatherapp.libre.solar.Observer
import com.weatherapp.libre.solar.SolarLightingEngine
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.ZoneId

class SolarLightingEngineTest {
    @Test
    fun testSolarLightingEngineCalculation() {
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        val observer = Observer(45.4215, -75.6972)

        val lighting = SolarLightingEngine.calculateSolarLightingPhases(today, zoneId, observer)

        assertNotNull(lighting.sunrise)
        assertNotNull(lighting.sunset)
        assertTrue(lighting.sunrise!!.azimuthDegrees in 0.0..360.0)
        assertTrue(lighting.sunset!!.azimuthDegrees in 0.0..360.0)
    }
}
