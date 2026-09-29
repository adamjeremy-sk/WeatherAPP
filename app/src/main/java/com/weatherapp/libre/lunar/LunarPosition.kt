package com.weatherapp.libre.lunar

import kotlin.math.*

object LunarPosition {

    data class LunarCoordinates(
        val apparentLongitudeDeg: Double,
        val latitudeDeg: Double,
        val distanceKm: Double,
        val horizontalParallaxDeg: Double,
        val semidiameterDeg: Double,
        val raDeg: Double,
        val decDeg: Double
    )

    /**
     * Returns LunarGeocentricState for a given Instant UTC.
     */
    fun geocentricState(instantUtc: java.time.Instant): LunarGeocentricState {
        val jdUtc = AstroTime.instantToJdUtc(instantUtc)
        val jdeTT = AstroTime.jdUtcToJdeTT(jdUtc)
        val coords = compute(jdeTT)

        return LunarGeocentricState(
            instantUtc = instantUtc,
            apparentGeocentricLongitudeDegrees = coords.apparentLongitudeDeg,
            apparentGeocentricLatitudeDegrees = coords.latitudeDeg,
            distanceKm = coords.distanceKm,
            horizontalParallaxDegrees = coords.horizontalParallaxDeg,
            apparentSemidiameterDegrees = coords.semidiameterDeg
        )
    }

