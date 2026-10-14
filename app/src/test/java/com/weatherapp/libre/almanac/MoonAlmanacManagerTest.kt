package com.weatherapp.libre.almanac

import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.lunar.EclipseType
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MoonAlmanacManagerTest {

    private val testLocation = LocationItem(
        id = 1L,
        name = "Ottawa",
        admin1 = "Ontario",
        country = "Canada",
        latitude = 45.4215,
        longitude = -75.6972,
        elevationMeters = 70.0
    )

    @Test
    fun testGetMoonAlmanac_RangeWithBloodMoon() {
        val startDate = LocalDate.of(2025, 3, 10)
        val daysForward = 7
        val almanac = MoonAlmanacManager.getMoonAlmanac(startDate, daysForward, testLocation, ZoneId.of("UTC"))

        assertEquals(7, almanac.size)

        val bloodMoonDay = almanac.find { it.date == LocalDate.of(2025, 3, 14) }
        assertNotNull("March 14, 2025 should be in almanac", bloodMoonDay)
        assertNotNull("March 14, 2025 should have eclipse info", bloodMoonDay!!.eclipseInfo)
        assertEquals(EclipseType.TOTAL, bloodMoonDay.eclipseInfo?.type)
        assertTrue(bloodMoonDay.isBloodMoon)
    }

    @Test
    fun testGetMoonAlmanac_NormalDays() {
        val startDate = LocalDate.of(2025, 2, 1)
        val daysForward = 5
        val almanac = MoonAlmanacManager.getMoonAlmanac(startDate, daysForward, testLocation, ZoneId.of("UTC"))

        assertEquals(5, almanac.size)
        almanac.forEach { day ->
            assertTrue(day.distanceKm > 350000.0 && day.distanceKm < 410000.0)
        }
    }
}
