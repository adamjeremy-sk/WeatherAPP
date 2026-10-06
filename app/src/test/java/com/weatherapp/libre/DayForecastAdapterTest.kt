package com.weatherapp.libre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DayForecastAdapterTest {

    @Test
    fun testJoinTwoLines_standardSunPosition() {
        val riseLine = "Rise 98° (E)"
        val setLine = "Set 261° (W)"
        val result = DayForecastAdapter.joinTwoLines(riseLine, setLine)

        assertEquals(1, result.count { it == '\n' })
        assertTrue(result.startsWith("Rise 98° (E)"))
        assertTrue(result.endsWith("Set 261° (W)"))
        assertEquals("Rise 98° (E)\nSet 261° (W)", result)
        assertFalse(result.contains("•"))
    }

    @Test
    fun testJoinTwoLines_standardMoonPosition() {
        val riseLine = "Rise 3:59 AM (73° E)"
        val setLine = "Set 5:51 PM (280° W)"
        val result = DayForecastAdapter.joinTwoLines(riseLine, setLine)

        assertEquals(1, result.count { it == '\n' })
        assertTrue(result.startsWith("Rise 3:59 AM (73° E)"))
        assertTrue(result.endsWith("Set 5:51 PM (280° W)"))
        assertEquals("Rise 3:59 AM (73° E)\nSet 5:51 PM (280° W)", result)
        assertFalse(result.contains("•"))
    }

    @Test
    fun testJoinTwoLines_noRiseInput() {
        val riseLine = "No Rise"
        val setLine = "Set 5:51 PM (280° W)"
        val result = DayForecastAdapter.joinTwoLines(riseLine, setLine)

        assertEquals(1, result.count { it == '\n' })
        assertTrue(result.startsWith("No Rise"))
        assertTrue(result.endsWith("Set 5:51 PM (280° W)"))
        assertEquals("No Rise\nSet 5:51 PM (280° W)", result)
        assertFalse(result.contains("•"))
    }

    @Test
    fun testJoinTwoLines_noSetInput() {
        val riseLine = "Rise 3:59 AM (73° E)"
        val setLine = "No Set"
        val result = DayForecastAdapter.joinTwoLines(riseLine, setLine)

        assertEquals(1, result.count { it == '\n' })
        assertTrue(result.startsWith("Rise 3:59 AM (73° E)"))
        assertTrue(result.endsWith("No Set"))
        assertEquals("Rise 3:59 AM (73° E)\nNo Set", result)
        assertFalse(result.contains("•"))
    }
}
