package com.weatherapp.libre.lunar

import kotlin.math.tan

object AtmosphericRefraction {

    /**
     * Calculates atmospheric refraction in degrees for a given true (geometric) altitude.
     * Uses Bennett / Meeus standard refraction formula with temperature & pressure correction (Meeus Ch. 16 Eq. 16.4).
     *
     * - Objects with true altitude below -1.0 degree are physically invisible; refraction returns 0.0,
     *   which also bypasses the formula's division asymptote at -5.11 degrees.
     * - The 0.0 to 60.0 arcminute clamp is a defensive physical sanity bound (maximum physical refraction near the
     *   horizon is approximately 40 arcminutes), not a domain correction.
     * - Note there is a documented discontinuity at the -1.0 degree boundary; it lies strictly below all rise/set
     *   event thresholds used by the library.
     *
     * @param trueAltitudeDeg True (geometric) altitude in degrees.
     * @param pressureHpa Atmospheric pressure in hPa (default: 1013.25 hPa).
     * @param temperatureCelsius Temperature in degrees Celsius (default: 10.0 °C).
     * @return Atmospheric refraction in degrees.
     */
    fun calculateRefractionDegrees(
        trueAltitudeDeg: Double,
        pressureHpa: Double = 1013.25,
        temperatureCelsius: Double = 10.0
    ): Double {
        if (trueAltitudeDeg < -1.0) return 0.0

        val tanArgDeg = trueAltitudeDeg + (10.3 / (trueAltitudeDeg + 5.11))
        val R_arcmin = 1.02 / tan(AstronomyMath.degToRad(tanArgDeg))

        val pressureTempCorrection = (pressureHpa / 1010.0) * (283.0 / (273.15 + temperatureCelsius))

        val correctedR_arcmin = (R_arcmin * pressureTempCorrection).coerceIn(0.0, 60.0)
        return correctedR_arcmin / 60.0
    }
}
