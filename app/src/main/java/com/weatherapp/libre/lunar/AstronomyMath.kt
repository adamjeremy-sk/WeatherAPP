package com.weatherapp.libre.lunar

import kotlin.math.*

object AstronomyMath {

    fun normalizeDegrees(angle: Double): Double {
        var a = angle % 360.0
        if (a < 0.0) a += 360.0
        return a
    }

    fun degToRad(deg: Double): Double = Math.toRadians(deg)
    fun radToDeg(rad: Double): Double = Math.toDegrees(rad)

    /**
     * Greenwich Mean Sidereal Time (GMST) in degrees for a given JD in UTC.
     * Meeus Ch. 12 Eq. 12.4
     */
    fun gmstDegrees(jdUtc: Double): Double {
        val T = AstroTime.julianCenturies(jdUtc)
        val gmstSeconds = 28016.0298 + (36000.7689254 * 36525.0 * T) + (0.000387933 * T * T) - (T * T * T / 38710000.0)
        val gmstDeg = (gmstSeconds / 240.0) % 360.0
        return normalizeDegrees(gmstDeg)
    }

    data class ObliquityAndNutation(
        val meanObliquityDeg: Double,
        val trueObliquityDeg: Double,
        val nutationInLongitudeDeg: Double,
        val nutationInObliquityDeg: Double
    )

    /**
     * Nutation and obliquity of the ecliptic.
     * Meeus Ch. 22
     */
    fun nutationAndObliquity(jdeTT: Double): ObliquityAndNutation {
        val T = AstroTime.julianCenturies(jdeTT)

        val eps0Seconds = 84381.448 - 46.8150 * T - 0.00059 * T * T + 0.001813 * T * T * T
        val eps0Deg = eps0Seconds / 3600.0

        val omegaRad = degToRad(125.04452 - 1934.136261 * T + 0.0020708 * T * T + T * T * T / 450000.0)
        val LRad     = degToRad(280.4665 + 36000.7698 * T)
        val LdashRad = degToRad(218.3165 + 481267.8813 * T)

        val deltaPsiSec = -17.20 * sin(omegaRad) - 1.32 * sin(2.0 * LRad) - 0.23 * sin(2.0 * LdashRad) + 0.21 * sin(2.0 * omegaRad)
        val deltaEpsSec = 9.20 * cos(omegaRad) + 0.57 * cos(2.0 * LRad) + 0.10 * cos(2.0 * LdashRad) - 0.09 * cos(2.0 * omegaRad)

        val deltaPsiDeg = deltaPsiSec / 3600.0
        val deltaEpsDeg = deltaEpsSec / 3600.0
        val trueEpsDeg = eps0Deg + deltaEpsDeg

        return ObliquityAndNutation(
            meanObliquityDeg = eps0Deg,
            trueObliquityDeg = trueEpsDeg,
            nutationInLongitudeDeg = deltaPsiDeg,
            nutationInObliquityDeg = deltaEpsDeg
        )
    }

    /**
     * Greenwich Apparent Sidereal Time (GAST) in degrees.
     */
    fun gastDegrees(jdUtc: Double, jdeTT: Double): Double {
        val gmst = gmstDegrees(jdUtc)
        val nut = nutationAndObliquity(jdeTT)
        val gast = gmst + nut.nutationInLongitudeDeg * cos(degToRad(nut.trueObliquityDeg))
        return normalizeDegrees(gast)
    }

    data class Equatorial(val raDeg: Double, val decDeg: Double)

    /**
     * Converts Ecliptic (lambda, beta) to Equatorial (RA, Dec) coordinates.
     * Meeus Ch. 13 Eq 13.3, 13.4
     */
    fun eclipticToEquatorial(lambdaDeg: Double, betaDeg: Double, epsDeg: Double): Equatorial {
        val lamRad = degToRad(lambdaDeg)
        val betRad = degToRad(betaDeg)
        val epsRad = degToRad(epsDeg)

        val sinDec = sin(betRad) * cos(epsRad) + cos(betRad) * sin(epsRad) * sin(lamRad)
        val decRad = asin(sinDec.coerceIn(-1.0, 1.0))

        val y = sin(lamRad) * cos(epsRad) - tan(betRad) * sin(epsRad)
        val x = cos(lamRad)
        val raRad = atan2(y, x)

        return Equatorial(
            raDeg = normalizeDegrees(radToDeg(raRad)),
            decDeg = radToDeg(decRad)
        )
    }
}
