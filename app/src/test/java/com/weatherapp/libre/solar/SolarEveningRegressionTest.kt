package com.weatherapp.libre.solar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter

class SolarEveningRegressionTest {

    @Test
    fun testParisOracle() {
        val date = LocalDate.of(2026, 10, 3)
        val observer = Observer(48.8566, 2.3522)
        val zoneId = ZoneId.of("Europe/Paris")
        val result = SolarRiseSetSolver.solve(date, zoneId, observer)
        
        val expSrSec = date.atTime(7, 52).atZone(zoneId).toInstant().epochSecond
        val expSsSec = date.atTime(19, 27).atZone(zoneId).toInstant().epochSecond
        
        val srSec = result.sunrise!!.instantUtc.epochSecond
        val ssSec = result.sunset!!.instantUtc.epochSecond
        
        val srDiff = Math.abs(srSec - expSrSec) / 60
        val ssDiff = Math.abs(ssSec - expSsSec) / 60
        
        assertTrue("Sunrise off by $srDiff mins", srDiff <= 1)
        assertTrue("Sunset off by $ssDiff mins", ssDiff <= 1)
    }

    @org.junit.Ignore("TODO: Needs an independent almanac source (e.g. timeanddate.com) to provide the true expected values before re-enabling.")
    @Test
    fun testSaskatoonDec23Oracle() {
        val date = LocalDate.of(2026, 12, 23)
        val zoneId = ZoneId.of("America/Regina")
        val observer = Observer(latitudeDegrees = 52.13, longitudeDegrees = -106.67)

        val details = SolarLightingEngine.calculateChronologicalSolarPhases(date, zoneId, observer, is24Hour = true)

        // Row 9: Golden Hour
        val r9 = details[8]
        assertEquals("Golden Hour", r9.name)

        // Row 10: Sunset
        val r10 = details[9]
        assertEquals("Sunset", r10.name)

        // Row 11: Civil Twilight
        val r11 = details[10]
        assertEquals("Civil Twilight", r11.name)

        // Row 12: Blue Hour
        val r12 = details[11]
        assertEquals("Blue Hour", r12.name)

        // Row 13: Nautical Twilight
        val r13 = details[12]
        assertEquals("Nautical Twilight", r13.name)

        // Row 14: Astronomical Twilight
        val r14 = details[13]
        assertEquals("Astronomical Twilight", r14.name)

        // Row 15: Night
        val r15 = details[14]
        assertEquals("Night", r15.name)
    }

