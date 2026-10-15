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
    val phaseCategory: com.weatherapp.libre.lunar.LunarPhaseCategory? = null,
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

fun DarkSkyWindow.getLocalizedQualityLabel(context: android.content.Context): String {
    return when (qualityLabel) {
        "Excellent" -> context.getString(R.string.rating_excellent)
        "Good" -> context.getString(R.string.rating_good)
        "Moderate" -> context.getString(R.string.rating_moderate)
        "Poor" -> context.getString(R.string.rating_poor)
        else -> qualityLabel
    }
}

fun DarkSkyWindow.getLocalizedMoonStatus(context: android.content.Context): String {
    return if (moonStatus == "Moon below horizon") {
        context.getString(R.string.label_moon_below_horizon)
    } else if (moonStatus.startsWith("Moon above horizon")) {
        val pct = moonStatus.substringAfter("(").substringBefore("%").trim()
        val pctInt = pct.toIntOrNull() ?: 0
        "${context.getString(R.string.label_moon_above_horizon)} (${context.getString(R.string.label_percent_illuminated, pctInt)})"
    } else {
        moonStatus
    }
}

fun VisibilityPeriod.getLocalizedCategory(context: android.content.Context): String {
    return when (category) {
        "Bright Night" -> context.getString(R.string.vis_bright_night)
        "Moderate Light" -> context.getString(R.string.vis_moderate_light)
        "Low Light" -> context.getString(R.string.vis_low_light)
        "Minimal Light" -> context.getString(R.string.vis_minimal_light)
        "No Natural Light" -> context.getString(R.string.vis_no_natural_light)
        else -> category
    }
}

fun VisibilityPeriod.getLocalizedTwilightStatus(context: android.content.Context): String {
    return when (twilightStatus) {
        "Civil Twilight" -> context.getString(R.string.phase_civil_twilight)
        "Nautical Twilight" -> context.getString(R.string.phase_nautical_twilight)
        "Astronomical Twilight" -> context.getString(R.string.phase_astronomical_twilight)
        "Astronomical Night" -> context.getString(R.string.phase_night)
        else -> twilightStatus
    }
}

fun VisibilityPeriod.getLocalizedCloudCoverStatus(context: android.content.Context): String? {
    return cloudCoverStatus?.replace("Cloud Cover", context.getString(R.string.label_cloud_cover))
}

fun SolunarPeriod.getLocalizedCenterEvent(context: android.content.Context): String {
    return when (centerEvent) {
        "Transit" -> context.getString(R.string.label_transit)
        "Nadir" -> context.getString(R.string.label_nadir)
        "Moonrise" -> context.getString(R.string.label_moonrise)
        "Moonset" -> context.getString(R.string.label_moonset)
        else -> centerEvent
    }
}

fun SolunarDaySummary.getLocalizedRatingLabel(context: android.content.Context): String {
    return when (ratingLabel) {
        "Excellent" -> context.getString(R.string.rating_excellent)
        "Good" -> context.getString(R.string.rating_good)
        "Fair", "Moderate" -> context.getString(R.string.rating_moderate)
        "Poor" -> context.getString(R.string.rating_poor)
        else -> ratingLabel
    }
}
