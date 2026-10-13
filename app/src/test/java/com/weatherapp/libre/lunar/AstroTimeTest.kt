package com.weatherapp.libre.lunar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

class AstroTimeTest {

    @Test
    fun testPre1970RoundTrip() {
        val apollo11Landing = Instant.parse("1969-07-20T20:17:00Z")
        val exactNegativeHalfSecond = Instant.parse("1969-12-31T23:59:59.500Z")

        val roundTrip1 = AstroTime.jdUtcToInstant(AstroTime.instantToJdUtc(apollo11Landing))
        val diffNanos1 = Duration.between(apollo11Landing, roundTrip1).abs().toNanos()
        assertTrue("Apollo 11 difference $diffNanos1 ns exceeds Double JD precision limit (200 µs)", diffNanos1 < 200_000)

        val roundTrip2 = AstroTime.jdUtcToInstant(AstroTime.instantToJdUtc(exactNegativeHalfSecond))
        val diffNanos2 = Duration.between(exactNegativeHalfSecond, roundTrip2).abs().toNanos()
        assertTrue("Negative half second difference $diffNanos2 ns exceeds Double JD precision limit (200 µs)", diffNanos2 < 200_000)
    }

    @Test
    fun testEpochBoundary() {
        val epoch = Instant.EPOCH
        val roundTrip = AstroTime.jdUtcToInstant(AstroTime.instantToJdUtc(epoch))
        assertEquals(epoch, roundTrip)
    }

    @Test
    fun testModernRoundTrip() {
        val modernInstant = Instant.parse("2026-06-21T12:00:00Z")
        val roundTrip = AstroTime.jdUtcToInstant(AstroTime.instantToJdUtc(modernInstant))
        assertEquals(modernInstant, roundTrip)
    }

    @Test
    fun testTtUtcInversionConsistency() {
        val instant = Instant.parse("2026-06-21T12:00:00Z")
        val jdUtc = AstroTime.instantToJdUtc(instant)
        val jdeTT = AstroTime.jdUtcToJdeTT(jdUtc)
        val invertedJdUtc = AstroTime.jdeTTToJdUtc(jdeTT)

        val diff = abs(invertedJdUtc - jdUtc)
        assertTrue("Inversion difference $diff exceeds threshold 1e-9", diff < 1e-9)
    }

    @Test
    fun testJulianCenturiesTT() {
        assertEquals(0.0, AstroTime.julianCenturiesTT(2451545.0), 1e-15)
        assertEquals(1.0, AstroTime.julianCenturiesTT(2451545.0 + 36525.0), 1e-15)
    }
}
