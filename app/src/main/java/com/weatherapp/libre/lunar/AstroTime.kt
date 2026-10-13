package com.weatherapp.libre.lunar

import java.time.Instant
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * Utility functions for astronomical time conversions and timescale handling.
 *
 * Supported timescales:
 * - UTC (Coordinated Universal Time): Used for real-world timestamps and [Instant] instances.
 *   Parameters and return values representing Julian Date in UTC are denoted as `jdUtc` (or `jd`).
 * - TT (Terrestrial Time): Used for celestial dynamics and ephemeris calculations per Meeus.
 *   Parameters and return values representing Julian Ephemeris Day in TT are denoted as `jdeTT`.
 *
 * DeltaT is defined as DeltaT = TT - UT1. UT1 is approximated by UTC (|UT1 - UTC| < 0.9 s),
 * so conversion accuracy between UTC and TT is bounded by [DeltaT.estimateSeconds].
 */
object AstroTime {

    /**
     * Converts an [Instant] in UTC to Julian Date in UTC (`jdUtc`).
     *
     * @param instant The timestamp in UTC.
     * @return Julian Date in UTC (`jdUtc`).
     *
     * JD 2440587.5 corresponds to 1970-01-01T00:00:00Z.
     */
    fun instantToJdUtc(instant: Instant): Double {
        val epochSeconds = instant.epochSecond.toDouble() + (instant.nano.toDouble() / 1e9)
        return 2440587.5 + (epochSeconds / 86400.0)
    }

    /**
     * Converts a Julian Date in UTC (`jdUtc`) to an [Instant] in UTC.
     *
     * Uses [floor] and [roundToLong] to correctly handle negative epochs (pre-1970 timestamps).
     *
     * @param jdUtc Julian Date in UTC.
     * @return The corresponding [Instant] in UTC.
     */
    fun jdUtcToInstant(jdUtc: Double): Instant {
        val epochSeconds = (jdUtc - 2440587.5) * 86400.0
        val secs = floor(epochSeconds).toLong()
        val nanos = ((epochSeconds - secs) * 1e9).roundToLong()
        return Instant.ofEpochSecond(secs, nanos)
    }

    /**
     * Converts Julian Date in UTC (`jdUtc`) to Julian Ephemeris Day in TT (`jdeTT`).
     *
     * Approximates UT1 with UTC (|UT1 - UTC| < 0.9 s), applying DeltaT = TT - UT1 via [DeltaT.estimateSeconds].
     *
     * @param jdUtc Julian Date in UTC.
     * @return Julian Ephemeris Day in TT (`jdeTT`).
     */
    fun jdUtcToJdeTT(jdUtc: Double): Double {
        val year = 2000.0 + ((jdUtc - 2451545.0) / 365.25)
        val deltaTSeconds = DeltaT.estimateSeconds(year)
        return jdUtc + (deltaTSeconds / 86400.0)
    }

    /**
     * Converts Julian Ephemeris Day in TT (`jdeTT`) to Julian Date in UTC (`jdUtc`).
     *
     * Iterates 3 times starting from `jdUtc = jdeTT` to converge on the correct UTC year and DeltaT,
     * inverting [jdUtcToJdeTT]. Approximates UT1 with UTC (|UT1 - UTC| < 0.9 s), bounded by [DeltaT.estimateSeconds].
     *
     * @param jdeTT Julian Ephemeris Day in TT.
     * @return Converted Julian Date in UTC (`jdUtc`).
     */
    fun jdeTTToJdUtc(jdeTT: Double): Double {
        var jdUtc = jdeTT
        repeat(3) {
            val year = 2000.0 + ((jdUtc - 2451545.0) / 365.25)
            val deltaTSeconds = DeltaT.estimateSeconds(year)
            jdUtc = jdeTT - (deltaTSeconds / 86400.0)
        }
        return jdUtc
    }

    /**
     * Converts Julian Ephemeris Day in TT (`jdeTT`) to an [Instant] in UTC.
     *
     * Inverts TT to UTC via [jdeTTToJdUtc] before converting to [Instant].
     *
     * @param jdeTT Julian Ephemeris Day in TT.
     * @return The corresponding [Instant] in UTC.
     */
    fun jdeTTToInstant(jdeTT: Double): Instant {
        return jdUtcToInstant(jdeTTToJdUtc(jdeTT))
    }

    /**
     * Converts an [Instant] in UTC to Julian Ephemeris Day in TT (`jdeTT`).
     *
     * Converts [Instant] to UTC JD via [instantToJdUtc] then applies DeltaT shift to TT via [jdUtcToJdeTT].
     *
     * @param instant The timestamp in UTC.
     * @return Julian Ephemeris Day in TT (`jdeTT`).
     */
    fun instantToJdeTT(instant: Instant): Double {
        return jdUtcToJdeTT(instantToJdUtc(instant))
    }

    /**
     * Computes Julian Centuries T from Julian Ephemeris Day in TT (`jdeTT`) relative to J2000.0 (2451545.0 TT).
     *
     * This is the canonical implementation expecting JDE in TT per Meeus algorithms.
     *
     * @param jdeTT Julian Ephemeris Day in TT.
     * @return Julian Centuries T in TT.
     */
    fun julianCenturiesTT(jdeTT: Double): Double =
        (jdeTT - 2451545.0) / 36525.0

    /**
     * Computes Julian Centuries T relative to J2000.0 (2451545.0).
     *
     * New and edited code must use [julianCenturiesTT]; this function is retained only for existing call sites
     * and is scheduled for removal after per-module migration.
     *
     * @param jd Julian Date (or JDE).
     * @return Julian Centuries T.
     */
    fun julianCenturies(jd: Double): Double {
        return (jd - 2451545.0) / 36525.0
    }
}
