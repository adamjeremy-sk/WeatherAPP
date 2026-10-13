package com.weatherapp.libre.solar

import kotlin.math.acos

data class Observer(
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
    val elevationMeters: Double = 0.0,
    val pressureHpa: Double = 1013.25,
    val temperatureCelsius: Double = 10.0,
) {
    init {
        require((latitudeDegrees in -90.0..90.0)) { "Latitude must be between -90.0 and 90.0 degrees." }
        require((longitudeDegrees in -180.0..180.0)) { "Longitude must be between -180.0 and 180.0 degrees." }
    }

    /**
     * Horizon dip in degrees due to observer elevation above sea level.
     * Calculated using exact geometric formula:
     * dipDegrees = Math.toDegrees(Math.acos(earthRadiusMeters / (earthRadiusMeters + elevationMeters)))
     * Where earthRadiusMeters = 6,371,000.
     */
    val horizonDipDegrees: Double
        get() = if (elevationMeters > 0.0) {
            Math.toDegrees(acos(EARTH_RADIUS_METERS / (EARTH_RADIUS_METERS + elevationMeters)))
        } else 0.0

    companion object {
        private const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
