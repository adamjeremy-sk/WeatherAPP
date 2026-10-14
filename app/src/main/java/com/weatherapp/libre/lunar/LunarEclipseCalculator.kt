package com.weatherapp.libre.lunar

import java.time.Instant
import kotlin.math.*

enum class EclipseType {
    PENUMBRAL,
    PARTIAL,
    TOTAL,
}

enum class ShadowBoundary {
    PENUMBRA,
    UMBRA_OUTER,
    UMBRA_INNER,
}

data class EclipseContacts(
    val p1: Instant? = null,
    val u1: Instant? = null,
    val u2: Instant? = null,
    val u3: Instant? = null,
    val u4: Instant? = null,
    val p4: Instant? = null,
)

data class LunarEclipseInfo(
    val type: EclipseType,
    val magnitude: Double,
    val contacts: EclipseContacts,
    val peakInstant: Instant,
)

sealed interface EclipseCalculationResult {
    data class Success(val eclipse: LunarEclipseInfo) : EclipseCalculationResult
    data object NoEclipse : EclipseCalculationResult
    data class Failure(val reason: String) : EclipseCalculationResult
}

/**
 * Inspired by the geometry of Jean Meeus's Astronomical Algorithms.
 * Uses a direct numerical spherical approach rather than Besselian elements.
 *
 * SCIENTIFIC DISCLOSURES & LIMITATIONS:
 * 1. Coordinates and Fundamental Plane:
 *    Shadow-relative separation is calculated using exact spherical geometry on the celestial sphere
 *    via a numerically stable atan2 formulation:
 *    y = sqrt(sin(β)^2 + (cos(β) * sin(Δλ))^2), x = cos(β) * cos(Δλ), separation = atan2(y, x).
 * 2. Earth Shadow Enlargement Factor:
 *    Earth's atmosphere enlarges the umbral and penumbral shadows. This calculator applies Chauvenet's
 *    empirical factor of 1.02 (+2% enlargement) strictly to the parallax terms (Earth's effective
 *    shadow-casting radius):
 *    Effective Parallax = 1.02 * (piMoon + piSun).
 *    Penumbral Radius = Effective Parallax + sSun.
 *    Umbral Radius = Effective Parallax - sSun.
 * 3. Numerical Precision vs Astronomical Accuracy:
 *    The stopping criterion `tolDays` (0.1 seconds = 1.15e-6 days) is a numerical convergence limit
 *    for 1D Golden Section Search and Bisection root-finding. The conversion from TT to UTC is exact
 *    conditional on the active leap-second table. The physical timing error budget arises from lunar
 *    series truncation (Meeus Ch. 47, ~10″ longitude / ~20s time), the angular-shadow approximation,
 *    and the empirical 1.02 enlargement factor, and must be measured against a reference ephemeris (~1 minute physical error budget).
 * 4. Earth Oblateness:
 *    The equatorial horizontal parallax is used directly without correcting for Earth's oblateness
 *    (the shadow-casting radius is slightly smaller). The modelled shadow is slightly too large, causing
 *    ingress contacts to be early and egress contacts late; the peak time is unaffected.
 * 5. Angular-Shadow Approximation:
 *    This is the classical shadow-cone geometry written in angles at the geocentre.
 * 6. Penumbral Convention:
 *    Applying the 1.02 enlargement factor to the penumbra has no observational basis; P1/P4 timings are strictly conventional.
 */
object LunarEclipseCalculator {

    private const val STEP_DAYS = 10.0 / 1440.0 // 10 minutes per step
    private const val MAX_STEPS = 36 // Up to 6 hours search in each direction
    private const val TOL_DAYS = 0.1 / 86400.0 // 0.1 second numerical tolerance

    /**
     * Exact angular distance in degrees between Moon center and Earth shadow center
     * using the numerically stable atan2 spherical formula.
     */
    fun moonToAntisolarAngularSeparationDeg(jde: Double): Double {
        val m = LunarPosition.compute(jde)
        val s = SolarPosition.compute(jde)
        val shadowLon = AstronomyMath.normalizeDegrees(s.apparentLongitudeDeg + 180.0)
        var dLon = m.apparentLongitudeDeg - shadowLon
        if (dLon > 180.0) dLon -= 360.0
        if (dLon < -180.0) dLon += 360.0

        val betaRad = AstronomyMath.degToRad(m.latitudeDeg)
        val dLonRad = AstronomyMath.degToRad(dLon)

        val y = sqrt(sin(betaRad).pow(2) + (cos(betaRad) * sin(dLonRad)).pow(2))
        val x = cos(betaRad) * cos(dLonRad)

        return Math.toDegrees(atan2(y, x))
    }

