package com.weatherapp.libre.solar

import kotlin.math.acos

data class Observer(
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
    val elevationMeters: Double = 0.0,
    val pressureHpa: Double = 1013.25,
    val temperatureCelsius: Double = 10.0
) {
    /**
     * Horizon dip in degrees due to observer elevation above sea level.
     * Calculated using exact geometric formula:
     * dipDegrees = Math.toDegrees(Math.acos(earthRadiusMeters / (earthRadiusMeters + elevationMeters)))
     * Where earthRadiusMeters = 6,371,000.
     */
    val horizonDipDegrees: Double
        get() = if (elevationMeters > 0.0) {
            Math.toDegrees(acos(6_371_000.0 / (6_371_000.0 + elevationMeters)))
        } else 0.0
}
