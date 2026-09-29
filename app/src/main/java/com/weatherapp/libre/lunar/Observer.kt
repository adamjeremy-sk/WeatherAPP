package com.weatherapp.libre.lunar

data class Observer(
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
    val elevationMeters: Double? = null,
    val pressureHpa: Double? = null,
    val temperatureCelsius: Double? = null,
    val horizonDipDegrees: Double? = null
) {
    val effectiveElevationMeters: Double
        get() = elevationMeters ?: 0.0

    val effectivePressureHpa: Double
        get() = pressureHpa ?: 1013.25

    val effectiveTemperatureCelsius: Double
        get() = temperatureCelsius ?: 10.0
}
