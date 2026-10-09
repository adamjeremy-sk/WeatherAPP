package com.weatherapp.libre.alerts

import androidx.annotation.Keep

@Keep
data class AlertLogEntry(
    val id: Long,
    val timestampUtc: Long,
    val locationName: String,
    val alertEvent: String,
    val severity: String,
    val isTest: Boolean
)
