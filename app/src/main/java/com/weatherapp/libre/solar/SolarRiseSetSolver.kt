package com.weatherapp.libre.solar

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

object SolarRiseSetSolver {

    data class CrossingEvent(
        val instantUtc: Instant,
        val isRising: Boolean,
        val azimuthDegrees: Double,
        val apparentAltitudeDegrees: Double
    )

    fun solve(
        localDate: LocalDate,
        zoneId: ZoneId,
        observer: Observer,
        definitions: LightingDefinitions = LightingDefinitions.DEFAULT
    ): SolarLightingResult {
        val localStartInstant = localDate.atStartOfDay(zoneId).toInstant()
        val localEndInstant = localDate.plusDays(1).atStartOfDay(zoneId).toInstant()

        // 72-hour search window
        val searchStartInstant = localStartInstant.minusSeconds(24 * 3600)
        val searchEndInstant = localEndInstant.plusSeconds(24 * 3600)

        val stepSeconds = 600L
        val totalSeconds = searchEndInstant.epochSecond - searchStartInstant.epochSecond

        val samples = mutableListOf<SolarPositionEngine.TopocentricSolarPosition>()
        var curSec = 0L
        while (curSec <= totalSeconds) {
            val inst = searchStartInstant.plusSeconds(curSec)
            samples.add(SolarPositionEngine.computeTopocentric(inst, observer))
            curSec += stepSeconds
        }

        val solarNoon = findSolarNoon(localStartInstant, localEndInstant, observer)

        val crossingCache = mutableMapOf<Double, List<CrossingEvent>>()
        fun getCrossings(target: Double): List<CrossingEvent> =
            crossingCache.getOrPut(target) { findAllCrossings(samples, target, observer) }

        val sunriseSunsetTargetAlt = definitions.sunriseSunsetAltitudeDegrees

        val sunriseEvent = findSingleCrossingForDate(
            ::getCrossings, localStartInstant, localEndInstant, sunriseSunsetTargetAlt, isRisingTarget = true
        )
        val sunsetEvent = findSingleCrossingForDate(
            ::getCrossings, localStartInstant, localEndInstant, sunriseSunsetTargetAlt, isRisingTarget = false
        )

        // Morning & Evening Twilights
        val civilPhase = buildTwilightPhase(::getCrossings, localStartInstant, localEndInstant, definitions.civilTwilightDegrees, sunriseSunsetTargetAlt)
        val nauticalPhase = buildTwilightPhase(::getCrossings, localStartInstant, localEndInstant, definitions.nauticalTwilightDegrees, definitions.civilTwilightDegrees)
        val astronomicalPhase = buildTwilightPhase(::getCrossings, localStartInstant, localEndInstant, definitions.astronomicalTwilightDegrees, definitions.nauticalTwilightDegrees)

        val eveningCivil = buildEveningPhase(::getCrossings, localStartInstant, localEndInstant, definitions.civilTwilightDegrees, sunriseSunsetTargetAlt)
        val eveningNautical = buildEveningPhase(::getCrossings, localStartInstant, localEndInstant, definitions.nauticalTwilightDegrees, definitions.civilTwilightDegrees)
        val eveningAstronomical = buildEveningPhase(::getCrossings, localStartInstant, localEndInstant, definitions.astronomicalTwilightDegrees, definitions.nauticalTwilightDegrees)

        // Golden Hour
        val morningGolden = buildMorningPhase(::getCrossings, localStartInstant, localEndInstant, definitions.goldenHourLowerBoundDegrees, definitions.goldenHourUpperBoundDegrees)
        val eveningGolden = buildEveningPhase(::getCrossings, localStartInstant, localEndInstant, definitions.goldenHourLowerBoundDegrees, definitions.goldenHourUpperBoundDegrees)

        // Blue Hour
        val morningBlue = buildMorningPhase(::getCrossings, localStartInstant, localEndInstant, definitions.blueHourLowerBoundDegrees, definitions.blueHourUpperBoundDegrees)
        val eveningBlue = buildEveningPhase(::getCrossings, localStartInstant, localEndInstant, definitions.blueHourLowerBoundDegrees, definitions.blueHourUpperBoundDegrees)

        return SolarLightingResult(
            solarNoon = solarNoon,
            sunrise = sunriseEvent,
            sunset = sunsetEvent,
            morningGoldenHour = morningGolden,
            eveningGoldenHour = eveningGolden,
            morningBlueHour = morningBlue,
            eveningBlueHour = eveningBlue,
            morningCivilTwilight = civilPhase,
            morningNauticalTwilight = nauticalPhase,
            morningAstronomicalTwilight = astronomicalPhase,
            eveningCivilTwilight = eveningCivil,
            eveningNauticalTwilight = eveningNautical,
            eveningAstronomicalTwilight = eveningAstronomical
        )
    }

