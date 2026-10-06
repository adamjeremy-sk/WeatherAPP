package com.weatherapp.libre

import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object UnitUtils {

    fun formatTemp(celsius: Double, unitSystem: UnitSystem): String {
        return if (unitSystem == UnitSystem.US_CUSTOMARY) {
            val fahrenheit = celsius * 9 / 5 + 32
            "${kotlin.math.round(fahrenheit).toInt()}°F"
        } else {
            "${kotlin.math.round(celsius).toInt()}°C"
        }
    }

    fun formatTempPrecise(celsius: Double, unitSystem: UnitSystem): String {
        return if (unitSystem == UnitSystem.US_CUSTOMARY) {
            val fahrenheit = celsius * 9 / 5 + 32
            String.format(Locale.getDefault(), "%.1f°F", fahrenheit)
        } else {
            String.format(Locale.getDefault(), "%.1f°C", celsius)
        }
    }

    fun shouldShowGusts(windKmh: Double, gustKmh: Double?): Boolean {
        return gustKmh != null && gustKmh > windKmh + 3.0
    }

    fun formatWindGusts(windKmh: Double, gustKmh: Double?, unitSystem: UnitSystem): String? {
        if (!shouldShowGusts(windKmh, gustKmh) || gustKmh == null) return null
        return if (unitSystem == UnitSystem.US_CUSTOMARY) {
            val gustMph = kotlin.math.round(gustKmh * 0.621371).toInt()
            "$gustMph mph"
        } else {
            val gustRounded = kotlin.math.round(gustKmh).toInt()
            "$gustRounded km/h"
        }
    }

    fun formatWindSpeed(kmh: Double, unitSystem: UnitSystem, directionDeg: Double? = null): String {
        val dirText = if (directionDeg != null) " ${getDegreesToCompass(directionDeg)}" else ""
        return if (unitSystem == UnitSystem.US_CUSTOMARY) {
            val mph = kotlin.math.round(kmh * 0.621371).toInt()
            "$mph mph$dirText"
        } else {
            val kmhRounded = kotlin.math.round(kmh).toInt()
            "$kmhRounded km/h$dirText"
        }
    }

    fun formatWindSpeedPrimary(kmh: Double, unitSystem: UnitSystem, directionDeg: Double? = null): String {
        val dirText = if (directionDeg != null) " ${getDegreesToCompass(directionDeg)}" else ""
        return if (unitSystem == UnitSystem.US_CUSTOMARY) {
            val mph = kotlin.math.round(kmh * 0.621371).toInt()
            "$mph mph$dirText"
        } else {
            val kmhRounded = kotlin.math.round(kmh).toInt()
            "$kmhRounded km/h$dirText"
        }
    }

    fun formatVisibility(meters: Double, unitSystem: UnitSystem): String {
        return if (unitSystem == UnitSystem.US_CUSTOMARY) {
            val miles = meters * 0.000621371
            String.format(Locale.getDefault(), "%.1f mi", miles)
        } else {
            val km = meters / 1000.0
            String.format(Locale.getDefault(), "%.1f km", km)
        }
    }

    fun formatPrecipitation(mm: Double, unitSystem: UnitSystem): String {
        return if (unitSystem == UnitSystem.US_CUSTOMARY) {
            val inches = mm / 25.4
            String.format(Locale.getDefault(), "%.2f in", inches)
        } else {
            String.format(Locale.getDefault(), "%.1f mm", mm)
        }
    }

    fun formatCeiling(cloudCoverPct: Int, humidityPct: Int, unitSystem: UnitSystem): String {
        if (cloudCoverPct < 25) return "Unlimited"
        val estimatedCeilingMeters = (100 - humidityPct) * 125.0
        val boundedMeters = estimatedCeilingMeters.coerceIn(300.0, 12000.0)

        return if (unitSystem == UnitSystem.US_CUSTOMARY) {
            val feet = boundedMeters * 3.28084
            "${(kotlin.math.round(feet / 100.0) * 100).toInt()} ft"
        } else {
            "${(kotlin.math.round(boundedMeters / 100.0) * 100).toInt()} m"
        }
    }

    fun getAqiDescription(aqi: Int): String {
        return when (aqi) {
            in 0..50 -> "Good"
            in 51..100 -> "Moderate"
            in 101..150 -> "Sensitive"
            in 151..200 -> "Unhealthy"
            in 201..300 -> "Very Unhealthy"
            else -> "Hazardous"
        }
    }

    fun getUvDescription(uv: Double): String {
        return when {
            uv < 3.0 -> "Low"
            uv < 6.0 -> "Moderate"
            uv < 8.0 -> "High"
            uv < 11.0 -> "Very High"
            else -> "Extreme"
        }
    }

    fun getDegreesToCompass(degrees: Double): String {
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = (kotlin.math.round((degrees % 360) / 45.0)).toInt() % 8
        return directions[index]
    }

    fun formatLocationTime(timezoneStr: String?, is24Hour: Boolean = false, nowInstant: java.time.Instant = java.time.Instant.now()): String {
        val zoneId = try {
            if (!timezoneStr.isNullOrEmpty()) java.time.ZoneId.of(timezoneStr) else java.time.ZoneId.systemDefault()
        } catch (e: Exception) {
            java.time.ZoneId.systemDefault()
        }
        val now = java.time.ZonedDateTime.ofInstant(nowInstant, zoneId)
        val pattern = if (is24Hour) "HH:mm" else "h:mm a"
        val formatter = java.time.format.DateTimeFormatter.ofPattern(pattern, java.util.Locale.getDefault())
        return now.format(formatter)
    }
}
