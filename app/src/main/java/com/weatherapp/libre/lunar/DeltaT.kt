package com.weatherapp.libre.lunar

/**
 * Estimates Delta T (ΔT = TT - UT1) in seconds for a given decimal year.
 *
 * Uses official polynomial expressions published by Fred Espenak and Jean Meeus
 * in the "Five Millennium Canon of Solar Eclipses" (NASA GSFC).
 *
 * Note: UT1 is approximated by UTC (|UT1 - UTC| < 0.9 s). The small correction "c"
 * for the Moon's secular acceleration is deliberately omitted as it is required only
 * for matching historical eclipses prior to 1955.
 */
object DeltaT {

    /**
     * Estimates Delta T (ΔT = TT - UT1) in seconds for a given decimal year.
     *
     * @param year The decimal year (e.g. 2026.5).
     * @return Estimated Delta T in seconds.
     */
    fun estimateSeconds(year: Double): Double {
        return when {
            year < -500.0 -> {
                val u = (year - 1820.0) / 100.0
                -20.0 + (32.0 * u * u)
            }
            year < 500.0 -> {
                val u = year / 100.0
                val u2 = u * u
                val u3 = u2 * u
                val u4 = u3 * u
                val u5 = u4 * u
                val u6 = u5 * u
                ((((((10583.6 - (1014.41 * u)) + (33.78311 * u2)) - (5.952053 * u3)) - (0.1798452 * u4)) + (0.022174192 * u5)) + (0.0090316521 * u6))
            }
            year < 1600.0 -> {
                val u = (year - 1000.0) / 100.0
                val u2 = u * u
                val u3 = u2 * u
                val u4 = u3 * u
                val u5 = u4 * u
                val u6 = u5 * u
                ((((((1574.2 - (556.01 * u)) + (71.23472 * u2)) + (0.319781 * u3)) - (0.8503463 * u4)) - (0.005050998 * u5)) + (0.0083572073 * u6))
            }
            year < 1700.0 -> {
                val t = year - 1600.0
                val t2 = t * t
                val t3 = t2 * t
                (120.0 - (0.9808 * t) - (0.01532 * t2)) + (t3 / 7129.0)
            }
            year < 1800.0 -> {
                val t = year - 1700.0
                val t2 = t * t
                val t3 = t2 * t
                val t4 = t3 * t
                (((8.83 + (0.1603 * t)) - (0.0059285 * t2)) + (0.00013336 * t3)) - (t4 / 1174000.0)
            }
            year < 1860.0 -> {
                val t = year - 1800.0
                val t2 = t * t
                val t3 = t2 * t
                val t4 = t3 * t
                val t5 = t4 * t
                val t6 = t5 * t
                val t7 = t6 * t
                (((((((13.72 - (0.332447 * t)) + (0.0068612 * t2)) + (0.0041116 * t3)) - (0.00037436 * t4)) + (0.0000121272 * t5)) - (0.0000001699 * t6)) + (0.000000000875 * t7))
            }
            year < 1900.0 -> {
                val t = year - 1860.0
                val t2 = t * t
                val t3 = t2 * t
                val t4 = t3 * t
                val t5 = t4 * t
                (((((7.62 + (0.5737 * t)) - (0.251754 * t2)) + (0.01680668 * t3)) - (0.0004473624 * t4)) + (t5 / 233174.0))
            }
            year < 1920.0 -> {
                val t = year - 1900.0
                val t2 = t * t
                val t3 = t2 * t
                val t4 = t3 * t
                (((-2.79 + (1.494119 * t)) - (0.0598939 * t2)) + (0.0061966 * t3)) - (0.000197 * t4)
            }
            year < 1941.0 -> {
                val t = year - 1920.0
                val t2 = t * t
                val t3 = t2 * t
                ((21.20 + (0.84493 * t)) - (0.076100 * t2)) + (0.0020936 * t3)
            }
            year < 1961.0 -> {
                val t = year - 1950.0
                val t2 = t * t
                val t3 = t2 * t
                ((29.07 + (0.407 * t)) - (t2 / 233.0)) + (t3 / 2547.0)
            }
            year < 1986.0 -> {
                val t = year - 1975.0
                val t2 = t * t
                val t3 = t2 * t
                ((45.45 + (1.067 * t)) - (t2 / 260.0)) - (t3 / 718.0)
            }
            year < 2005.0 -> {
                val t = year - 2000.0
                val t2 = t * t
                val t3 = t2 * t
                val t4 = t3 * t
                val t5 = t4 * t
                (((((63.86 + (0.3345 * t)) - (0.060374 * t2)) + (0.0017275 * t3)) + (0.000651814 * t4)) + (0.00002373599 * t5))
            }
            year < 2050.0 -> {
                val t = year - 2000.0
                val t2 = t * t
                (62.92 + (0.32217 * t)) + (0.005589 * t2)
            }
            year < 2150.0 -> {
                val u = (year - 1820.0) / 100.0
                (-20.0 + (32.0 * u * u)) - (0.5628 * (2150.0 - year))
            }
            else -> {
                val u = (year - 1820.0) / 100.0
                -20.0 + (32.0 * u * u)
            }
        }
    }
}