    /**
     * Computes the interaction boundary radius in degrees for a given trial time [jde]
     * and [ShadowBoundary] target.
     */
    fun computeBoundaryRadius(jde: Double, boundary: ShadowBoundary): Double {
        val m = LunarPosition.compute(jde)
        val s = SolarPosition.compute(jde)

        val piMoon = m.horizontalParallaxDeg
        val sMoon = m.semidiameterDeg

        val piSun = (8.794 / 3600.0) / s.distanceAu
        val sSun = (959.63 / 3600.0) / s.distanceAu

        val effectiveParallaxSum = 1.02 * (piMoon + piSun)

        return when (boundary) {
            ShadowBoundary.PENUMBRA -> (effectiveParallaxSum + sSun) + sMoon
            ShadowBoundary.UMBRA_OUTER -> (effectiveParallaxSum - sSun) + sMoon
            ShadowBoundary.UMBRA_INNER -> (effectiveParallaxSum - sSun) - sMoon
        }
    }

    /**
     * Calculates lunar eclipse details for an eclipse occurring near the supplied instant
     * (searches up to ±12 hours, bounded by a ±6h linear clamp and a ±6h outward walk).
     * Returns an [EclipseCalculationResult] enum/sealed state.
     */
    fun calculateLunarEclipse(fullMoonInstant: Instant): EclipseCalculationResult {
        val jdUtc = AstroTime.instantToJdUtc(fullMoonInstant)
        val jdeTT = AstroTime.jdUtcToJdeTT(jdUtc)

        // Helper function for linear velocity estimation
        fun computeRelativeCoords(jde: Double): Pair<Double, Double> {
            val m = LunarPosition.compute(jde)
            val s = SolarPosition.compute(jde)
            val shadowLon = AstronomyMath.normalizeDegrees(s.apparentLongitudeDeg + 180.0)
            var dLon = m.apparentLongitudeDeg - shadowLon
            if (dLon > 180.0) dLon -= 360.0
            if (dLon < -180.0) dLon += 360.0
            val x = dLon * cos(AstronomyMath.degToRad(m.latitudeDeg))
            val y = m.latitudeDeg
            return Pair(x, y)
        }

        // STEP 1: Initial linear guess for peak time
        val dtHours = 0.5
        val dtDays = dtHours / 24.0
        val (x1, y1) = computeRelativeCoords(jdeTT - dtDays)
        val (x2, y2) = computeRelativeCoords(jdeTT + dtDays)

        val xDot = (x2 - x1) / (2.0 * dtHours)
        val yDot = (y2 - y1) / (2.0 * dtHours)
        val nSq = xDot * xDot + yDot * yDot

        val (x0, y0) = computeRelativeCoords(jdeTT)
        val tauHoursUnbounded = if (nSq > 1e-12) -(x0 * xDot + y0 * yDot) / nSq else 0.0
        val tauHours = tauHoursUnbounded.coerceIn(-6.0, 6.0)
        val jdePeakGuess = jdeTT + tauHours / 24.0

        // STEP 2: Dynamic Outward Stepping to Bracket the Minimum for Peak Search
        var prevSep = moonToAntisolarAngularSeparationDeg(jdePeakGuess)
        var lowPeak = jdePeakGuess
        var foundLowBracket = false
        for (step in 1..MAX_STEPS) {
            val trialJde = jdePeakGuess - step * STEP_DAYS
            val currSep = moonToAntisolarAngularSeparationDeg(trialJde)
            if (currSep.isNaN() || currSep.isInfinite()) {
                return EclipseCalculationResult.Failure("Non-finite separation during lowPeak bracketing")
            }
            if (currSep > prevSep) {
                lowPeak = trialJde
                foundLowBracket = true
                break
            }
            prevSep = currSep
        }

        prevSep = moonToAntisolarAngularSeparationDeg(jdePeakGuess)
        var highPeak = jdePeakGuess
        var foundHighBracket = false
        for (step in 1..MAX_STEPS) {
            val trialJde = jdePeakGuess + step * STEP_DAYS
            val currSep = moonToAntisolarAngularSeparationDeg(trialJde)
            if (currSep.isNaN() || currSep.isInfinite()) {
                return EclipseCalculationResult.Failure("Non-finite separation during highPeak bracketing")
            }
            if (currSep > prevSep) {
                highPeak = trialJde
                foundHighBracket = true
                break
            }
            prevSep = currSep
        }

        if (!foundLowBracket || !foundHighBracket) {
            return EclipseCalculationResult.Failure("Failed to bracket peak minimum")
        }

        val phi = (sqrt(5.0) - 1.0) / 2.0

        var cPeak = highPeak - phi * (highPeak - lowPeak)
        var dPeak = lowPeak + phi * (highPeak - lowPeak)
        var fcPeak = moonToAntisolarAngularSeparationDeg(cPeak)
        var fdPeak = moonToAntisolarAngularSeparationDeg(dPeak)

        var iterPeak = 0
        while (abs(highPeak - lowPeak) > TOL_DAYS && iterPeak < 100) {
            iterPeak++
            if (fcPeak.isNaN() || fcPeak.isInfinite() || fdPeak.isNaN() || fdPeak.isInfinite()) {
                return EclipseCalculationResult.Failure("Non-finite separation in peak search")
            }

            if (fcPeak < fdPeak) {
                highPeak = dPeak
                dPeak = cPeak
                fdPeak = fcPeak
                cPeak = highPeak - phi * (highPeak - lowPeak)
                fcPeak = moonToAntisolarAngularSeparationDeg(cPeak)
            } else {
                lowPeak = cPeak
                cPeak = dPeak
                fcPeak = fdPeak
                dPeak = lowPeak + phi * (highPeak - lowPeak)
                fdPeak = moonToAntisolarAngularSeparationDeg(dPeak)
            }
        }

        // STEP 3.1: Post-loop finite & convergence checks
        if (fcPeak.isNaN() || fcPeak.isInfinite() || fdPeak.isNaN() || fdPeak.isInfinite()) {
            return EclipseCalculationResult.Failure("Non-finite separation at peak convergence")
        }

        if (abs(highPeak - lowPeak) > TOL_DAYS) {
            return EclipseCalculationResult.Failure("Peak search did not converge")
        }

        val jdePeak = (lowPeak + highPeak) / 2.0
        val peakInstant = roundToNearestMinute(AstroTime.jdeTTToInstant(jdePeak))

        // Minimum angular separation (gamma) at peak eclipse instant
        val gamma = moonToAntisolarAngularSeparationDeg(jdePeak)

        // STEP 2: Unified interaction radius calculations at peak
        val moonPeak = LunarPosition.compute(jdePeak)
        val sMoon = moonPeak.semidiameterDeg

        val penumbraInteraction = computeBoundaryRadius(jdePeak, ShadowBoundary.PENUMBRA)
        val umbraOuterInteraction = computeBoundaryRadius(jdePeak, ShadowBoundary.UMBRA_OUTER)
        val umbraInnerInteraction = computeBoundaryRadius(jdePeak, ShadowBoundary.UMBRA_INNER)

        // STEP 3.2: Direct gamma-to-radius eclipse type classification
        val type = when {
            gamma <= umbraInnerInteraction -> EclipseType.TOTAL
            gamma <= umbraOuterInteraction -> EclipseType.PARTIAL
            gamma <= penumbraInteraction -> EclipseType.PENUMBRAL
            else -> return EclipseCalculationResult.NoEclipse
        }

        // Magnitudes calculated after type determination
        val penumbralMag = (penumbraInteraction - gamma) / (2.0 * sMoon)
        val umbralMag = (umbraOuterInteraction - gamma) / (2.0 * sMoon)
        val magnitude = if (type == EclipseType.PENUMBRAL) max(0.0, penumbralMag) else max(0.0, umbralMag)

        // Dynamic Root-finding for contact times without parameter shadowing
        fun separationMinusBoundary(jde: Double, boundary: ShadowBoundary): Double {
            val sep = moonToAntisolarAngularSeparationDeg(jde)
            val targetRadius = computeBoundaryRadius(jde, boundary)
            return sep - targetRadius
        }

        fun findContactFromPeak(centerJde: Double, boundary: ShadowBoundary, isIngress: Boolean): Pair<Double, Instant>? {
            val fPeak = separationMinusBoundary(centerJde, boundary)
            if (fPeak > 0.0 || fPeak.isNaN() || fPeak.isInfinite()) {
                return null // Peak is outside radius or non-finite, contact never occurs
            }

            val direction = if (isIngress) -1.0 else 1.0

            var t1 = centerJde
            var t2 = centerJde
            var foundBracket = false

            for (step in 1..MAX_STEPS) {
                t2 = centerJde + direction * (step * STEP_DAYS)
                val f2 = separationMinusBoundary(t2, boundary)
                if (f2.isNaN() || f2.isInfinite()) {
                    return null
                }
                if (f2 > 0.0) {
                    foundBracket = true
                    break
                }
                t1 = t2
            }

            if (!foundBracket) {
                return null
            }

            // Bisect discovered bracket [t1, t2] (or [t2, t1]) with dynamic radii at trial time
            var a = min(t1, t2)
            var b = max(t1, t2)
            var fa = separationMinusBoundary(a, boundary)
            val fb = separationMinusBoundary(b, boundary)

            // Strict assertion: outward stepping guarantees sign change
            require(fa * fb <= 0.0) { "Bisection bracket invalid" }

            var iter = 0
            while (abs(b - a) > TOL_DAYS && iter < 100) {
                iter++
                val mid = (a + b) / 2.0
                val fMid = separationMinusBoundary(mid, boundary)
                if (fMid.isNaN() || fMid.isInfinite()) {
                    return null
                }

                if (fa * fMid <= 0.0) {
                    b = mid
                } else {
                    a = mid
                    fa = fMid
                }
            }

            if (abs(b - a) > TOL_DAYS) {
                return null // Non-convergent interval
            }

            val finalJde = (a + b) / 2.0
            val finalResidual = abs(separationMinusBoundary(finalJde, boundary))
            if (finalResidual > 1e-4) {
                return null // Residual error exceeds 0.0001 deg tolerance
            }

            val finalInstant = roundToNearestMinute(AstroTime.jdeTTToInstant(finalJde))
            return Pair(finalJde, finalInstant)
        }

        val p1Result = findContactFromPeak(jdePeak, ShadowBoundary.PENUMBRA, isIngress = true)
        val p4Result = findContactFromPeak(jdePeak, ShadowBoundary.PENUMBRA, isIngress = false)

        val u1Result = if (type == EclipseType.PARTIAL || type == EclipseType.TOTAL) findContactFromPeak(jdePeak, ShadowBoundary.UMBRA_OUTER, isIngress = true) else null
        val u4Result = if (type == EclipseType.PARTIAL || type == EclipseType.TOTAL) findContactFromPeak(jdePeak, ShadowBoundary.UMBRA_OUTER, isIngress = false) else null

        val u2Result = if (type == EclipseType.TOTAL) findContactFromPeak(jdePeak, ShadowBoundary.UMBRA_INNER, isIngress = true) else null
        val u3Result = if (type == EclipseType.TOTAL) findContactFromPeak(jdePeak, ShadowBoundary.UMBRA_INNER, isIngress = false) else null

        // Strict Contact Completeness Validation
        val requiredContactsMet = when (type) {
            EclipseType.PENUMBRAL -> p1Result != null && p4Result != null
            EclipseType.PARTIAL -> p1Result != null && u1Result != null && u4Result != null && p4Result != null
            EclipseType.TOTAL -> p1Result != null && u1Result != null && u2Result != null &&
                    u3Result != null && u4Result != null && p4Result != null
        }
        if (!requiredContactsMet) {
            return EclipseCalculationResult.Failure("Failed to resolve all required contacts for $type eclipse")
        }

        // Unrounded Chronological Verification
        val p1Jde = p1Result?.first
        val u1Jde = u1Result?.first
        val u2Jde = u2Result?.first
        val u3Jde = u3Result?.first
        val u4Jde = u4Result?.first
        val p4Jde = p4Result?.first

        val p1Valid = p1Jde == null || (u1Jde == null || p1Jde <= u1Jde) && p1Jde <= jdePeak
        val u1Valid = u1Jde == null || (u2Jde == null || u1Jde <= u2Jde) && u1Jde <= jdePeak
        val u2Valid = u2Jde == null || u2Jde <= jdePeak
        val u3Valid = u3Jde == null || jdePeak <= u3Jde
        val u4Valid = u4Jde == null || (u3Jde == null || u3Jde <= u4Jde) && jdePeak <= u4Jde
        val p4Valid = p4Jde == null || (u4Jde == null || u4Jde <= p4Jde) && p4Jde >= jdePeak

        if (!p1Valid || !u1Valid || !u2Valid || !u3Valid || !u4Valid || !p4Valid) {
            return EclipseCalculationResult.Failure("Contact times failed chronological sanity checks")
        }

        val contacts = EclipseContacts(
            p1 = p1Result?.second,
            u1 = u1Result?.second,
            u2 = u2Result?.second,
            u3 = u3Result?.second,
            u4 = u4Result?.second,
            p4 = p4Result?.second,
        )

        val info = LunarEclipseInfo(
            type = type,
            magnitude = magnitude,
            contacts = contacts,
            peakInstant = peakInstant,
        )

        return EclipseCalculationResult.Success(info)
    }

    private fun roundToNearestMinute(instant: Instant): Instant {
        val totalSec = instant.epochSecond.toDouble() + (instant.nano.toDouble() / 1e9)
        val roundedMinSec = (totalSec / 60.0).roundToLong() * 60L
        return Instant.ofEpochSecond(roundedMinSec)
    }
}