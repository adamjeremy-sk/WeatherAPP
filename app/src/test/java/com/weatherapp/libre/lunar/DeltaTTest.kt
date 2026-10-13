package com.weatherapp.libre.lunar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DeltaTTest {

    @Test
    fun testContinuityAtBranchBoundaries() {
        val boundaries = doubleArrayOf(
            -500.0, 500.0, 1600.0, 1700.0, 1800.0, 1860.0, 1900.0, 1920.0,
            1941.0, 1961.0, 1986.0, 2005.0, 2050.0, 2150.0,
        )

        for (boundary in boundaries) {
            val dtBefore = DeltaT.estimateSeconds(boundary - 0.001)
            val dtAfter = DeltaT.estimateSeconds(boundary + 0.001)
            val diff = abs(dtBefore - dtAfter)
            assertTrue("Discontinuity at boundary $boundary: |$dtBefore - $dtAfter| = $diff >= 0.5s", diff < 0.5)
        }
    }

    @Test
    fun testKnownValues() {
        assertEquals(63.86, DeltaT.estimateSeconds(2000.0), 1.0)
        assertEquals(69.3, DeltaT.estimateSeconds(2020.0), 3.0)
        assertEquals(93.0, DeltaT.estimateSeconds(2050.0), 1.0)
        assertEquals(-2.79, DeltaT.estimateSeconds(1900.0), 1.0)

        val dt2200 = DeltaT.estimateSeconds(2200.0)
        val dt2201 = DeltaT.estimateSeconds(2201.0)
        assertTrue("DeltaT at 2200 ($dt2200) must be strictly positive", dt2200 > 0.0)
        assertTrue("DeltaT at 2201 ($dt2201) must be strictly greater than at 2200 ($dt2200)", dt2201 > dt2200)
    }
}
