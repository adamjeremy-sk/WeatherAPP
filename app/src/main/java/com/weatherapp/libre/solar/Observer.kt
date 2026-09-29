package com.weatherapp.libre.solar

data class Observer(
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
    val elevationMeters: Double = 0.0,
    val pressureHpa: Double = 1013.25,
    val temperatureCelsius: Double = 10.0
) {
    /**
     * Horizon dip in degrees due to observer elevation above sea level.
     * Dip (degrees) ~ (1.76 / 60.0) * sqrt(elevationMeters)
     */
    val horizonDipDegrees: Double
        get() = if (elevationMeters > 0.0) (1.76 / 60.0) * kotlin.math.sqrt(elevationMeters) else 0.0
}
