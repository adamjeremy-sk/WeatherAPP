package com.weatherapp.libre.solar

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SolarGeometricOracleTest {

    @Test
    fun testParisOracleAndFourCityDump() {
        val date = LocalDate.of(2026, 10, 3)
        val parisObserver = Observer(48.8566, 2.3522)
        val parisZoneId = ZoneId.of("Europe/Paris")
        
        val parisResult = SolarRiseSetSolver.solve(date, parisZoneId, parisObserver)
        
        // Target: 07:52 local
        val expSrSec = date.atTime(7, 52).atZone(parisZoneId).toInstant().epochSecond
        // Target: 19:27 local
        val expSsSec = date.atTime(19, 27).atZone(parisZoneId).toInstant().epochSecond
        
        val srSec = parisResult.sunrise!!.instantUtc.epochSecond
        val ssSec = parisResult.sunset!!.instantUtc.epochSecond
        
        val srDiff = Math.abs(srSec - expSrSec) / 60
        val ssDiff = Math.abs(ssSec - expSsSec) / 60
        
        println("Paris Sunrise Diff: $srDiff mins, Sunset Diff: $ssDiff mins")
        
        val fmt = DateTimeFormatter.ofPattern("HH:mm:ss")
        val cities = listOf(
            Triple("Paris", Observer(48.8566, 2.3522), ZoneId.of("Europe/Paris")),
            Triple("Saskatoon", Observer(52.13, -106.67), ZoneId.of("America/Regina")),
            Triple("Tromso", Observer(69.65, 18.96), ZoneId.of("Europe/Oslo")),
            Triple("Wellington", Observer(-41.29, 174.78), ZoneId.of("Pacific/Auckland"))
        )
        
        for ((name, obs, zone) in cities) {
            val result = SolarRiseSetSolver.solve(date, zone, obs)
            val sr = result.sunrise?.instantUtc?.atZone(zone)?.format(fmt) ?: "None"
            val ss = result.sunset?.instantUtc?.atZone(zone)?.format(fmt) ?: "None"
            println("$name: Sunrise $sr, Sunset $ss")
        }
        
        val details = SolarLightingEngine.calculateChronologicalSolarPhases(date, parisZoneId, parisObserver)
        println("\nParis Boundaries:")
        for (d in details) {
            println("${d.stepIndex} ${d.name}: ${d.timeRangeText}")
        }
        
        assertTrue("Sunrise off by $srDiff mins", srDiff <= 2)
        assertTrue("Sunset off by $ssDiff mins", ssDiff <= 2)
    }
}
