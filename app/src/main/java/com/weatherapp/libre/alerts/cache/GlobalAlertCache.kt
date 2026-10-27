package com.weatherapp.libre.alerts.cache

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.alerts.models.AlertSeverity
import com.weatherapp.libre.alerts.models.UnifiedAlert
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Locale

class AlertSeverityTypeAdapter : TypeAdapter<AlertSeverity>() {
    override fun write(out: JsonWriter, value: AlertSeverity?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.name)
        }
    }

    override fun read(`in`: JsonReader): AlertSeverity {
        if (`in`.peek() == JsonToken.NULL) {
            `in`.nextNull()
            return AlertSeverity.UNKNOWN
        }
        val str = `in`.nextString()
        return try {
            AlertSeverity.valueOf(str.uppercase(Locale.US))
        } catch (e: Exception) {
            AlertSeverity.UNKNOWN
        }
    }
}

object GlobalAlertCache {
    private const val PREFS_NAME = "global_alert_cache_prefs"

    private val gson = GsonBuilder()
        .registerTypeAdapter(AlertSeverity::class.java, AlertSeverityTypeAdapter())
        .create()

    @Keep
    data class CacheEntry(
        @SerializedName("alerts") val alerts: List<UnifiedAlert> = emptyList()
    )

    fun cacheKey(location: LocationItem): String {
        return String.format(Locale.US, "global_alerts_loc_%.4f_%.4f", location.latitude, location.longitude)
    }

    private fun reconcile(
        existingAlerts: List<UnifiedAlert>,
        newAlerts: List<UnifiedAlert>
    ): List<UnifiedAlert> {
        val resultMap = existingAlerts.associateBy { it.id }.toMutableMap()

        for (newAlert in newAlerts) {
            when (newAlert.msgType.lowercase(Locale.US)) {
                "cancel" -> {
                    // Remove this alert and any alerts it references
                    resultMap.remove(newAlert.id)
                    newAlert.references.forEach { refId -> resultMap.remove(refId) }
                }
                "update" -> {
                    // Replace referenced alerts with this new alert
                    newAlert.references.forEach { refId -> resultMap.remove(refId) }
                    resultMap[newAlert.id] = newAlert
                }
                else -> {
                    // Alert, Error, etc. - add or replace
                    resultMap[newAlert.id] = newAlert
                }
            }
        }

        return resultMap.values.toList()
    }

    private fun isAlertActive(expiresStr: String?, now: Instant): Boolean {
        if (expiresStr == null) return true
        return try {
            OffsetDateTime.parse(expiresStr).toInstant().isAfter(now)
        } catch (_: Exception) {
            try {
                Instant.parse(expiresStr).isAfter(now)
            } catch (_: Exception) {
                true // Keep alert if parsing fails
            }
        }
    }

    fun writeAlertsForLocation(context: Context, location: LocationItem, alerts: List<UnifiedAlert>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = cacheKey(location)

        // Read existing alerts for this location
        val existingAlerts = readAlertsForLocation(context, location)

        // Reconcile new alerts with existing ones
        val reconciledAlerts = reconcile(existingAlerts, alerts)

        // Filter out expired alerts using OffsetDateTime.parse / Instant.parse
        val now = Instant.now()
        val activeAlerts = reconciledAlerts.filter { alert ->
            isAlertActive(alert.expires, now)
        }

        val cacheEntry = CacheEntry(activeAlerts)
        prefs.edit().putString(key, gson.toJson(cacheEntry)).apply()
    }

    fun readAlertsForLocation(context: Context, location: LocationItem): List<UnifiedAlert> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = cacheKey(location)
        return try {
            val json = prefs.getString(key, null)
            if (json != null) {
                val cacheEntry = gson.fromJson(json, CacheEntry::class.java)
                cacheEntry?.alerts ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearExpiredAlerts(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allKeys = prefs.all.keys
        val now = Instant.now()

        val expiredKeys = allKeys.filter { key ->
            key.startsWith("global_alerts_loc_") && prefs.getString(key, null) != null
        }

        for (key in expiredKeys) {
            val cacheEntry = prefs.getString(key, null)?.let { gson.fromJson(it, CacheEntry::class.java) }
            val filteredAlerts = cacheEntry?.alerts?.filter { alert ->
                isAlertActive(alert.expires, now)
            } ?: emptyList()

            if (filteredAlerts.isEmpty()) {
                prefs.edit().remove(key).apply()
            } else {
                prefs.edit().putString(key, gson.toJson(CacheEntry(filteredAlerts))).apply()
            }
        }
    }
}
