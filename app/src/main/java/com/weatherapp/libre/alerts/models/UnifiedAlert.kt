package com.weatherapp.libre.alerts.models

import androidx.annotation.Keep

@Keep
data class UnifiedAlert(
    val id: String,
    val source: AlertSource,
    val locationLat: Double,
    val locationLon: Double,
    val severity: AlertSeverity,
    val eventNative: String,
    val headline: String?,
    val description: String?,
    val instruction: String?,
    val onset: String?,
    val expires: String?,
    val areaDesc: String?
)

@Keep
enum class AlertSource { ECCC, METEOALARM, NWS, BOM }

@Keep
enum class AlertSeverity { UNKNOWN, INFO, YELLOW, ORANGE, RED }
