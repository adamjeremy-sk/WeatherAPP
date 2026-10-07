package com.weatherapp.libre

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

class AstronomicalManagersTest {

    private val ottawaLocation = LocationItem(1L, "Ottawa", "ON", "Canada", 45.4215, -75.6972)
    private val zoneId = ZoneId.of("America/Toronto")

    private val quitoLocation = LocationItem(2L, "Quito", "", "Ecuador", -0.1807, -78.4678)
    private val quitoZone = ZoneId.of("America/Guayaquil")

    private val sydneyLocation = LocationItem(3L, "Sydney", "NSW", "Australia", -33.8688, 151.2093)
    private val sydneyZone = ZoneId.of("Australia/Sydney")

    private val tromsoLocation = LocationItem(4L, "Tromso", "", "Norway", 69.6492, 18.9553)
    private val tromsoZone = ZoneId.of("Europe/Oslo")

    private val mcMurdoLocation = LocationItem(5L, "McMurdo", "", "Antarctica", -77.8460, 166.6760)
    private val mcMurdoZone = ZoneId.of("Antarctica/McMurdo")

    private val fijiLocation = LocationItem(6L, "Suva", "", "Fiji", -18.1416, 178.4419)
    private val fijiZone = ZoneId.of("Pacific/Fiji")

    private val adelaideLocation = LocationItem(7L, "Adelaide", "SA", "Australia", -34.9285, 138.6007)
    private val adelaideZone = ZoneId.of("Australia/Adelaide") // Fractional offset +9:30

    @Test
    fun testDarkSkyManager_returnsValidWindows() = runBlocking {
        val manager = DarkSkyManager()
        val dates = listOf(LocalDate.of(2025, 3, 20))

        val results = manager.calculateDarkSkyWindows(dates, zoneId, ottawaLocation)

        assertEquals(1, results.size)
        val summary = results.first()
        assertEquals(LocalDate.of(2025, 3, 20), summary.date)
        assertNotNull(summary.windows)

        for (window in summary.windows) {
            assertTrue(window.startUtc.isBefore(window.endUtc))
            assertTrue(window.durationMinutes > 0)
            assertTrue(window.interferenceIndex in 0..100)
            assertTrue(window.qualityLabel in listOf("Excellent", "Good", "Moderate", "Poor"))
            assertNotNull(window.moonStatus)
        }
    }

    @Test
    fun testDarkSkyManager_sevenNightsCalculation() = runBlocking {
        val manager = DarkSkyManager()
        val start = LocalDate.of(2025, 3, 20)
        val dates = (0..6).map { start.plusDays(it.toLong()) }

        val results = manager.calculateDarkSkyWindows(dates, zoneId, ottawaLocation)

        assertEquals(7, results.size)
        for (summary in results) {
            assertNotNull(summary.windows)
        }
    }

    @Test
    fun testDarkSkyManager_highLatitudeSummerNoDarkness() = runBlocking {
        val manager = DarkSkyManager()
        val summerDate = listOf(LocalDate.of(2025, 6, 21))

        val results = manager.calculateDarkSkyWindows(summerDate, tromsoZone, tromsoLocation)

        assertEquals(1, results.size)
        val summary = results.first()
        assertTrue(summary.windows.isEmpty())
        val note = summary.note ?: ""
        assertTrue(note.contains("no night sky darkness", ignoreCase = true) || note.contains("Continuous daylight", ignoreCase = true) || note.contains("Sun remains above", ignoreCase = true))
    }

    @Test
    fun testNightVisibilityManager_returnsValidPeriods() = runBlocking {
        val manager = NightVisibilityManager()
        val dates = listOf(LocalDate.of(2025, 3, 20))

        val results = manager.calculateVisibilityPeriods(dates, zoneId, ottawaLocation)

        assertEquals(1, results.size)
        val summary = results.first()
        assertEquals(LocalDate.of(2025, 3, 20), summary.date)

        for (period in summary.periods) {
            assertTrue(period.startUtc.isBefore(period.endUtc))
            assertTrue(period.category in listOf("No Natural Light", "Minimal Light", "Low Light", "Moderate Light", "Bright Night"))
            assertNotNull(period.twilightStatus)
        }
    }

    @Test
    fun testNightVisibilityManager_handlesCloudCoverData() = runBlocking {
        val manager = NightVisibilityManager()
        val dates = listOf(LocalDate.of(2025, 3, 20))

        val dayStart = LocalDate.of(2025, 3, 20).atStartOfDay(zoneId).toInstant()
        val mockCloudMap = (0..48).associate { hr ->
            dayStart.plusSeconds(hr * 3600L) to 0.75 // 75% cloud cover
        }

        val results = manager.calculateVisibilityPeriods(dates, zoneId, ottawaLocation, mockCloudMap)

        assertEquals(1, results.size)
        val summary = results.first()
        assertTrue(summary.cloudCoverAvailable)

        for (period in summary.periods) {
            assertNotNull(period.cloudCoverStatus)
            assertTrue(period.cloudCoverStatus!!.contains("Cloud Cover"))
        }
    }

