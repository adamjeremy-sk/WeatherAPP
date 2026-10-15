package com.weatherapp.libre

import java.time.Instant

data class SolarPhaseDetail(
    val stepIndex: Int,
    val name: String,
    val iconEmoji: String,
    val timeRangeText: String,
    val durationText: String,
    val elevationDescription: String,
    val isCurrentActivePhase: Boolean = false,
    val startInstant: Instant? = null,
    val endInstant: Instant? = null
)

fun SolarPhaseDetail.getDisplayName(context: android.content.Context): String {
    return when (name.lowercase()) {
        "night" -> context.getString(R.string.phase_night)
        "astronomical twilight" -> context.getString(R.string.phase_astronomical_twilight)
        "nautical twilight" -> context.getString(R.string.phase_nautical_twilight)
        "blue hour" -> context.getString(R.string.phase_blue_hour)
        "civil twilight" -> context.getString(R.string.phase_civil_twilight)
        "sunrise" -> context.getString(R.string.phase_sunrise)
        "golden hour" -> context.getString(R.string.phase_golden_hour)
        "daytime" -> context.getString(R.string.phase_daytime)
        "sunset" -> context.getString(R.string.phase_sunset)
        else -> name
    }
}
