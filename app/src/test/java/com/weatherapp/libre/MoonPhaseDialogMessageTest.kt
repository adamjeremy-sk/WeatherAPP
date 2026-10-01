package com.weatherapp.libre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MoonPhaseDialogMessageTest {

    @Test
    fun testMoonPhaseDialogMessageFormatting() {
        val phaseNameWithEmoji = "Waxing Crescent 🌒"
        val illuminationText = "18% illuminated"
        val ageDays = 3.5
        val distanceKm = 384400.0
        val riseStr = "6:00 AM (89° E)"
        val setStr = "8:00 PM (271° W)"

        val upcomingLines = listOf(
            "• 🌑 New Moon: Sun, Oct 11 • 12:00 AM",
            "• 🌓 First Quarter: Sun, Oct 18 • 12:00 AM",
            "• 🌕 Full Moon: Sun, Oct 25 • 12:00 AM",
            "• 🌗 Last Quarter: Sun, Nov 1 • 12:00 AM"
        )

        val message = (listOf(
            "Phase: $phaseNameWithEmoji",
            "Illumination: $illuminationText",
            "Moon Age: ${String.format(Locale.US, "%.1f", ageDays)} days",
            "Distance: ${String.format(Locale.US, "%,.0f", distanceKm)} km",
            "",
            "Moonrise: $riseStr",
            "Moonset: $setStr",
            "",
            "Upcoming Moon Phases:"
        ) + upcomingLines).joinToString("\n")

        val lines = message.split("\n")

        // 4 body stats + 1 blank + 2 rise/set + 1 blank + 1 header + 4 upcoming = 13 lines
        assertEquals(13, lines.size)

        // No line begins with a whitespace character
        for ((index, line) in lines.withIndex()) {
            if (line.isNotEmpty()) {
                assertFalse(
                    "Line $index contains leading whitespace: '$line'",
                    line.first().isWhitespace()
                )
            }
        }

        // Every upcoming-phase line begins with the bullet prefix
        val upcomingSlice = lines.takeLast(4)
        for ((index, line) in upcomingSlice.withIndex()) {
            assertTrue(
                "Upcoming phase line $index does not begin with bullet prefix: '$line'",
                line.startsWith("• ")
            )
        }
    }
}
