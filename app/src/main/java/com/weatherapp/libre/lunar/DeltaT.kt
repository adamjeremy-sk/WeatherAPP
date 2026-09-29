package com.weatherapp.libre.lunar

object DeltaT {
    /**
     * Estimates Delta T = TT - UTC in seconds for a given decimal year.
     * Polynomial fits based on Espenak & Meeus (NASA GSFC / IERS).
     */
    fun estimateSeconds(year: Double): Double {
        return when {
            year < 1970.0 -> {
                val t = year - 1900.0
                -2.79 + 1.494119 * t - 0.0598939 * t * t + 0.0061966 * t * t * t
            }
            year in 1970.0..1985.999 -> {
                val t = year - 1975.0
                45.45 + 1.067 * t - 0.02 * t * t
            }
            year in 1986.0..2004.999 -> {
                val t = year - 2000.0
                63.86 + 0.3345 * t - 0.0033584 * t * t
            }
            year in 2005.0..2050.0 -> {
                val t = year - 2015.0
                67.80 + 0.12 * t
            }
            else -> {
                val t = (year - 1820.0) / 100.0
                -20.0 + 32.0 * t * t
            }
        }
    }
}
