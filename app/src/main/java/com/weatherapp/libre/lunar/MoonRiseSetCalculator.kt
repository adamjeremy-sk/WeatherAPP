package com.weatherapp.libre.lunar

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

object MoonRiseSetCalculator {

    /**
     * Finds all Moonrise and Moonset events for a given local date and observer.
     */
    fun moonEventsForLocalDate(
        localDate: LocalDate,
        zoneId: ZoneId,
        observer: Observer
    ): List<MoonEvent> {
        val localStartInstant = localDate.atStartOfDay(zoneId).toInstant()
        val localEndInstant = localDate.plusDays(1).atStartOfDay(zoneId).toInstant()

        val searchStartInstant = localStartInstant.minusSeconds(36 * 3600)
        val searchEndInstant = localEndInstant.plusSeconds(36 * 3600)

        val events = findRiseSetEventsInInterval(searchStartInstant, searchEndInstant, observer)

        val filtered = events.filter {
            !it.instantUtc.isBefore(localStartInstant) && it.instantUtc.isBefore(localEndInstant)
        }

        return filtered.sortedBy { it.instantUtc }
    }

    private fun computeTargetAltitude(observer: Observer, lunar: LunarPosition.LunarCoordinates): Double {
        val semidiameterDeg = lunar.semidiameterDeg
        val dipDeg = observer.horizonDipDegrees ?: 0.0
        return -semidiameterDeg - dipDeg
    }

    private fun getApparentAltitudeAt(instant: Instant, observer: Observer): Pair<Double, TopocentricConverter.TopocentricPosition> {
        val jdUtc = AstroTime.instantToJdUtc(instant)
        val jdeTT = AstroTime.jdUtcToJdeTT(jdUtc)
        val lunar = LunarPosition.compute(jdeTT)
        val topo = TopocentricConverter.computeTopocentric(lunar, observer, jdUtc, jdeTT)
        val targetAlt = computeTargetAltitude(observer, lunar)
        val fVal = topo.apparentAltitudeDegrees - targetAlt
        return Pair(fVal, topo)
    }

    private fun findRiseSetEventsInInterval(
        startInstant: Instant,
        endInstant: Instant,
        observer: Observer
    ): List<MoonEvent> {
        val stepSeconds = 600L
        val totalSecs = endInstant.epochSecond - startInstant.epochSecond

        val samples = mutableListOf<Triple<Instant, Double, TopocentricConverter.TopocentricPosition>>()

        var curSec = 0L
        while (curSec <= totalSecs) {
            val inst = startInstant.plusSeconds(curSec)
            val (fVal, topo) = getApparentAltitudeAt(inst, observer)
            samples.add(Triple(inst, fVal, topo))
            curSec += stepSeconds
        }

        val foundEvents = mutableListOf<MoonEvent>()

        for (i in 0 until samples.size - 1) {
            val (t1, f1, _) = samples[i]
            val (t2, f2, _) = samples[i + 1]

            if (f1 * f2 <= 0.0) {
                val eventType = if (f1 < f2) MoonEventType.MOONRISE else MoonEventType.MOONSET
                val rootInstant = solveRootBrent(t1, t2, f1, f2, observer)
                val (_, topoRoot) = getApparentAltitudeAt(rootInstant, observer)

                foundEvents.add(
                    MoonEvent(
                        type = eventType,
                        instantUtc = rootInstant,
                        azimuthDegrees = topoRoot.azimuthDegrees,
                        altitudeDegrees = topoRoot.apparentAltitudeDegrees,
                        observer = observer
                    )
                )
            }
        }

        val deduplicated = mutableListOf<MoonEvent>()
        for (event in foundEvents) {
            if (deduplicated.none { abs(it.instantUtc.epochSecond - event.instantUtc.epochSecond) < 30 && it.type == event.type }) {
                deduplicated.add(event)
            }
        }

        return deduplicated
    }

    private fun solveRootBrent(
        t1: Instant,
        t2: Instant,
        f1In: Double,
        f2In: Double,
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

        var iterations = 0
        while (abs(b - a) > toleranceSeconds && iterations < 50) {
            iterations++

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

            val instS = Instant.ofEpochSecond(s.toLong(), ((s - s.toLong()) * 1e9).toLong())
            val fs = getApparentAltitudeAt(instS, observer).first

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

        val sec = b.toLong()
        val nsec = ((b - sec) * 1e9).toLong()
        return Instant.ofEpochSecond(sec, nsec)
    }
}
