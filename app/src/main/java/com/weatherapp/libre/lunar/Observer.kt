package com.weatherapp.libre.lunar

import kotlin.math.sqrt

/**
 * Represents an observer location on Earth for astronomical calculations.
 *
 * @property latitudeDegrees Geodetic latitude in degrees (-90 to +90).
 * @property longitudeDegrees Longitude in degrees (-180 to +180), where East is positive and West is negative.
 * @property elevationMeters Observer elevation above sea level in meters (optional, default 0.0 m).
 * @property pressureHpa Atmospheric pressure in hPa (optional, default 1013.25 hPa).
 * @property temperatureCelsius Temperature in degrees Celsius (optional, default 10.0 °C).
 * @property horizonDipDegrees Explicit horizon dip in degrees (optional).
 */
data class Observer(
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
    val elevationMeters: Double? = null,
    val pressureHpa: Double? = null,
    val temperatureCelsius: Double? = null,
    val horizonDipDegrees: Double? = null,
) {
    init {
        require((latitudeDegrees in -90.0..90.0)) { "Latitude must be between -90.0 and 90.0 degrees." }
        require((longitudeDegrees in -180.0..180.0)) { "Longitude must be between -180.0 and 180.0 degrees." }
    }

    val effectiveElevationMeters: Double
        get() = elevationMeters ?: 0.0

    val effectivePressureHpa: Double
        get() = pressureHpa ?: 1013.25

    val effectiveTemperatureCelsius: Double
        get() = temperatureCelsius ?: 10.0

    /**
     * The effective horizon dip in degrees.
     * Uses the explicitly provided [horizonDipDegrees] if available.
     * Otherwise, calculates the geometric dip of the horizon based on
     * [effectiveElevationMeters] using the standard approximation:
     * 1.76 * sqrt(height_m) / 60.0.
     * Returns 0.0 for elevations at or below sea level.
     */
    val effectiveHorizonDipDegrees: Double
        get() = horizonDipDegrees ?: (1.76 * sqrt(effectiveElevationMeters.coerceAtLeast(0.0)) / 60.0)
}
