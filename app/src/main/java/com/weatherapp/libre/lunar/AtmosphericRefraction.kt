package com.weatherapp.libre.lunar

import kotlin.math.tan

object AtmosphericRefraction {

    /**
     * Calculates atmospheric refraction in degrees for a given true (geometric) altitude.
     * Uses Bennett / Meeus standard refraction formula with temperature & pressure correction.
     * Meeus Ch. 16 Eq. 16.3 & 16.4
     */
    fun calculateRefractionDegrees(
        trueAltitudeDeg: Double,
        pressureHpa: Double = 1013.25,
        temperatureCelsius: Double = 10.0
    ): Double {
        val altDeg = trueAltitudeDeg.coerceAtLeast(-5.0)

        val tanArgDeg = altDeg + (10.3 / (altDeg + 5.11))
        val R_arcmin = 1.02 / tan(AstronomyMath.degToRad(tanArgDeg))

        val pressureTempCorrection = (pressureHpa / 1010.0) * (283.0 / (273.15 + temperatureCelsius))

        val correctedR_arcmin = (R_arcmin * pressureTempCorrection).coerceIn(0.0, 60.0)
        return correctedR_arcmin / 60.0
    }
}
