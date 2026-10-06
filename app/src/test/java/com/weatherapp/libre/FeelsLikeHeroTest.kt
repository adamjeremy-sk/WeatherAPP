package com.weatherapp.libre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class FeelsLikeHeroTest {

    @Test
    fun testFeelsLikeFormattingMetricPrecisionParity() {
        Locale.setDefault(Locale.US)
        val apparentTemps = listOf(0.0, 21.5, -10.3, 35.7)

        for (temp in apparentTemps) {
            val formattedTemp = UnitUtils.formatTempPrecise(temp, UnitSystem.METRIC)
            val expectedString = String.format(Locale.US, "Feels like %1\$s", formattedTemp)

            val directExpected = String.format(Locale.US, "Feels like %.1f°C", temp)
            assertEquals(expectedString, directExpected)
        }
    }

    @Test
    fun testFeelsLikeFormattingUsCustomaryPrecisionParity() {
        Locale.setDefault(Locale.US)
        val apparentTemps = listOf(0.0, 21.5, -10.0, 37.0)

        for (temp in apparentTemps) {
            val formattedTemp = UnitUtils.formatTempPrecise(temp, UnitSystem.US_CUSTOMARY)
            val expectedString = String.format(Locale.US, "Feels like %1\$s", formattedTemp)

            val fahrenheit = temp * 9 / 5 + 32
            val directExpected = String.format(Locale.US, "Feels like %.1f°F", fahrenheit)
            assertEquals(expectedString, directExpected)
        }
    }

    @Test
    fun testFeelsLikeVisibilityPredicate_nullValue_yieldsGone() {
        val showFeelsLike = WeatherDisplayManager.shouldShowFeelsLike(isToday = true, apparentTemperature = null)
        assertFalse("Null apparent temperature must yield GONE (false)", showFeelsLike)
    }

    @Test
    fun testFeelsLikeVisibilityPredicate_notToday_yieldsGone() {
        val showFeelsLike = WeatherDisplayManager.shouldShowFeelsLike(isToday = false, apparentTemperature = 21.5)
        assertFalse("Non-today selected day must yield GONE (false)", showFeelsLike)
    }

    @Test
    fun testFeelsLikeVisibilityPredicate_todayAndPresentValue_yieldsVisible() {
        val showFeelsLike = WeatherDisplayManager.shouldShowFeelsLike(isToday = true, apparentTemperature = 21.5)
        assertTrue("Today with present apparent temperature must yield VISIBLE (true)", showFeelsLike)
    }

    @Test
    fun testFeelsLikeVisibilityPredicate_todayAndValueEqualsActualTemp_yieldsVisible() {
        val actualTemp = 21.5
        val apparentTemp = 21.5
        assertEquals(actualTemp, apparentTemp, 0.001)

        val showFeelsLike = WeatherDisplayManager.shouldShowFeelsLike(isToday = true, apparentTemperature = apparentTemp)
        assertTrue("Today with apparent temp equal to actual temp must yield VISIBLE (true)", showFeelsLike)
    }
}
