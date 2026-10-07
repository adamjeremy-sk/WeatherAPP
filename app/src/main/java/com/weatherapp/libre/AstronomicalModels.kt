package com.weatherapp.libre

import java.time.Instant
import java.time.LocalDate

data class DarkSkyWindow(
    val startUtc: Instant,
    val endUtc: Instant,
    val durationMinutes: Int,
    val qualityLabel: String,
    val interferenceIndex: Int,
    val moonStatus: String
)

data class DarkSkyDaySummary(
    val date: LocalDate,
    val windows: List<DarkSkyWindow>,
    val note: String? = null
)

data class VisibilityPeriod(
    val startUtc: Instant,
    val endUtc: Instant,
    val category: String,
    val moonIllumination: Double,
    val moonAltitude: Double,
    val twilightStatus: String,
    val cloudCoverStatus: String?
)

data class NightVisibilityDaySummary(
    val date: LocalDate,
    val periods: List<VisibilityPeriod>,
    val cloudCoverAvailable: Boolean,
    val note: String? = null
)

data class SolunarPeriod(
    val startUtc: Instant,
    val endUtc: Instant,
    val type: String,        // "Major" or "Minor"
    val centerEvent: String  // "Transit", "Nadir", "Moonrise", "Moonset"
)

data class SolunarDaySummary(
    val date: LocalDate,
    val phaseName: String,
    val illumination: Double,
    val moonrise: Instant?,
    val moonset: Instant?,
    val transit: Instant?,
    val nadir: Instant?,
    val majorPeriods: List<SolunarPeriod>,
    val minorPeriods: List<SolunarPeriod>,
    val rating: Int,
    val ratingLabel: String
)
