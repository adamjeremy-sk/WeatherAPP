package com.weatherapp.libre.lunar

import java.time.Instant
import kotlin.math.*

object MoonPhaseCalculator {

    fun phaseAt(instant: Instant, observer: Observer? = null): MoonPhaseInfo {
        val jdUtc = AstroTime.instantToJdUtc(instant)
        val jdeTT = AstroTime.jdUtcToJdeTT(jdUtc)

        val sun = SolarPosition.compute(jdeTT)
        val moon = LunarPosition.compute(jdeTT)

        val elongationDeg = AstronomyMath.normalizeDegrees(moon.apparentLongitudeDeg - sun.apparentLongitudeDeg)

        val rAu = sun.distanceAu
        val DeltaKm = moon.distanceKm
        val DeltaAu = DeltaKm / 149597870.7

        val psiRad = AstronomyMath.degToRad(elongationDeg)
        val iRad = atan2(rAu * sin(psiRad), DeltaAu - rAu * cos(psiRad))

        val k = (1.0 + cos(iRad)) / 2.0

        val prevNewMoonInstant = findPrecedingNewMoonInstant(instant)
        val ageSeconds = (instant.epochSecond - prevNewMoonInstant.epochSecond).toDouble() +
                (instant.nano - prevNewMoonInstant.nano).toDouble() / 1e9
        val ageDays = (ageSeconds / 86400.0).coerceAtLeast(0.0)

        val phaseName = determinePhaseName(elongationDeg)

        return MoonPhaseInfo(
            instantUtc = instant,
            phaseAngleDegrees = elongationDeg,
            illuminationFraction = k.coerceIn(0.0, 1.0),
            ageDays = ageDays,
            phaseName = phaseName,
            geocentricDistanceKm = moon.distanceKm
        )
    }

    private fun determinePhaseName(elongationDeg: Double): MoonPhaseName {
        return when {
            elongationDeg < 45.0 || elongationDeg >= 315.0 -> MoonPhaseName.NEW_MOON
            elongationDeg < 135.0 -> MoonPhaseName.FIRST_QUARTER
            elongationDeg < 225.0 -> MoonPhaseName.FULL_MOON
            else -> MoonPhaseName.LAST_QUARTER
        }
    }

