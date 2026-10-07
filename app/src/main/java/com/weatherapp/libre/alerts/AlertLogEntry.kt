package com.weatherapp.libre.alerts

data class AlertLogEntry(
    val id: Long,
    val timestampUtc: Long,
    val locationName: String,
    val alertEvent: String,
    val severity: String,
    val isTest: Boolean
)
