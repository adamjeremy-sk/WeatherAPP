package com.weatherapp.libre.lunar

import kotlin.math.*

object SolarPosition {

    data class SolarCoordinates(
        val apparentLongitudeDeg: Double,
        val latitudeDeg: Double,
        val distanceAu: Double
    )

    /**
     * Computes the geocentric apparent ecliptic longitude and distance of the Sun.
     * Meeus Ch. 25
     */
    fun compute(jdeTT: Double): SolarCoordinates {
        val T = AstroTime.julianCenturiesTT(jdeTT)

        val L0 = AstronomyMath.normalizeDegrees(280.46646 + 36000.76983 * T + 0.0003032 * T * T)
        val M = AstronomyMath.degToRad(AstronomyMath.normalizeDegrees(357.52911 + 35999.05029 * T - 0.0001537 * T * T))

        val C = (1.914602 - 0.004817 * T - 0.000014 * T * T) * sin(M) +
                (0.019993 - 0.000101 * T) * sin(2.0 * M) +
                0.000289 * sin(3.0 * M)

        val sunTrueLongitude = L0 + C
        val e = 0.016708634 - 0.000042037 * T - 0.0000001267 * T * T
        val v = M + AstronomyMath.degToRad(C)

        val R = (1.000001018 * (1.0 - e * e)) / (1.0 + e * cos(v))

        val nut = AstronomyMath.nutationAndObliquity(jdeTT)
        val omegaRad = AstronomyMath.degToRad(AstronomyMath.normalizeDegrees(125.04 - 1934.136 * T))
        val lambdaApparent = sunTrueLongitude + nut.nutationInLongitudeDeg - 0.00569 - 0.00478 * sin(omegaRad)

        return SolarCoordinates(
            apparentLongitudeDeg = AstronomyMath.normalizeDegrees(lambdaApparent),
            latitudeDeg = 0.0,
            distanceAu = R
        )
    }
}
