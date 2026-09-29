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