    @Test
    fun testFullYearSweepMirrorResidualsAndChainContinuity() {
        val locations = listOf(
            Triple("Saskatoon", Observer(52.13, -106.67), ZoneId.of("America/Regina")),
            Triple("Tromso", Observer(69.65, 18.96), ZoneId.of("Europe/Oslo")),
            Triple("Wellington", Observer(-41.29, 174.78), ZoneId.of("Pacific/Auckland"))
        )

        for ((name, observer, zoneId) in locations) {
            // Mirror symmetry degrades with latitude and declination change rate, so this sweep uses a generous tolerance
            val maxAllowedMins = if (Math.abs(observer.latitudeDegrees) > 60.0) 35 else 5
            var date = LocalDate.of(2026, 1, 1)
            val endDate = LocalDate.of(2026, 12, 31)

            while (!date.isAfter(endDate)) {
                val localStart = date.atStartOfDay(zoneId).toInstant()
                val localEnd = date.plusDays(1).atStartOfDay(zoneId).toInstant()

                val lighting = SolarLightingEngine.calculateSolarLightingPhases(date, zoneId, observer)
                val noon = lighting.solarNoon?.instantUtc

                if (noon != null) {
                    val noonSec = noon.epochSecond

                    fun inDay(phase: SolarPhase?): Boolean {
                        if (phase == null) return false
                        val s = phase.startInstantUtc
                        val e = phase.endInstantUtc
                        return !s.isBefore(localStart) && s.isBefore(localEnd) && !e.isBefore(localStart) && !e.isAfter(localEnd)
                    }

                    // 1. Sunrise / Sunset symmetry
                    val sr = lighting.sunrise?.instantUtc
                    val ss = lighting.sunset?.instantUtc
                    if (sr != null && ss != null &&
                        !sr.isBefore(localStart) && sr.isBefore(localEnd) &&
                        !ss.isBefore(localStart) && ss.isBefore(localEnd)) {
                        val dayMins = ChronoUnit.MINUTES.between(sr, ss)
                        if (dayMins in 240..1200) {
                            val expectedSsSec = 2 * noonSec - sr.epochSecond
                            val diffMins = Math.abs(ss.epochSecond - expectedSsSec) / 60
                            assertTrue("[$name $date] Sunset mirror residual $diffMins mins > $maxAllowedMins mins", diffMins <= maxAllowedMins)
                        }
                    }

                    // 2. Evening Golden Hour
                    val mg = lighting.morningGoldenHour
                    val eg = lighting.eveningGoldenHour
                    if (inDay(mg) && inDay(eg)) {
                        val expEgStart = 2 * noonSec - mg!!.endInstantUtc.epochSecond
                        val expEgEnd = 2 * noonSec - mg.startInstantUtc.epochSecond
                        val diffStart = Math.abs(eg!!.startInstantUtc.epochSecond - expEgStart) / 60
                        val diffEnd = Math.abs(eg.endInstantUtc.epochSecond - expEgEnd) / 60
                        assertTrue("[$name $date] Evening Golden start mirror residual $diffStart mins > $maxAllowedMins mins", diffStart <= maxAllowedMins)
                        assertTrue("[$name $date] Evening Golden end mirror residual $diffEnd mins > $maxAllowedMins mins", diffEnd <= maxAllowedMins)

                        // Duration equality
                        val mgDur = ChronoUnit.MINUTES.between(mg.startInstantUtc, mg.endInstantUtc)
                        val egDur = ChronoUnit.MINUTES.between(eg.startInstantUtc, eg.endInstantUtc)
                        val durDiff = Math.abs(mgDur - egDur)
                        assertTrue("[$name $date] Golden hour duration diff $durDiff mins > $maxAllowedMins mins", durDiff <= maxAllowedMins)
                    }

                    // 3. Evening Civil Twilight
                    val mc = lighting.civilTwilight
                    val ec = lighting.eveningCivilTwilight
                    if (inDay(mc) && inDay(ec)) {
                        val expEcStart = 2 * noonSec - mc!!.endInstantUtc.epochSecond
                        val expEcEnd = 2 * noonSec - mc.startInstantUtc.epochSecond
                        val diffStart = Math.abs(ec!!.startInstantUtc.epochSecond - expEcStart) / 60
                        val diffEnd = Math.abs(ec.endInstantUtc.epochSecond - expEcEnd) / 60
                        assertTrue("[$name $date] Evening Civil start mirror residual $diffStart mins > $maxAllowedMins mins", diffStart <= maxAllowedMins)
                        assertTrue("[$name $date] Evening Civil end mirror residual $diffEnd mins > $maxAllowedMins mins", diffEnd <= maxAllowedMins)

                        val mcDur = ChronoUnit.MINUTES.between(mc.startInstantUtc, mc.endInstantUtc)
                        val ecDur = ChronoUnit.MINUTES.between(ec.startInstantUtc, ec.endInstantUtc)
                        val durDiff = Math.abs(mcDur - ecDur)
                        assertTrue("[$name $date] Civil twilight duration diff $durDiff mins > $maxAllowedMins mins", durDiff <= maxAllowedMins)
                    }

                    // 4. Evening Nautical Twilight
                    val mn = lighting.nauticalTwilight
                    val en = lighting.eveningNauticalTwilight
                    if (inDay(mn) && inDay(en)) {
                        val expEnStart = 2 * noonSec - mn!!.endInstantUtc.epochSecond
                        val expEnEnd = 2 * noonSec - mn.startInstantUtc.epochSecond
                        val diffStart = Math.abs(en!!.startInstantUtc.epochSecond - expEnStart) / 60
                        val diffEnd = Math.abs(en.endInstantUtc.epochSecond - expEnEnd) / 60
                        assertTrue("[$name $date] Evening Nautical start mirror residual $diffStart mins > $maxAllowedMins mins", diffStart <= maxAllowedMins)
                        assertTrue("[$name $date] Evening Nautical end mirror residual $diffEnd mins > $maxAllowedMins mins", diffEnd <= maxAllowedMins)

                        val mnDur = ChronoUnit.MINUTES.between(mn.startInstantUtc, mn.endInstantUtc)
                        val enDur = ChronoUnit.MINUTES.between(en.startInstantUtc, en.endInstantUtc)
                        val durDiff = Math.abs(mnDur - enDur)
                        assertTrue("[$name $date] Nautical twilight duration diff $durDiff mins > $maxAllowedMins mins", durDiff <= maxAllowedMins)
                    }

                    // 5. Evening Astronomical Twilight
                    val ma = lighting.astronomicalTwilight
                    val ea = lighting.eveningAstronomicalTwilight
                    if (inDay(ma) && inDay(ea)) {
                        val expEaStart = 2 * noonSec - ma!!.endInstantUtc.epochSecond
                        val expEaEnd = 2 * noonSec - ma.startInstantUtc.epochSecond
                        val diffStart = Math.abs(ea!!.startInstantUtc.epochSecond - expEaStart) / 60
                        val diffEnd = Math.abs(ea.endInstantUtc.epochSecond - expEaEnd) / 60
                        assertTrue("[$name $date] Evening Astronomical start mirror residual $diffStart mins > $maxAllowedMins mins", diffStart <= maxAllowedMins)
                        assertTrue("[$name $date] Evening Astronomical end mirror residual $diffEnd mins > $maxAllowedMins mins", diffEnd <= maxAllowedMins)

                        val maDur = ChronoUnit.MINUTES.between(ma.startInstantUtc, ma.endInstantUtc)
                        val eaDur = ChronoUnit.MINUTES.between(ea.startInstantUtc, ea.endInstantUtc)
                        val durDiff = Math.abs(maDur - eaDur)
                        assertTrue("[$name $date] Astronomical twilight duration diff $durDiff mins > $maxAllowedMins mins", durDiff <= maxAllowedMins)
                    }
                }

                // Verify Core Twilight Chain Continuity & No Band Spans Midnight
                val details = SolarLightingEngine.calculateChronologicalSolarPhases(date, zoneId, observer)
                if (details.size > 1) { // Skip polar single-row
                    val rowMap = details.associateBy { it.stepIndex }

                    // Core Evening Twilight Chain: Sunset (Row 10) -> Evening Civil (Row 11) -> Evening Nautical (Row 13) -> Evening Astro (Row 14) -> Evening Night (Row 15)
                    val r10 = rowMap[10] // Sunset
                    val r11 = rowMap[11] // Evening Civil
                    val r13 = rowMap[13] // Evening Nautical
                    val r14 = rowMap[14] // Evening Astro
                    val r15 = rowMap[15] // Evening Night

                    if (r10 != null && r11 != null && r13 != null && r14 != null && r15 != null) {
                        assertEquals("[$name $date] Row 10 Sunset != Row 11 Civil start", r10.startInstant, r11.startInstant)
                        assertEquals("[$name $date] Row 11 Civil end != Row 13 Nautical start", r11.endInstant, r13.startInstant)
                        assertEquals("[$name $date] Row 13 Nautical end != Row 14 Astro start", r13.endInstant, r14.startInstant)
                        assertEquals("[$name $date] Row 14 Astro end != Row 15 Night start", r14.endInstant, r15.startInstant)
                    }

                    // No twilight band spans midnight
                    for (cur in details) {
                        if (cur.startInstant != null && cur.endInstant != null && cur.name != "Night" && cur.name != "Daytime") {
                            val startLocalDate = cur.startInstant.atZone(zoneId).toLocalDate()
                            val endLocalDate = cur.endInstant.atZone(zoneId).toLocalDate()
                            assertEquals("[$name $date] Phase ${cur.name} spans midnight ($startLocalDate to $endLocalDate)", date, startLocalDate)
                            assertEquals("[$name $date] Phase ${cur.name} spans midnight ($startLocalDate to $endLocalDate)", date, endLocalDate)
                        }
                    }
                }

                date = date.plusDays(1)
            }
        }
    }
}
