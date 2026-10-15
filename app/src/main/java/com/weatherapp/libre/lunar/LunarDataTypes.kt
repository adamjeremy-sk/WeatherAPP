package com.weatherapp.libre.lunar

import java.time.Instant

enum class MoonPhaseName {
    NEW_MOON,
    FIRST_QUARTER,
    FULL_MOON,
    LAST_QUARTER,
}

fun MoonPhaseName.getNameRes(): Int = when (this) {
    MoonPhaseName.NEW_MOON -> com.weatherapp.libre.R.string.phase_new_moon
    MoonPhaseName.FIRST_QUARTER -> com.weatherapp.libre.R.string.phase_first_quarter
    MoonPhaseName.FULL_MOON -> com.weatherapp.libre.R.string.phase_full_moon
    MoonPhaseName.LAST_QUARTER -> com.weatherapp.libre.R.string.phase_last_quarter
}

fun MoonPhaseName.getEmoji(): String = when (this) {
    MoonPhaseName.NEW_MOON -> "🌑"
    MoonPhaseName.FIRST_QUARTER -> "🌓"
    MoonPhaseName.FULL_MOON -> "🌕"
    MoonPhaseName.LAST_QUARTER -> "🌗"
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

fun LunarPhaseCategory.getNameRes(): Int = when (this) {
    LunarPhaseCategory.NEW -> com.weatherapp.libre.R.string.phase_new_moon
    LunarPhaseCategory.WAXING_CRESCENT -> com.weatherapp.libre.R.string.phase_waxing_crescent
    LunarPhaseCategory.FIRST_QUARTER -> com.weatherapp.libre.R.string.phase_first_quarter
    LunarPhaseCategory.WAXING_GIBBOUS -> com.weatherapp.libre.R.string.phase_waxing_gibbous
    LunarPhaseCategory.FULL -> com.weatherapp.libre.R.string.phase_full_moon
    LunarPhaseCategory.WANING_GIBBOUS -> com.weatherapp.libre.R.string.phase_waning_gibbous
    LunarPhaseCategory.LAST_QUARTER -> com.weatherapp.libre.R.string.phase_last_quarter
    LunarPhaseCategory.WANING_CRESCENT -> com.weatherapp.libre.R.string.phase_waning_crescent
}

fun LunarPhaseCategory.getEmoji(): String = when (this) {
    LunarPhaseCategory.NEW -> "🌑"
    LunarPhaseCategory.WAXING_CRESCENT -> "🌒"
    LunarPhaseCategory.FIRST_QUARTER -> "🌓"
    LunarPhaseCategory.WAXING_GIBBOUS -> "🌔"
    LunarPhaseCategory.FULL -> "🌕"
    LunarPhaseCategory.WANING_GIBBOUS -> "🌖"
    LunarPhaseCategory.LAST_QUARTER -> "🌗"
    LunarPhaseCategory.WANING_CRESCENT -> "🌘"
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
