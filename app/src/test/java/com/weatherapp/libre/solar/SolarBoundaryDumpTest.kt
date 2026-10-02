package com.weatherapp.libre.solar

import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SolarBoundaryDumpTest {

    @Test
    fun dumpTromsoSept17() {
        val date = LocalDate.of(2026, 9, 17)
        val zoneId = ZoneId.of("Europe/Oslo")
        val observer = Observer(latitudeDegrees = 69.65, longitudeDegrees = 18.96)

        val result = SolarLightingEngine.calculateSolarLightingPhases(date, zoneId, observer)
        val details = SolarLightingEngine.calculateChronologicalSolarPhases(date, zoneId, observer)

        println("=== TROMSO SEPT 17 ===")
        println("Solar Noon: ${result.solarNoon?.instantUtc?.atZone(zoneId)}")
        println("Morning Astro: ${result.astronomicalTwilight?.startInstantUtc?.atZone(zoneId)} to ${result.astronomicalTwilight?.endInstantUtc?.atZone(zoneId)}")
        println("Evening Astro: ${result.eveningAstronomicalTwilight?.startInstantUtc?.atZone(zoneId)} to ${result.eveningAstronomicalTwilight?.endInstantUtc?.atZone(zoneId)}")

        val samples = mutableListOf<SolarPositionEngine.TopocentricSolarPosition>()
        val start = date.atStartOfDay(zoneId).toInstant()
        for (i in -24..24) {
            val inst = start.plusSeconds(i.toLong() * 3600)
            val pos = SolarPositionEngine.computeTopocentric(inst, observer)
            println("Hour $i: ${pos.apparentAltitudeDegrees}")
        }
    }
}
