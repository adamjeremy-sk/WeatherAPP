package com.weatherapp.libre.lunar

import kotlin.math.*

object TopocentricConverter {

    data class TopocentricPosition(
        val topocentricRaDeg: Double,
        val topocentricDecDeg: Double,
        val hourAngleDeg: Double,
        val azimuthDegrees: Double,       // True North = 0°, East = 90°
        val trueAltitudeDegrees: Double,  // Geometric altitude above horizon
        val apparentAltitudeDegrees: Double // Refracted altitude
    )

    fun topocentricHorizontalState(
        instantUtc: java.time.Instant,
        observer: Observer
    ): LunarTopocentricHorizontalState {
        val jdUtc = AstroTime.instantToJdUtc(instantUtc)
        val jdeTT = AstroTime.jdUtcToJdeTT(jdUtc)
        val lunar = LunarPosition.compute(jdeTT)
        val topo = computeTopocentric(lunar, observer, jdUtc, jdeTT)
        val h0 = (0.7275 * lunar.horizontalParallaxDeg) - 0.5667

        return LunarTopocentricHorizontalState(
            instantUtc = instantUtc,
            altitudeDegrees = topo.apparentAltitudeDegrees,
            azimuthDegrees = topo.azimuthDegrees,
            topocentricRightAscensionDegrees = topo.topocentricRaDeg,
            topocentricDeclinationDegrees = topo.topocentricDecDeg,
            horizonThresholdDegrees = h0
        )
    }

    /**
     * Converts geocentric lunar coordinates (RA, Dec, Parallax, Distance)
     * to topocentric coordinates for an observer on the WGS84 ellipsoid.
     * Meeus Ch. 40 (Topocentric Coordinates) & Ch. 13 (Horizontal Coordinates).
     */
    fun computeTopocentric(
        lunar: LunarPosition.LunarCoordinates,
        observer: Observer,
        jdUtc: Double,
        jdeTT: Double
    ): TopocentricPosition {
        val latRad = AstronomyMath.degToRad(observer.latitudeDegrees)
        val lonRad = AstronomyMath.degToRad(observer.longitudeDegrees)
        val elevKm = observer.effectiveElevationMeters / 1000.0

        val a = 6378.137 // Earth equatorial radius km
        val f = 1.0 / 298.257223563
        val b = a * (1.0 - f)
        val e2 = (a * a - b * b) / (a * a)

        val sinLat = sin(latRad)
        val cosLat = cos(latRad)

        val N = a / sqrt(1.0 - e2 * sinLat * sinLat)
        val rhoSinPhiDash = (N * (1.0 - e2) + elevKm) * sinLat / a
        val rhoCosPhiDash = (N + elevKm) * cosLat / a

        val gastDeg = AstronomyMath.gastDegrees(jdUtc, jdeTT)
        val lastDeg = AstronomyMath.normalizeDegrees(gastDeg + observer.longitudeDegrees)

        val HRad = AstronomyMath.degToRad(AstronomyMath.normalizeDegrees(lastDeg - lunar.raDeg))
        val decRad = AstronomyMath.degToRad(lunar.decDeg)
        val piRad = AstronomyMath.degToRad(lunar.horizontalParallaxDeg)

        val sinPi = sin(piRad)

        val deltaRaRad = atan2(
            -rhoCosPhiDash * sinPi * sin(HRad),
            cos(decRad) - rhoCosPhiDash * sinPi * cos(HRad)
        )

        val topocentricRaDeg = AstronomyMath.normalizeDegrees(lunar.raDeg + AstronomyMath.radToDeg(deltaRaRad))
        val topocentricHRad = HRad - deltaRaRad

        val sinTopocentricDec = (sin(decRad) - rhoSinPhiDash * sinPi) * cos(deltaRaRad) /
                (cos(decRad) - rhoCosPhiDash * sinPi * cos(HRad))
        val topocentricDecRad = asin(sinTopocentricDec.coerceIn(-1.0, 1.0))
        val topocentricDecDeg = AstronomyMath.radToDeg(topocentricDecRad)

        val sinAlt = sin(latRad) * sin(topocentricDecRad) + cos(latRad) * cos(topocentricDecRad) * cos(topocentricHRad)
        val trueAltRad = asin(sinAlt.coerceIn(-1.0, 1.0))
        val trueAltDeg = AstronomyMath.radToDeg(trueAltRad)

        val y = sin(topocentricHRad)
        val x = cos(topocentricHRad) * sin(latRad) - tan(topocentricDecRad) * cos(latRad)
        val azimuthFromSouthRad = atan2(y, x)
        val azimuthFromNorthDeg = AstronomyMath.normalizeDegrees(AstronomyMath.radToDeg(azimuthFromSouthRad) + 180.0)

        val refractionDeg = AtmosphericRefraction.calculateRefractionDegrees(
            trueAltitudeDeg = trueAltDeg,
            pressureHpa = observer.effectivePressureHpa,
            temperatureCelsius = observer.effectiveTemperatureCelsius
        )

        val apparentAltDeg = trueAltDeg + refractionDeg

        return TopocentricPosition(
            topocentricRaDeg = topocentricRaDeg,
            topocentricDecDeg = topocentricDecDeg,
            hourAngleDeg = AstronomyMath.normalizeDegrees(AstronomyMath.radToDeg(topocentricHRad)),
            azimuthDegrees = azimuthFromNorthDeg,
            trueAltitudeDegrees = trueAltDeg,
            apparentAltitudeDegrees = apparentAltDeg
        )
    }
}
