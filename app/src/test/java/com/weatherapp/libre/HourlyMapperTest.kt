package com.weatherapp.libre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HourlyMapperTest {

    @Test
    fun testWindowSlicingTo36Hours() {
        val now = Instant.parse("2023-10-02T14:30:00Z")
        val times = mutableListOf<String>()
        val temps = mutableListOf<Double?>()

        // Generate 50 consecutive UTC hours starting at 2023-10-02T00:00:00Z
        val startInstant = Instant.parse("2023-10-02T00:00:00Z")
        for (i in 0 until 50) {
            val inst = startInstant.plus(i.toLong(), ChronoUnit.HOURS)
            times.add(inst.toString())
            temps.add(20.0 + i)
        }

        val rawData = HourlyData(
            time = times,
            temperature2m = temps
        )

        val mapped = HourlyMapper.mapRawHourly(rawData)
        assertEquals(50, mapped.size)

        val zoneId = ZoneId.of("America/New_York")
        val items = HourlyMapper.build36HourList(mapped, now, zoneId)

        // Hour 14:00:00Z is index 14 in raw data. 36 hours sliced -> 36 HourRows + day dividers (Oct 2 -> Oct 3)
        val hourRows = items.filterIsInstance<HourlyListItem.HourRow>()
        assertEquals(36, hourRows.size)

        // First row is the current hour (14:00Z)
        val firstRow = hourRows.first()
        assertEquals(Instant.parse("2023-10-02T14:00:00Z"), firstRow.forecast.time)
        assertTrue(firstRow.isCurrentHour)

        // Last row is 14:00 + 35 hours = Oct 4 01:00Z
        val lastRow = hourRows.last()
        assertEquals(Instant.parse("2023-10-04T01:00:00Z"), lastRow.forecast.time)
        assertFalse(lastRow.isCurrentHour)

        // HorizonFooter should NOT be appended because full 36 hours were available
        val footer = items.filterIsInstance<HourlyListItem.HorizonFooter>()
        assertTrue(footer.isEmpty())
    }

    @Test
    fun testDstContinuitySpringForward() {
        // America/New_York spring forward transition date: 2023-03-12 (23 local hours)
        val now = Instant.parse("2023-03-11T12:00:00Z")
        val times = mutableListOf<String>()

        val startInstant = Instant.parse("2023-03-11T00:00:00Z")
        for (i in 0 until 48) {
            times.add(startInstant.plus(i.toLong(), ChronoUnit.HOURS).toString())
        }

        val rawData = HourlyData(time = times)
        val mapped = HourlyMapper.mapRawHourly(rawData)
        val zoneId = ZoneId.of("America/New_York")

        val items = HourlyMapper.build36HourList(mapped, now, zoneId)
        val hourRows = items.filterIsInstance<HourlyListItem.HourRow>()

        assertEquals(36, hourRows.size)
        // Verify all timestamps are strictly 1 hour apart (3600 seconds)
        for (i in 0 until hourRows.size - 1) {
            val t1 = hourRows[i].forecast.time
            val t2 = hourRows[i + 1].forecast.time
            assertEquals(3600L, ChronoUnit.SECONDS.between(t1, t2))
        }
    }

    @Test
    fun testNullHandling() {
        val times = listOf("2023-10-02T14:00:00Z")
        val rawData = HourlyData(
            time = times,
            temperature2m = listOf(null),
            precipitationProbability = listOf(null)
        )

        val mapped = HourlyMapper.mapRawHourly(rawData)
        assertEquals(1, mapped.size)
        assertNull(mapped.first().temperature)
        assertNull(mapped.first().precipitationProbability)
        assertNull(mapped.first().uvIndex)
    }

    @Test
    fun testTruncationAndHorizonFooter() {
        val now = Instant.parse("2023-10-02T14:00:00Z")
        val times = mutableListOf<String>()

        // Generate only 10 consecutive hours starting at 2023-10-02T14:00:00Z
        val startInstant = Instant.parse("2023-10-02T14:00:00Z")
        for (i in 0 until 10) {
            times.add(startInstant.plus(i.toLong(), ChronoUnit.HOURS).toString())
        }

        val rawData = HourlyData(time = times)
        val mapped = HourlyMapper.mapRawHourly(rawData)
        val zoneId = ZoneId.of("America/New_York")

        val items = HourlyMapper.build36HourList(mapped, now, zoneId)
        val hourRows = items.filterIsInstance<HourlyListItem.HourRow>()
        assertEquals(10, hourRows.size)

        // HorizonFooter MUST be appended at the end because window < 36
        val footer = items.filterIsInstance<HourlyListItem.HorizonFooter>()
        assertEquals(1, footer.size)
        assertEquals("Hourly data ends here for this model", footer.first().message)
    }
}