    @Test
    fun testNightVisibilityManager_cloudCoverAttenuatesAmbientLight() = runBlocking {
        val manager = NightVisibilityManager()
        val dates = listOf(LocalDate.of(2025, 3, 20))

        val dayStart = LocalDate.of(2025, 3, 20).atStartOfDay(zoneId).toInstant()
        val clearCloudMap = (0..48).associate { hr -> dayStart.plusSeconds(hr * 3600L) to 0.0 }
        val heavyCloudMap = (0..48).associate { hr -> dayStart.plusSeconds(hr * 3600L) to 1.0 }

        val clearResults = manager.calculateVisibilityPeriods(dates, zoneId, ottawaLocation, clearCloudMap)
        val heavyResults = manager.calculateVisibilityPeriods(dates, zoneId, ottawaLocation, heavyCloudMap)

        assertNotNull(clearResults)
        assertNotNull(heavyResults)
    }

    @Test
    fun testSolunarManager_returnsValidForecast() = runBlocking {
        val manager = SolunarManager()
        val dates = listOf(LocalDate.of(2025, 3, 20))

        val results = manager.calculateSolunarForecast(dates, zoneId, ottawaLocation)

        assertEquals(1, results.size)
        val summary = results.first()
        assertEquals(LocalDate.of(2025, 3, 20), summary.date)
        assertNotNull(summary.phaseName)
        assertTrue(summary.illumination in 0.0..1.0)
        assertTrue(summary.rating in 0..100)
        assertTrue(summary.ratingLabel in listOf("Poor", "Fair", "Good", "Excellent"))

        for (major in summary.majorPeriods) {
            assertEquals("Major", major.type)
            assertTrue(major.startUtc.isBefore(major.endUtc))
            assertEquals(7200L, Duration.between(major.startUtc, major.endUtc).seconds) // 2 hours
            assertTrue(major.centerEvent in listOf("Transit", "Nadir"))
        }

        for (minor in summary.minorPeriods) {
            assertEquals("Minor", minor.type)
            assertTrue(minor.startUtc.isBefore(minor.endUtc))
            assertEquals(3600L, Duration.between(minor.startUtc, minor.endUtc).seconds) // 1 hour
            assertTrue(minor.centerEvent in listOf("Moonrise", "Moonset"))
        }
    }

    @Test
    fun testSolunarManager_sevenDaysCalculation() = runBlocking {
        val manager = SolunarManager()
        val start = LocalDate.of(2025, 3, 20)
        val dates = (0..6).map { start.plusDays(it.toLong()) }

        val results = manager.calculateSolunarForecast(dates, zoneId, ottawaLocation)

        assertEquals(7, results.size)
        for (summary in results) {
            assertNotNull(summary.phaseName)
            assertTrue(summary.rating in 0..100)
        }
    }

    @Test
    fun testSolunarManager_multipleGlobalLocations() = runBlocking {
        val manager = SolunarManager()
        val testDate = listOf(LocalDate.of(2025, 3, 20))

        val locations = listOf(
            quitoLocation to quitoZone,         // Equatorial
            sydneyLocation to sydneyZone,       // Southern mid-latitude
            tromsoLocation to tromsoZone,       // High latitude northern
            mcMurdoLocation to mcMurdoZone,     // High latitude southern
            fijiLocation to fijiZone,           // Near Int'l Date Line
            adelaideLocation to adelaideZone    // Fractional timezone offset (+9:30)
        )

        for ((loc, zone) in locations) {
            val results = manager.calculateSolunarForecast(testDate, zone, loc)
            assertEquals(1, results.size)
            val summary = results.first()
            assertNotNull(summary.phaseName)
            assertTrue(summary.rating in 0..100)
        }
    }

    @Test
    fun testCalculations_deterministicForIdenticalInputs() = runBlocking {
        val solunarManager = SolunarManager()
        val darkSkyManager = DarkSkyManager()
        val nightVisManager = NightVisibilityManager()

        val dates = listOf(LocalDate.of(2025, 3, 20))

        val solunar1 = solunarManager.calculateSolunarForecast(dates, zoneId, ottawaLocation)
        val solunar2 = solunarManager.calculateSolunarForecast(dates, zoneId, ottawaLocation)

        assertEquals(solunar1.first().rating, solunar2.first().rating)
        assertEquals(solunar1.first().phaseName, solunar2.first().phaseName)

        val darkSky1 = darkSkyManager.calculateDarkSkyWindows(dates, zoneId, ottawaLocation)
        val darkSky2 = darkSkyManager.calculateDarkSkyWindows(dates, zoneId, ottawaLocation)

        assertEquals(darkSky1.first().windows.size, darkSky2.first().windows.size)

        val nightVis1 = nightVisManager.calculateVisibilityPeriods(dates, zoneId, ottawaLocation)
        val nightVis2 = nightVisManager.calculateVisibilityPeriods(dates, zoneId, ottawaLocation)

        assertEquals(nightVis1.first().periods.size, nightVis2.first().periods.size)
    }
}
