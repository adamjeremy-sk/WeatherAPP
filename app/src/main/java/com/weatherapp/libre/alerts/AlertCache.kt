package com.weatherapp.libre.alerts

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.alerts.models.CapAlert
import androidx.annotation.Keep
import java.time.Instant
import java.util.Locale

object AlertCache {
    private const val PREFS_NAME = "alert_cache_prefs"
    private const val KEY_ACTIVE_ALERTS_CACHE = "app_active_alerts_cache_v3"

    @Keep
    data class CachedAlert(
        val identifier: String,
        val event: String,
        val severity: String,
        val headline: String?,
        val description: String?,
        val instruction: String?,
        val areaDesc: String,
        val sentUtcMs: Long,
        val expiresUtcMs: Long?,
        val isTestFixture: Boolean,
        val isSyntheticTest: Boolean = false
    ) {
        fun severityLevel(): Int {
            return when (severity.lowercase(Locale.getDefault())) {
                "extreme" -> 4
                "severe" -> 3
                "moderate" -> 2
                "minor" -> 1
                else -> 0
            }
        }
    }

    fun cacheKey(location: LocationItem): String {
        val nameClean = location.displayName.lowercase(Locale.getDefault())
        val coordsClean = String.format(Locale.US, "%.4f,%.4f", location.latitude, location.longitude)
        return "${nameClean}_${coordsClean}"
    }

    fun readCache(context: Context): Map<String, List<CachedAlert>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ACTIVE_ALERTS_CACHE, null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, List<CachedAlert>>>() {}.type
            Gson().fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun writeCacheForLocation(context: Context, location: LocationItem, alerts: List<CachedAlert>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCache = readCache(context).toMutableMap()
        val key = cacheKey(location)
        val nowMs = Instant.now().toEpochMilli()

        // Merge logic: Preserve unexpired test fixtures / synthetic test alerts already cached for this location
        val existingLocationAlerts = currentCache[key] ?: emptyList()
        val unexpiredSyntheticAlerts = existingLocationAlerts.filter { alert ->
            (alert.isSyntheticTest || alert.isTestFixture) && (alert.expiresUtcMs == null || alert.expiresUtcMs > nowMs)
        }

        // New real/test alerts replace real alerts, while unexpired synthetic test alerts are retained
        val finalAlerts = (alerts + unexpiredSyntheticAlerts).distinctBy { it.identifier }

        if (finalAlerts.isEmpty()) {
            currentCache.remove(key)
        } else {
            currentCache[key] = finalAlerts
        }

        prefs.edit().putString(KEY_ACTIVE_ALERTS_CACHE, Gson().toJson(currentCache)).apply()
    }

    fun CapAlert.toCachedAlert(location: LocationItem, isSyntheticTest: Boolean = this.isSyntheticTest || this.isTestFixture): CachedAlert? {
        val info = this.infoList.firstOrNull() ?: return null
        return CachedAlert(
            identifier = this.identifier,
            event = info.event,
            severity = info.severity,
            headline = info.headline,
            description = info.description,
            instruction = info.instruction,
            areaDesc = info.areaList.firstOrNull()?.areaDesc ?: location.displayName,
            sentUtcMs = this.sent.toEpochMilli(),
            expiresUtcMs = info.expires?.toEpochMilli(),
            isTestFixture = this.isTestFixture,
            isSyntheticTest = isSyntheticTest
        )
    }
}