    /**
     * Computes the geocentric apparent coordinates of the Moon according to Meeus Ch. 47.
     */
    fun compute(jdeTT: Double): LunarCoordinates {
        val T = AstroTime.julianCenturies(jdeTT)

        val Ldash = AstronomyMath.normalizeDegrees(218.3164477 + 481267.8812342 * T - 0.0015786 * T * T + T * T * T / 538841.0 - T * T * T * T / 65194000.0)
        val D = AstronomyMath.degToRad(297.8501921 + 445267.1114034 * T - 0.0018819 * T * T + T * T * T / 545868.0 - T * T * T * T / 113065000.0)
        val M = AstronomyMath.degToRad(357.5291092 + 35999.0502909 * T - 0.0001536 * T * T + T * T * T / 24490000.0)
        val Mdash = AstronomyMath.degToRad(134.9633964 + 477198.8675055 * T + 0.0087414 * T * T + T * T * T / 69699.0 - T * T * T * T / 14712000.0)
        val F = AstronomyMath.degToRad(93.2720950 + 483202.0175233 * T - 0.0036539 * T * T - T * T * T / 3526000.0 + T * T * T * T / 863310000.0)

        val A1 = AstronomyMath.degToRad(119.75 + 131.849 * T)
        val A2 = AstronomyMath.degToRad(53.09 + 479264.290 * T)
        val A3 = AstronomyMath.degToRad(313.45 + 481266.484 * T)
        val E = 1.0 - 0.002516 * T - 0.0000074 * T * T

        var sumL = 0.0
        fun addL(d: Double, m: Double, md: Double, f: Double, coeff: Double) {
            var c = coeff
            val mAbs = abs(m)
            if (mAbs == 1.0) c *= E
            else if (mAbs == 2.0) c *= E * E
            sumL += c * sin(d * D + m * M + md * Mdash + f * F)
        }

        addL(0.0, 0.0, 1.0, 0.0, 6288774.0)
        addL(2.0, 0.0, -1.0, 0.0, 1274027.0)
        addL(2.0, 0.0, 0.0, 0.0, 658314.0)
        addL(0.0, 0.0, 2.0, 0.0, 213618.0)
        addL(-2.0, 0.0, 1.0, 0.0, -185116.0)
        addL(0.0, 1.0, 0.0, 0.0, -114332.0)
        addL(0.0, 0.0, 0.0, 2.0, -58793.0)
        addL(2.0, 0.0, -2.0, 0.0, 57066.0)
        addL(2.0, -1.0, -1.0, 0.0, 53322.0)
        addL(2.0, 0.0, 1.0, 0.0, 45758.0)
        addL(2.0, -1.0, 0.0, 0.0, -40923.0)
        addL(0.0, 1.0, -1.0, 0.0, -34720.0)
        addL(1.0, 0.0, 0.0, 0.0, -30383.0)
        addL(0.0, 1.0, 1.0, 0.0, 15327.0)
        addL(2.0, -1.0, 1.0, 0.0, -12528.0)
        addL(0.0, 0.0, 1.0, -2.0, 10980.0)
        addL(4.0, 0.0, -1.0, 0.0, 10675.0)
        addL(0.0, 0.0, 3.0, 0.0, 10034.0)
        addL(4.0, 0.0, -2.0, 0.0, 8548.0)
        addL(2.0, 1.0, -1.0, 0.0, -7888.0)
        addL(2.0, 1.0, 0.0, 0.0, -6766.0)
        addL(1.0, 0.0, -1.0, 0.0, -5163.0)
        addL(1.0, 1.0, 0.0, 0.0, 4987.0)
        addL(2.0, -1.0, -2.0, 0.0, 4026.0)
        addL(2.0, 0.0, -3.0, 0.0, 3660.0)
        addL(2.0, 0.0, 2.0, 0.0, 3539.0)
        addL(2.0, 0.0, 0.0, -2.0, -2280.0)
        addL(0.0, 0.0, 1.0, 2.0, -2058.0)
        addL(2.0, -2.0, 0.0, 0.0, -1781.0)
        addL(0.0, 0.0, 2.0, -2.0, -1458.0)
        addL(2.0, 1.0, -2.0, 0.0, -1298.0)
        addL(2.0, 0.0, -1.0, -2.0, -662.0)
        addL(4.0, -1.0, -1.0, 0.0, -643.0)
        addL(0.0, 1.0, -2.0, 0.0, -510.0)

        sumL += 3958.0 * sin(A1) + 1962.0 * sin(Ldash - F) + 318.0 * sin(A2)

        var sumB = 0.0
        fun addB(d: Double, m: Double, md: Double, f: Double, coeff: Double) {
            var c = coeff
            val mAbs = abs(m)
            if (mAbs == 1.0) c *= E
            else if (mAbs == 2.0) c *= E * E
            sumB += c * sin(d * D + m * M + md * Mdash + f * F)
        }

        addB(0.0, 0.0, 0.0, 1.0, 5128122.0)
        addB(0.0, 0.0, 1.0, 1.0, 280602.0)
        addB(0.0, 0.0, 1.0, -1.0, 277693.0)
        addB(2.0, 0.0, 0.0, -1.0, 173237.0)
        addB(2.0, 0.0, -1.0, 1.0, 55413.0)
        addB(2.0, 0.0, -1.0, -1.0, 46271.0)
        addB(2.0, 0.0, 0.0, 1.0, 32573.0)
        addB(0.0, 0.0, 2.0, 1.0, 17198.0)
        addB(2.0, 0.0, 1.0, -1.0, 9266.0)
        addB(0.0, 0.0, 2.0, -1.0, 8822.0)
        addB(2.0, -1.0, 0.0, -1.0, 8216.0)
        addB(2.0, 0.0, 1.0, 1.0, 4324.0)
        addB(2.0, -1.0, -1.0, -1.0, 4200.0)

        sumB += -2235.0 * sin(Ldash) + 382.0 * sin(A3) + 175.0 * sin(A1 - F) + 175.0 * sin(A1 + F) + 127.0 * sin(Ldash - Mdash) - 115.0 * sin(Ldash + Mdash)

        var sumR = 0.0
        fun addR(d: Double, m: Double, md: Double, f: Double, coeff: Double) {
            var c = coeff
            val mAbs = abs(m)
            if (mAbs == 1.0) c *= E
            else if (mAbs == 2.0) c *= E * E
            sumR += c * cos(d * D + m * M + md * Mdash + f * F)
        }

        addR(0.0, 0.0, 1.0, 0.0, -20905355.0)
        addR(2.0, 0.0, -1.0, 0.0, -3699111.0)
        addR(2.0, 0.0, 0.0, 0.0, -2955968.0)
        addR(0.0, 0.0, 2.0, 0.0, -569925.0)
        addR(0.0, 1.0, 0.0, 0.0, 48888.0)
        addR(0.0, 0.0, 0.0, 2.0, -31490.0)
        addR(2.0, 0.0, -2.0, 0.0, 246158.0)
        addR(2.0, -1.0, -1.0, 0.0, -152138.0)
        addR(2.0, 0.0, 1.0, 0.0, -170733.0)
        addR(2.0, -1.0, 0.0, 0.0, -204586.0)
        addR(0.0, 1.0, -1.0, 0.0, -129620.0)
        addR(1.0, 0.0, 0.0, 0.0, 108743.0)
        addR(0.0, 1.0, 1.0, 0.0, 104755.0)
        addR(2.0, -1.0, 1.0, 0.0, 10321.0)
        addR(0.0, 0.0, 1.0, -2.0, 79661.0)

        val nut = AstronomyMath.nutationAndObliquity(jdeTT)
        val lambdaDeg = AstronomyMath.normalizeDegrees(Ldash + sumL / 1_000_000.0 + nut.nutationInLongitudeDeg)
        val betaDeg = sumB / 1_000_000.0

        val distanceKm = 385000.56 + (sumR / 1000.0)

        val sinParallax = 6378.14 / distanceKm
        val parallaxDeg = AstronomyMath.radToDeg(asin(sinParallax.coerceIn(0.0, 1.0)))

        val semidiameterDeg = AstronomyMath.radToDeg(asin(0.272481 * sinParallax))

        val eq = AstronomyMath.eclipticToEquatorial(lambdaDeg, betaDeg, nut.trueObliquityDeg)

        return LunarCoordinates(
            apparentLongitudeDeg = lambdaDeg,
            latitudeDeg = betaDeg,
            distanceKm = distanceKm,
            horizontalParallaxDeg = parallaxDeg,
            semidiameterDeg = semidiameterDeg,
            raDeg = eq.raDeg,
            decDeg = eq.decDeg
        )
    }
}
