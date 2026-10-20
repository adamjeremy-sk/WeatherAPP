package com.weatherapp.libre.almanac

import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.lunar.EclipseType
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class LunarCalendarManagerTest {

    private val testLocation = LocationItem(
        id = 1L,
        name = "Ottawa",
        admin1 = "Ontario",
        country = "Canada",
        latitude = 45.4215,
        longitude = -75.6972,
        elevationMeters = 70.0,
    )

    @Test
    fun testGetMonthlyForecast_March2025() {
        val forecast = LunarCalendarManager.getMonthlyForecast(
            year = 2025,
            month = 3,
            location = testLocation,
            hourlyCloudCoverMap = null,
            zoneId = ZoneId.of("UTC"),
        )

        assertEquals(31, forecast.size)

        val march14 = forecast.find { it.date == LocalDate.of(2025, 3, 14) }
        assertNotNull("March 14, 2025 should be in forecast", march14)
        assertNotNull("March 14, 2025 should have eclipse info", march14!!.eclipseInfo)
        assertEquals(EclipseType.TOTAL, march14.eclipseInfo?.type)
        assertTrue(march14.isBloodMoon)

        forecast.forEach { day ->
            assertNotNull(day.date)
            assertTrue(day.distanceKm > 350000.0 && day.distanceKm < 410000.0)
            assertNotNull(day.deepSkyScore)
            assertTrue(day.deepSkyScore!! in 0.0..100.0)
            assertNotNull(day.lunarViewingScore)
            assertTrue(day.lunarViewingScore!! in 0.0..100.0)
        }
    }

    @Test
    fun testGetWeeklyForecast_March2025() {
        val sundayDate = LocalDate.of(2025, 3, 9)
        val forecast = LunarCalendarManager.getWeeklyForecast(
            startDate = sundayDate,
            location = testLocation,
            hourlyCloudCoverMap = null,
            zoneId = ZoneId.of("UTC"),
        )

        assertEquals(7, forecast.size)
        assertEquals(sundayDate, forecast[0].date)
        assertEquals(sundayDate.plusDays(6), forecast[6].date)

        val march14 = forecast.find { it.date == LocalDate.of(2025, 3, 14) }
        assertNotNull("March 14, 2025 should be in week forecast", march14)
        assertEquals(EclipseType.TOTAL, march14!!.eclipseInfo?.type)
        assertTrue(march14.isBloodMoon)
    }
}
