package com.weatherapp.libre.solar

import java.time.Instant

data class LightingDefinitions(
    val sunriseSunsetAltitudeDegrees: Double = -0.8333,
    val civilTwilightDegrees: Double = -6.0,
    val nauticalTwilightDegrees: Double = -12.0,
    val astronomicalTwilightDegrees: Double = -18.0,
    val goldenHourLowerBoundDegrees: Double = -4.0,
    val goldenHourUpperBoundDegrees: Double = 6.0,
    val blueHourLowerBoundDegrees: Double = -8.0,
    val blueHourUpperBoundDegrees: Double = -4.0,
) {
    companion object {
        val DEFAULT = LightingDefinitions()
    }
}

data class SolarEvent(
    val instantUtc: Instant,
    val azimuthDegrees: Double,
    val apparentAltitudeDegrees: Double,
)

data class SolarPhase(
    val startInstantUtc: Instant,
    val endInstantUtc: Instant,
    val startAzimuthDegrees: Double,
    val endAzimuthDegrees: Double,
    val lowerAltitudeDegrees: Double? = null,
    val upperAltitudeDegrees: Double? = null,
)

data class SolarLightingResult(
    val solarNoon: SolarEvent?,
    val sunrise: SolarEvent?,
    val sunset: SolarEvent?,
    val morningGoldenHour: SolarPhase?,
    val eveningGoldenHour: SolarPhase?,
    val morningBlueHour: SolarPhase?,
    val eveningBlueHour: SolarPhase?,
    val morningCivilTwilight: SolarPhase?,
    val morningNauticalTwilight: SolarPhase?,
    val morningAstronomicalTwilight: SolarPhase?,
    val eveningCivilTwilight: SolarPhase? = null,
    val eveningNauticalTwilight: SolarPhase? = null,
    val eveningAstronomicalTwilight: SolarPhase? = null,
)
