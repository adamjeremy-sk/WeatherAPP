package com.weatherapp.libre.lunar

import java.time.Instant

object AstroTime {

    /**
     * Converts an Instant (UTC) to Julian Date in UTC.
     * JD 2440587.5 = 1970-01-01T00:00:00Z
     */
    fun instantToJdUtc(instant: Instant): Double {
        val epochSeconds = instant.epochSecond.toDouble() + instant.nano.toDouble() / 1e9
        return 2440587.5 + epochSeconds / 86400.0
    }

    /**
     * Converts a Julian Date in UTC to Instant.
     */
    fun jdUtcToInstant(jdUtc: Double): Instant {
        val epochSeconds = (jdUtc - 2440587.5) * 86400.0
        val secs = epochSeconds.toLong()
        var nanos = ((epochSeconds - secs) * 1e9).toLong()
        if (nanos < 0) {
            nanos += 1_000_000_000L
        }
        return Instant.ofEpochSecond(secs, nanos)
    }

    /**
     * Converts Julian Date in UTC to Julian Ephemeris Day (JDE, in TT).
     */
    fun jdUtcToJdeTT(jdUtc: Double): Double {
        val year = 2000.0 + (jdUtc - 2451545.0) / 365.25
        val deltaTSeconds = DeltaT.estimateSeconds(year)
        return jdUtc + deltaTSeconds / 86400.0
    }

    /**
     * Converts JDE (in TT) to Julian Date in UTC.
     */
    fun jdeTTToJdUtc(jdeTT: Double): Double {
        val year = 2000.0 + (jdeTT - 2451545.0) / 365.25
        val deltaTSeconds = DeltaT.estimateSeconds(year)
        return jdeTT - deltaTSeconds / 86400.0
    }

    fun jdeTTToInstant(jdeTT: Double): Instant {
        return jdUtcToInstant(jdeTTToJdUtc(jdeTT))
    }

    fun instantToJdeTT(instant: Instant): Double {
        return jdUtcToJdeTT(instantToJdUtc(instant))
    }

    /**
     * Computes Julian Centuries T from JDE or JD (relative to J2000.0 = 2451545.0).
     */
    fun julianCenturies(jd: Double): Double {
        return (jd - 2451545.0) / 36525.0
    }
}
