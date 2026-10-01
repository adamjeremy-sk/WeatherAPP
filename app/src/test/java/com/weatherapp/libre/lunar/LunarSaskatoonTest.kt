package com.weatherapp.libre.lunar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

class LunarSaskatoonTest {
    @Test
    fun testSaskatoonRiseSet() {
        val saskatoon = Observer(
            latitudeDegrees = 52.13,
            longitudeDegrees = -106.67,
            elevationMeters = 0.0,
            temperatureCelsius = 10.0,
            pressureHpa = 1013.25
        )
        val zone = ZoneId.of("America/Regina")
        val date = LocalDate.of(2026, 10, 2)
        val events = MoonRiseSetCalculator.moonEventsForLocalDate(date, zone, saskatoon)
        
        println("Saskatoon 2026-10-02 events:")
        events.forEach {
            println("${it.type} at ${it.instantUtc.atZone(zone)} azimuth ${it.azimuthDegrees}")
        }
        
        val rise = events.firstOrNull { it.type == MoonEventType.MOONRISE }
        val set = events.firstOrNull { it.type == MoonEventType.MOONSET }
        
        // Rise: 21:53 @ 41° NE
        val expectedRise = date.atTime(21, 53).atZone(zone).toInstant()
        val expectedSet = date.atTime(15, 13).atZone(zone).toInstant()
        
        assertTrue(abs(rise!!.instantUtc.epochSecond - expectedRise.epochSecond) <= 60)
        assertTrue(abs(set!!.instantUtc.epochSecond - expectedSet.epochSecond) <= 60)
        
        assertEquals(41.0, rise.azimuthDegrees, 1.0)
        assertEquals(319.0, set.azimuthDegrees, 1.0)
    }
}
