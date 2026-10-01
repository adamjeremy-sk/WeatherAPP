package com.weatherapp.libre.lunar

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

class LunarAzimuthSanityTest {
    @Test
    fun testAzimuthSanity() {
        val observer = Observer(52.13, -106.67, 0.0, 10.0, 1013.25)
        val zone = ZoneId.of("America/Regina")
        
        val date = LocalDate.of(2026, 10, 2)
        val events = MoonRiseSetCalculator.moonEventsForLocalDate(date, zone, observer)
        
        val rise = events.first { it.type == MoonEventType.MOONRISE }
        val set = events.first { it.type == MoonEventType.MOONSET }
        
        val mirrorRise = 360.0 - rise.azimuthDegrees
        val diff = abs(mirrorRise - set.azimuthDegrees)
        println("Rise azimuth: ${rise.azimuthDegrees}, Set azimuth: ${set.azimuthDegrees}, Diff: $diff")
        assertTrue("Azimuths are not mirrors: $diff", diff <= 2.0)
    }
}
