package com.weatherapp.libre.solar

import com.weatherapp.libre.lunar.AstroTime
import com.weatherapp.libre.lunar.AstronomyMath
import java.time.Instant
import kotlin.math.*

object SolarPositionEngine {

    data class TopocentricSolarPosition(
        val instantUtc: Instant,
        val azimuthDegrees: Double,         // True North = 0.0°, Clockwise
        val trueAltitudeDegrees: Double,    // Geometric altitude
        val apparentAltitudeDegrees: Double // Refracted altitude
    )

    /**
     * Calculates the topocentric apparent position of the Sun for a given Instant UTC and Observer.
     * Uses Meeus Ch. 25 & WGS84 topocentric conversion.
     */
    fun computeTopocentric(instant: Instant, observer: Observer): TopocentricSolarPosition {
        val jdUtc = AstroTime.instantToJdUtc(instant)
        val jdeTT = AstroTime.jdUtcToJdeTT(jdUtc)
        val T = AstroTime.julianCenturies(jdeTT)

        // Geometric Mean Longitude of the Sun L0 (Meeus Eq 25.2)
        val L0 = AstronomyMath.normalizeDegrees(280.46646 + 36000.76983 * T + 0.0003032 * T * T)

        // Mean Anomaly of the Sun M (Meeus Eq 25.3)
        val M = AstronomyMath.degToRad(357.52911 + 35999.05029 * T - 0.0001537 * T * T)

        // Sun's Equation of Center C (Meeus Eq 25.4)
        val C = (1.914602 - 0.004817 * T - 0.000014 * T * T) * sin(M) +
                (0.019993 - 0.000101 * T) * sin(2.0 * M) +
                0.000289 * sin(3.0 * M)

        // Sun's True Longitude and True Anomaly
        val sunTrueLongitude = L0 + C
        val v = M + AstronomyMath.degToRad(C)
        val e = 0.016708634 - 0.000042037 * T - 0.0000001267 * T * T

        // Sun-Earth Distance R in AU
        val R = (1.000001018 * (1.0 - e * e)) / (1.0 + e * cos(v))

        // Nutation and Obliquity of date
        val nut = AstronomyMath.nutationAndObliquity(jdeTT)
        val omegaRad = AstronomyMath.degToRad(125.04 - 1934.136 * T)

        // Apparent ecliptic longitude of Sun (including aberration & nutation)
        val lambdaApparentDeg = AstronomyMath.normalizeDegrees(sunTrueLongitude + nut.nutationInLongitudeDeg - 0.00569 - 0.00478 * sin(omegaRad))
        val betaDeg = 0.0 // Sun's ecliptic latitude is negligible (< 0.0002°)

        // Convert Ecliptic (lambda, beta) to Equatorial (RA, Dec)
        val eq = AstronomyMath.eclipticToEquatorial(lambdaApparentDeg, betaDeg, nut.trueObliquityDeg)

        // Topocentric parallax correction for Sun (solar parallax ~ 8.794 arcsec)
        val piDeg = 0.0024428 / R

        // Observer WGS84 position
        val latRad = AstronomyMath.degToRad(observer.latitudeDegrees)
        val elevKm = observer.elevationMeters / 1000.0
        val a = 6378.137
        val f = 1.0 / 298.257223563
        val b = a * (1.0 - f)
        val e2 = (a * a - b * b) / (a * a)

        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val N = a / sqrt(1.0 - e2 * sinLat * sinLat)
        val rhoSinPhiDash = (N * (1.0 - e2) + elevKm) * sinLat / a
        val rhoCosPhiDash = (N + elevKm) * cosLat / a

        // Greenwich Apparent Sidereal Time (GAST)
        val gastDeg = AstronomyMath.gastDegrees(jdUtc, jdeTT)
        val lastDeg = AstronomyMath.normalizeDegrees(gastDeg + observer.longitudeDegrees)

        // Hour Angle H
        val HRad = AstronomyMath.degToRad(AstronomyMath.normalizeDegrees(lastDeg - eq.raDeg))
        val decRad = AstronomyMath.degToRad(eq.decDeg)
        val piRad = AstronomyMath.degToRad(piDeg)
        val sinPi = sin(piRad)

        // Topocentric Hour Angle & Declination
        val deltaRaRad = atan2(
            -rhoCosPhiDash * sinPi * sin(HRad),
            cos(decRad) - rhoCosPhiDash * sinPi * cos(HRad)
        )
        val topocentricHRad = HRad - deltaRaRad

        val decNumerator = (sin(decRad) - rhoSinPhiDash * sinPi) * cos(deltaRaRad)
        val decDenominator = cos(decRad) - rhoCosPhiDash * sinPi * cos(HRad)
        val topocentricDecRad = atan2(decNumerator, decDenominator)

        // Topocentric Horizontal Coordinates (Altitude, Azimuth)
        val sinAlt = sin(latRad) * sin(topocentricDecRad) + cos(latRad) * cos(topocentricDecRad) * cos(topocentricHRad)
        val trueAltRad = asin(sinAlt.coerceIn(-1.0, 1.0))
        val trueAltDeg = AstronomyMath.radToDeg(trueAltRad)

        val y = sin(topocentricHRad)
        val x = cos(topocentricHRad) * sin(latRad) - tan(topocentricDecRad) * cos(latRad)
        val azimuthFromSouthRad = atan2(y, x)
        val azimuthFromNorthDeg = AstronomyMath.normalizeDegrees(AstronomyMath.radToDeg(azimuthFromSouthRad) + 180.0)

        // Refraction using Bennett's formula
        val refractionDeg = calculateRefraction(trueAltDeg, observer.pressureHpa, observer.temperatureCelsius)
        val apparentAltDeg = trueAltDeg + refractionDeg

        return TopocentricSolarPosition(
            instantUtc = instant,
            azimuthDegrees = azimuthFromNorthDeg,
            trueAltitudeDegrees = trueAltDeg,
            apparentAltitudeDegrees = apparentAltDeg
        )
    }

    /**
     * Bennett's refraction formula with pressure and temperature corrections.
     */
    private fun calculateRefraction(trueAltDeg: Double, pressureHpa: Double, tempCelsius: Double): Double {
        if (trueAltDeg < -1.0) return 0.0
        val tanArg = trueAltDeg + (10.3 / (trueAltDeg + 5.11))
        val R_arcmin = 1.02 / tan(AstronomyMath.degToRad(tanArg))
        val ptFactor = (pressureHpa / 1010.0) * (283.0 / (273.15 + tempCelsius))
        return (R_arcmin * ptFactor).coerceIn(0.0, 60.0) / 60.0
    }
}