    fun computeMeeusPhaseJde(k: Double): Double {
        val T = k / 1236.85
        val T2 = T * T
        val T3 = T2 * T
        val T4 = T3 * T

        val phaseFrac = k - floor(k)
        val phaseIndex = (round(phaseFrac * 4.0).toInt() % 4 + 4) % 4

        var jde0 = 2451534.85610 + 29.530588861 * k + 0.0001178 * T2 - 0.000000155 * T3 + 0.00033 * sin(AstronomyMath.degToRad(166.56 + 132.87 * T - 0.009173 * T2))

        val M = AstronomyMath.degToRad(2.5534 + 29.10535670 * k - 0.0000218 * T2 - 0.00000011 * T3)
        val Mdash = AstronomyMath.degToRad(201.5643 + 385.81693528 * k + 0.0107438 * T2 + 0.00001239 * T3 - 0.000000058 * T4)
        val F = AstronomyMath.degToRad(160.7108 + 390.67050274 * k - 0.0036539 * T2 - T * T * T / 3526000.0 + T * T * T * T / 863310000.0)
        val Omega = AstronomyMath.degToRad(124.7746 - 1.56375580 * k + 0.0020691 * T2 + 0.00000215 * T3)

        val E = 1.0 - 0.002516 * T - 0.0000074 * T2

        var corr = 0.0

        when (phaseIndex) {
            0 -> {
                corr = -0.40720 * sin(Mdash) +
                        0.17241 * E * sin(M) +
                        0.01608 * sin(2.0 * Mdash) +
                        0.01044 * sin(2.0 * F) -
                        0.00739 * E * sin(Mdash - M) -
                        0.00514 * E * sin(Mdash + M) +
                        0.00208 * E * E * sin(2.0 * M) -
                        0.00111 * sin(Mdash - 2.0 * F) -
                        0.00057 * sin(Mdash + 2.0 * F) +
                        0.00056 * E * sin(2.0 * Mdash + M) -
                        0.00042 * sin(3.0 * Mdash) +
                        0.00042 * E * sin(M + 2.0 * F) +
                        0.00038 * E * sin(M - 2.0 * F) -
                        0.00024 * E * sin(2.0 * Mdash - M) -
                        0.00017 * sin(Omega)
            }
            2 -> {
                corr = -0.40614 * sin(Mdash) +
                        0.17302 * E * sin(M) +
                        0.01614 * sin(2.0 * Mdash) +
                        0.01043 * sin(2.0 * F) -
                        0.00734 * E * sin(Mdash - M) -
                        0.00515 * E * sin(Mdash + M) +
                        0.00209 * E * E * sin(2.0 * M) -
                        0.00111 * sin(Mdash - 2.0 * F) -
                        0.00057 * sin(Mdash + 2.0 * F) +
                        0.00056 * E * sin(2.0 * Mdash + M) -
                        0.00042 * sin(3.0 * Mdash) +
                        0.00042 * E * sin(M + 2.0 * F) +
                        0.00038 * E * sin(M - 2.0 * F) -
                        0.00024 * E * sin(2.0 * Mdash - M) -
                        0.00017 * sin(Omega)
            }
            1, 3 -> {
                val sign = if (phaseIndex == 1) +1.0 else -1.0
                corr = -0.38662 * sin(Mdash) +
                        0.16524 * E * sin(M) +
                        0.01602 * sin(2.0 * Mdash) +
                        0.01016 * sin(2.0 * F) -
                        0.00703 * E * sin(Mdash - M) -
                        0.00481 * E * sin(Mdash + M) +
                        0.00192 * E * E * sin(2.0 * M) -
                        0.00111 * sin(Mdash - 2.0 * F) +
                        sign * 0.00306
            }
        }

        jde0 += corr

        val additionalCorr = 0.000325 * sin(AstronomyMath.degToRad(299.77 + 0.107408 * k - 0.009173 * T2)) +
                0.000165 * sin(AstronomyMath.degToRad(251.88 + 0.016321 * k)) +
                0.000164 * sin(AstronomyMath.degToRad(251.83 + 26.651886 * k)) +
                0.000126 * sin(AstronomyMath.degToRad(349.42 + 36.412478 * k)) +
                0.000110 * sin(AstronomyMath.degToRad(84.66 + 18.206239 * k)) +
                0.000062 * sin(AstronomyMath.degToRad(141.74 + 35.258784 * k)) +
                0.000060 * sin(AstronomyMath.degToRad(208.50 + 2.302194 * k)) +
                0.000056 * sin(AstronomyMath.degToRad(154.21 + 29.225698 * k))

        jde0 += additionalCorr
        return jde0
    }

    private fun findPrecedingNewMoonInstant(instant: Instant): Instant {
        val jdUtc = AstroTime.instantToJdUtc(instant)
        val kEstimate = floor((jdUtc - 2451534.85610) / 29.53058867)
        var bestJde = computeMeeusPhaseJde(kEstimate)
        var bestInstant = AstroTime.jdeTTToInstant(bestJde)

        if (bestInstant.isAfter(instant)) {
            bestJde = computeMeeusPhaseJde(kEstimate - 1.0)
            bestInstant = AstroTime.jdeTTToInstant(bestJde)
        }

        return bestInstant
    }

    fun phaseEventsBetween(startUtc: Instant, endUtc: Instant): List<MoonPhaseEvent> {
        if (!startUtc.isBefore(endUtc)) return emptyList()

        val startJdUtc = AstroTime.instantToJdUtc(startUtc)
        val endJdUtc = AstroTime.instantToJdUtc(endUtc)

        val kStart = floor((startJdUtc - 2451534.85610) / 29.53058867) - 1.0
        val kEnd = ceil((endJdUtc - 2451534.85610) / 29.53058867) + 1.0

        val results = mutableListOf<MoonPhaseEvent>()

        var k = kStart
        while (k <= kEnd) {
            for (phaseQuarter in 0..3) {
                val kPhase = k + phaseQuarter * 0.25
                val jdeTT = computeMeeusPhaseJde(kPhase)
                val eventInstant = AstroTime.jdeTTToInstant(jdeTT)

                if (!eventInstant.isBefore(startUtc) && eventInstant.isBefore(endUtc)) {
                    val phaseType = when (phaseQuarter) {
                        0 -> MoonPhaseName.NEW_MOON
                        1 -> MoonPhaseName.FIRST_QUARTER
                        2 -> MoonPhaseName.FULL_MOON
                        3 -> MoonPhaseName.LAST_QUARTER
                        else -> MoonPhaseName.NEW_MOON
                    }
                    results.add(MoonPhaseEvent(phaseType, eventInstant))
                }
            }
            k += 1.0
        }

        return results.distinctBy { Pair(it.type, it.instantUtc.toEpochMilli()) }.sortedBy { it.instantUtc }
    }
}
