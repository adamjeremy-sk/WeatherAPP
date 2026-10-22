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

    fun writeAlertsForLocation(context: Context, location: LocationItem, alerts: List<UnifiedAlert>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = cacheKey(location)
        val cacheEntry = CacheEntry(alerts)
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
                alert.expires?.let { expires ->
                    try {
                        Instant.parse(expires).isAfter(now)
                    } catch (e: Exception) {
                        true
                    }
                } ?: true
            } ?: emptyList()

            if (filteredAlerts.isEmpty()) {
                prefs.edit().remove(key).apply()
            } else {
                prefs.edit().putString(key, gson.toJson(CacheEntry(filteredAlerts))).apply()
            }
        }
    }
}
