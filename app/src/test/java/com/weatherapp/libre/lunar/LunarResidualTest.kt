package com.weatherapp.libre.lunar

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

class LunarResidualTest {
    @Test
    fun testResiduals() {
        val cities = listOf(
            Observer(52.13, -106.67, 0.0, 10.0, 1013.25) to ZoneId.of("America/Regina"), // Saskatoon
            Observer(69.65, 18.96, 0.0, 10.0, 1013.25) to ZoneId.of("Europe/Oslo"),     // Tromso
            Observer(-41.29, 174.78, 0.0, 10.0, 1013.25) to ZoneId.of("Pacific/Auckland") // Wellington
        )
        
        val startDate = LocalDate.of(2026, 10, 1)
        
        for ((observer, zone) in cities) {
            for (i in 0 until 30) {
                val date = startDate.plusDays(i.toLong())
                val events = MoonRiseSetCalculator.moonEventsForLocalDate(date, zone, observer)
                for (event in events) {
                    val jdUtc = AstroTime.instantToJdUtc(event.instantUtc)
                    val jdeTT = AstroTime.jdUtcToJdeTT(jdUtc)
                    val lunar = LunarPosition.compute(jdeTT)
                    val topo = TopocentricConverter.computeTopocentric(lunar, observer, jdUtc, jdeTT)
                    val residual = topo.apparentAltitudeDegrees - (-lunar.semidiameterDeg)
                    
                    if (abs(residual) > 0.005) {
                        println("FAIL: residual $residual for $event in ${zone.id}")
                    }
                    assertTrue("Residual $residual > 0.005", abs(residual) <= 0.005)
                }
            }
        }
    }
}