    private fun findSolarNoon(
        localStartInstant: Instant,
        localEndInstant: Instant,
        observer: Observer
    ): SolarEvent? {
        val totalSecs = localEndInstant.epochSecond - localStartInstant.epochSecond
        var bestPos = SolarPositionEngine.computeTopocentric(localStartInstant, observer)

        var curSec = 0L
        while (curSec <= totalSecs) {
            val inst = localStartInstant.plusSeconds(curSec)
            val pos = SolarPositionEngine.computeTopocentric(inst, observer)
            if (pos.apparentAltitudeDegrees > bestPos.apparentAltitudeDegrees) {
                bestPos = pos
            }
            curSec += 300L
        }

        var a = bestPos.instantUtc.minusSeconds(600).epochSecond.toDouble()
        var b = bestPos.instantUtc.plusSeconds(600).epochSecond.toDouble()

        val gr = (kotlin.math.sqrt(5.0) - 1.0) / 2.0
        var c = b - gr * (b - a)
        var d = a + gr * (b - a)

        var iter = 0
        while (abs(b - a) > 0.5 && iter < 30) {
            iter++
            val posC = SolarPositionEngine.computeTopocentric(Instant.ofEpochSecond(c.toLong()), observer)
            val posD = SolarPositionEngine.computeTopocentric(Instant.ofEpochSecond(d.toLong()), observer)

            if (posC.apparentAltitudeDegrees > posD.apparentAltitudeDegrees) {
                b = d
                d = c
                c = b - gr * (b - a)
            } else {
                a = c
                c = d
                d = a + gr * (b - a)
            }
        }

        val noonSec = (a + b) / 2.0
        val noonInstant = Instant.ofEpochSecond(noonSec.toLong())
        val noonPos = SolarPositionEngine.computeTopocentric(noonInstant, observer)

        return SolarEvent(
            instantUtc = noonInstant,
            azimuthDegrees = noonPos.azimuthDegrees,
            apparentAltitudeDegrees = noonPos.apparentAltitudeDegrees
        )
    }

    private fun findSingleCrossingForDate(
        getCrossings: (Double) -> List<CrossingEvent>,
        localStartInstant: Instant,
        localEndInstant: Instant,
        targetAltitude: Double,
        isRisingTarget: Boolean
    ): SolarEvent? {
        val allCrossings = getCrossings(targetAltitude)
        val matches = allCrossings.filter {
            it.isRising == isRisingTarget &&
                    !it.instantUtc.isBefore(localStartInstant) &&
                    it.instantUtc.isBefore(localEndInstant)
        }
        val best = matches.minByOrNull { abs(it.instantUtc.epochSecond - (localStartInstant.epochSecond + 43200)) }
            ?: matches.firstOrNull()

        return best?.let {
            SolarEvent(
                instantUtc = it.instantUtc,
                azimuthDegrees = it.azimuthDegrees,
                apparentAltitudeDegrees = it.apparentAltitudeDegrees
            )
        }
    }

    private fun buildMorningPhase(
        getCrossings: (Double) -> List<CrossingEvent>,
        localStartInstant: Instant,
        localEndInstant: Instant,
        lowerTarget: Double,
        upperTarget: Double
    ): SolarPhase? {
        val lowerRising = getCrossings(lowerTarget)
            .filter { it.isRising && !it.instantUtc.isBefore(localStartInstant) && it.instantUtc.isBefore(localEndInstant) }
        val upperRising = getCrossings(upperTarget)
            .filter { it.isRising && !it.instantUtc.isBefore(localStartInstant) && it.instantUtc.isBefore(localEndInstant) }

        val start = lowerRising.firstOrNull() ?: return null
        val end = upperRising.find { it.instantUtc.isAfter(start.instantUtc) } ?: return null

        return SolarPhase(
            startInstantUtc = start.instantUtc,
            endInstantUtc = end.instantUtc,
            startAzimuthDegrees = start.azimuthDegrees,
            endAzimuthDegrees = end.azimuthDegrees,
            lowerAltitudeDegrees = lowerTarget,
            upperAltitudeDegrees = upperTarget
        )
    }

    private fun buildEveningPhase(
        getCrossings: (Double) -> List<CrossingEvent>,
        localStartInstant: Instant,
        localEndInstant: Instant,
        lowerTarget: Double,
        upperTarget: Double
    ): SolarPhase? {
        val upperFalling = getCrossings(upperTarget)
            .filter { !it.isRising && !it.instantUtc.isBefore(localStartInstant) && it.instantUtc.isBefore(localEndInstant) }
        val lowerFalling = getCrossings(lowerTarget)
            .filter { !it.isRising && !it.instantUtc.isBefore(localStartInstant) && it.instantUtc.isBefore(localEndInstant) }

        val start = upperFalling.firstOrNull() ?: return null
        val end = lowerFalling.find { it.instantUtc.isAfter(start.instantUtc) } ?: return null

        return SolarPhase(
            startInstantUtc = start.instantUtc,
            endInstantUtc = end.instantUtc,
            startAzimuthDegrees = start.azimuthDegrees,
            endAzimuthDegrees = end.azimuthDegrees,
            lowerAltitudeDegrees = lowerTarget,
            upperAltitudeDegrees = upperTarget
        )
    }

