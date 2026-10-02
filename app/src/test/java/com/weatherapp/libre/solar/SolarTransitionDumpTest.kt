package com.weatherapp.libre.solar

import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SolarTransitionDumpTest {

    @Test
    fun dumpTromsoSeptTransition() {
        val observer = Observer(69.65, 18.96)
        val zoneId = ZoneId.of("Europe/Oslo")
        var date = LocalDate.of(2026, 9, 10)
        val endDate = LocalDate.of(2026, 9, 30)

        while (!date.isAfter(endDate)) {
            val lighting = SolarLightingEngine.calculateSolarLightingPhases(date, zoneId, observer)
            
            val mc = lighting.civilTwilight != null
            val ec = lighting.eveningCivilTwilight != null
            val mn = lighting.nauticalTwilight != null
            val en = lighting.eveningNauticalTwilight != null
            val ma = lighting.astronomicalTwilight != null
            val ea = lighting.eveningAstronomicalTwilight != null

            println("Date: $date | MC: $mc | EC: $ec | MN: $mn | EN: $en | MA: $ma | EA: $ea")
            
            if (date.dayOfMonth == 17) {
                println("Sept 17 MA: ${lighting.astronomicalTwilight}")
                println("Sept 17 EA: ${lighting.eveningAstronomicalTwilight}")
            }
            
            date = date.plusDays(1)
        }
    }
}
