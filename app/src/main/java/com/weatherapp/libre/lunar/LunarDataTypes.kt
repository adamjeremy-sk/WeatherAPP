package com.weatherapp.libre.lunar

import java.time.Instant

enum class MoonPhaseName {
    NEW_MOON,
    FIRST_QUARTER,
    FULL_MOON,
    LAST_QUARTER,
}

enum class LunarPhaseCategory {
    NEW,
    WAXING_CRESCENT,
    FIRST_QUARTER,
    WAXING_GIBBOUS,
    FULL,
    WANING_GIBBOUS,
    LAST_QUARTER,
    WANING_CRESCENT,
}

enum class MoonEventType {
    MOONRISE,
    MOONSET,
}

data class MoonPhaseInfo(
    val instantUtc: Instant,
    val phaseAngleDegrees: Double,
    val illuminationFraction: Double,
    val ageDays: Double,
    val phaseCategory: LunarPhaseCategory,
    val geocentricDistanceKm: Double,
)

data class MoonPhaseEvent(
    val type: MoonPhaseName,
    val instantUtc: Instant,
)

data class MoonEvent(
    val type: MoonEventType,
    val instantUtc: Instant,
    val azimuthDegrees: Double,
    val altitudeDegrees: Double,
)

data class LunarGeocentricState(
    val instantUtc: Instant,
    val apparentGeocentricLongitudeDegrees: Double,
    val apparentGeocentricLatitudeDegrees: Double,
    val distanceKm: Double,
    val horizontalParallaxDegrees: Double,
    val apparentSemidiameterDegrees: Double,
    val rightAscensionDegrees: Double,
    val declinationDegrees: Double,
)

data class LunarTopocentricHorizontalState(
    val instantUtc: Instant,
    val altitudeDegrees: Double,
    val azimuthDegrees: Double,
    val topocentricRightAscensionDegrees: Double,
    val topocentricDeclinationDegrees: Double,
    val horizonThresholdDegrees: Double,
)
