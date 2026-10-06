package com.weatherapp.libre

import com.weatherapp.libre.lunar.LunarCalculator
import com.weatherapp.libre.lunar.MoonPhaseInfo
import com.weatherapp.libre.lunar.MoonPhaseName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MetricCellTest {

    @Test
    fun testDaylightCellModel_secondaryIsNull() {
        val daylightCell = MetricCell(
            type = MetricType.DAYLIGHT,
            title = "Sunlight Duration",
            value = "11h 49m",
            secondary = null,
            iconResId = R.drawable.ic_daylight
        )
        assertNull("Daylight cell secondary must be null", daylightCell.secondary)
        assertFalse("Daylight cell affordance flag must be false", daylightCell.hasDogEarAffordance)
    }

    @Test
    fun testMoonCellModel_hasNewTitleAndDogEarAffordanceTrue() {
        val moonCell = MetricCell(
            type = MetricType.MOON,
            title = "Moon Phase",
            value = "Waning Gibbous",
            secondary = "77% illuminated",
            iconResId = R.drawable.ic_moon
        )
        assertEquals("Moon Phase", moonCell.title)
        assertTrue("Moon cell affordance flag must be true", moonCell.hasDogEarAffordance)
    }

    @Test
    fun testStandardCells_dogEarAffordanceIsFalse() {
        val standardTypes = listOf(
            MetricType.WIND,
            MetricType.AIR_QUALITY,
            MetricType.UV,
            MetricType.VISIBILITY,
            MetricType.HUMIDITY,
            MetricType.CEILING,
            MetricType.DAYLIGHT
        )

        for (type in standardTypes) {
            val cell = MetricCell(
                type = type,
                title = "Test Title",
                value = "Test Value",
                iconResId = 0
            )
            assertFalse("Non-moon cell $type must have hasDogEarAffordance = false", cell.hasDogEarAffordance)
        }
    }

    @Test
    fun testPhaseToDrawableMapping_allEightBucketsAndBoundaries() {
        val testAnglesAndExpected = listOf(
            0.0 to Pair("New Moon", R.drawable.ic_moon_new),
            22.4 to Pair("New Moon", R.drawable.ic_moon_new),
            22.5 to Pair("Waxing Crescent", R.drawable.ic_moon_waxing_crescent),
            67.4 to Pair("Waxing Crescent", R.drawable.ic_moon_waxing_crescent),
            67.5 to Pair("First Quarter", R.drawable.ic_moon_first_quarter),
            112.4 to Pair("First Quarter", R.drawable.ic_moon_first_quarter),
            112.5 to Pair("Waxing Gibbous", R.drawable.ic_moon_waxing_gibbous),
            157.4 to Pair("Waxing Gibbous", R.drawable.ic_moon_waxing_gibbous),
            157.5 to Pair("Full Moon", R.drawable.ic_moon_full),
            202.4 to Pair("Full Moon", R.drawable.ic_moon_full),
            202.5 to Pair("Waning Gibbous", R.drawable.ic_moon_waning_gibbous),
            247.4 to Pair("Waning Gibbous", R.drawable.ic_moon_waning_gibbous),
            247.5 to Pair("Last Quarter", R.drawable.ic_moon_last_quarter),
            292.4 to Pair("Last Quarter", R.drawable.ic_moon_last_quarter),
            292.5 to Pair("Waning Crescent", R.drawable.ic_moon_waning_crescent),
            337.4 to Pair("Waning Crescent", R.drawable.ic_moon_waning_crescent),
            337.5 to Pair("New Moon", R.drawable.ic_moon_new),
            359.9 to Pair("New Moon", R.drawable.ic_moon_new)
        )

        for ((angle, expected) in testAnglesAndExpected) {
            val dummyInfo = MoonPhaseInfo(
                instantUtc = Instant.EPOCH,
                phaseAngleDegrees = angle,
                illuminationFraction = 0.5,
                ageDays = 14.0,
                phaseName = MoonPhaseName.FULL_MOON,
                geocentricDistanceKm = 384400.0
            )

            val formatted = LunarCalculator.formatPhaseDisplay(dummyInfo)
            val standaloneResId = LunarCalculator.getPhaseDrawableResId(angle)

            assertEquals("Phase name mismatch at angle $angle", expected.first, formatted.phaseName)
            assertEquals("Drawable mismatch at angle $angle in FormattedLunarPhase", expected.second, formatted.iconResId)
            assertEquals("Standalone drawable mismatch at angle $angle", expected.second, standaloneResId)
        }
    }

    @Test
    fun testEmojiFreePhaseNames_containNoEmojiCodePoints() {
        val allAngles = listOf(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0)

        for (angle in allAngles) {
            val dummyInfo = MoonPhaseInfo(
                instantUtc = Instant.EPOCH,
                phaseAngleDegrees = angle,
                illuminationFraction = 0.5,
                ageDays = 14.0,
                phaseName = MoonPhaseName.FULL_MOON,
                geocentricDistanceKm = 384400.0
            )

            val formatted = LunarCalculator.formatPhaseDisplay(dummyInfo)

            for (char in formatted.phaseName) {
                val type = Character.getType(char).toByte()
                assertFalse(
                    "Phase name '${formatted.phaseName}' contains non-text emoji character '$char'",
                    type == Character.SURROGATE || type == Character.OTHER_SYMBOL
                )
            }
        }
    }
}