    private fun buildTwilightPhase(
        getCrossings: (Double) -> List<CrossingEvent>,
        localStartInstant: Instant,
        localEndInstant: Instant,
        lowerTarget: Double,
        upperTarget: Double
    ): SolarPhase? {
        return buildMorningPhase(getCrossings, localStartInstant, localEndInstant, lowerTarget, upperTarget)
    }

    private fun findAllCrossings(
        samples: List<SolarPositionEngine.TopocentricSolarPosition>,
        targetAltitude: Double,
        observer: Observer
    ): List<CrossingEvent> {
        val events = mutableListOf<CrossingEvent>()
        val adjustedTarget = targetAltitude - observer.horizonDipDegrees

        for (i in 0 until samples.size - 1) {
            val s1 = samples[i]
            val s2 = samples[i + 1]

            val f1 = s1.apparentAltitudeDegrees - adjustedTarget
            val f2 = s2.apparentAltitudeDegrees - adjustedTarget

            if (f1 * f2 <= 0.0) {
                val isRising = f1 < f2
                val rootInstant = solveRootBrent(s1.instantUtc, s2.instantUtc, f1, f2, adjustedTarget, observer)
                val pos = SolarPositionEngine.computeTopocentric(rootInstant, observer)

                events.add(
                    CrossingEvent(
                        instantUtc = rootInstant,
                        isRising = isRising,
                        azimuthDegrees = pos.azimuthDegrees,
                        apparentAltitudeDegrees = pos.apparentAltitudeDegrees
                    )
                )
            }
        }

        return events.distinctBy { Pair(it.isRising, it.instantUtc.epochSecond / 10) }.sortedBy { it.instantUtc }
    }

    private fun solveRootBrent(
        t1: Instant,
        t2: Instant,
        f1In: Double,
        f2In: Double,
        targetAltitude: Double,
        observer: Observer,
        toleranceSeconds: Double = 0.5
    ): Instant {
        var a = t1.epochSecond.toDouble() + t1.nano / 1e9
        var b = t2.epochSecond.toDouble() + t2.nano / 1e9
        var fa = f1In
        var fb = f2In

        if (abs(fa) < abs(fb)) {
            var tmp = a; a = b; b = tmp
            tmp = fa; fa = fb; fb = tmp
        }

        var c = a
        var fc = fa
        var mflag = true
        var d = 0.0

        var iter = 0
        while (abs(b - a) > toleranceSeconds && iter < 50) {
            iter++

            var s: Double
            if (fa != fc && fb != fc) {
                s = (a * fb * fc) / ((fa - fb) * (fa - fc)) +
                    (b * fa * fc) / ((fb - fa) * (fb - fc)) +
                    (c * fa * fb) / ((fc - fa) * (fc - fb))
            } else {
                s = b - fb * (b - a) / (fb - fa)
            }

            val cond1 = (s < (3 * a + b) / 4 || s > b) && (s < b || s > (3 * a + b) / 4)
            val cond2 = mflag && abs(s - b) >= abs(b - c) / 2
            val cond3 = !mflag && abs(s - b) >= abs(c - d) / 2
            val cond4 = mflag && abs(b - c) < toleranceSeconds
            val cond5 = !mflag && abs(c - d) < toleranceSeconds

            if (cond1 || cond2 || cond3 || cond4 || cond5) {
                s = (a + b) / 2.0
                mflag = true
            } else {
                mflag = false
            }

            val secs = floor(s).toLong()
            val nanos = ((s - secs) * 1e9).roundToLong()
            val instS = Instant.ofEpochSecond(secs, nanos)
            val posS = SolarPositionEngine.computeTopocentric(instS, observer)
            val fs = posS.apparentAltitudeDegrees - targetAltitude

            d = c
            c = b
            fc = fb

            if (fa * fs < 0) {
                b = s
                fb = fs
            } else {
                a = s
                fa = fs
            }

            if (abs(fa) < abs(fb)) {
                val tmpA = a; a = b; b = tmpA
                val tmpF = fa; fa = fb; fb = tmpF
            }
        }

        val secs = floor(b).toLong()
        val nanos = ((b - secs) * 1e9).roundToLong()
        return Instant.ofEpochSecond(secs, nanos)
    }
}
