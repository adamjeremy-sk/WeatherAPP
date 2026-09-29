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

    fun formatWindSpeed(kmh: Double, unitSystem: UnitSystem, directionDeg: Double? = null, gustKmh: Double? = null): String {
        val dirText = if (directionDeg != null) " ${getDegreesToCompass(directionDeg)}" else ""
        val gustText = if (gustKmh != null && gustKmh > kmh + 3.0) {
            if (unitSystem == UnitSystem.US_CUSTOMARY) {
                val gustMph = kotlin.math.round(gustKmh * 0.621371).toInt()
                " (Gusts $gustMph mph)"
            } else {
                val gustRounded = kotlin.math.round(gustKmh).toInt()
                " (Gusts $gustRounded km/h)"
            }
        } else ""

        return if (unitSystem == UnitSystem.US_CUSTOMARY) {
            val mph = kotlin.math.round(kmh * 0.621371).toInt()
            "$mph mph$dirText$gustText"
        } else {
            val kmhRounded = kotlin.math.round(kmh).toInt()
            "$kmhRounded km/h$dirText$gustText"
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

    fun formatWindSpeedGusts(gustKmh: Double?, kmh: Double, unitSystem: UnitSystem): String? {
        if (gustKmh == null || gustKmh <= kmh + 3.0) return null
        return if (unitSystem == UnitSystem.US_CUSTOMARY) {
            val gustMph = kotlin.math.round(gustKmh * 0.621371).toInt()
            "Gusts: $gustMph mph"
        } else {
            val gustRounded = kotlin.math.round(gustKmh).toInt()
            "Gusts: $gustRounded km/h"
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

    fun formatLocationTime(timezoneStr: String?, is24Hour: Boolean = false): String {
        val zoneId = try {
            if (!timezoneStr.isNullOrEmpty()) ZoneId.of(timezoneStr) else ZoneId.systemDefault()
        } catch (e: Exception) {
            ZoneId.systemDefault()
        }
        val now = ZonedDateTime.now(zoneId)
        val pattern = if (is24Hour) "HH:mm" else "h:mm a"
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        return now.format(formatter)
    }
}
