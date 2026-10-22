package com.weatherapp.libre.alerts

import android.content.Context
import android.util.Log
import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.LocationPreferences
import com.weatherapp.libre.alerts.models.CapAlert
import java.time.Instant
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class AlertManager(
    private val context: Context,
    private val locationPreferences: LocationPreferences
) {

    companion object {
        private const val TAG = "AlertManager"
    }

    private val liveAlertsByLocation = ConcurrentHashMap<String, MutableList<CapAlert>>()
    private val testAlertsByLocation = ConcurrentHashMap<String, MutableList<CapAlert>>()

    private fun locationKey(location: LocationItem): String = String.format(Locale.US, "%.4f_%.4f", location.latitude, location.longitude)

    fun processCapAlert(
        alert: CapAlert,
        location: LocationItem,
        timezoneStr: String?,
        isForegroundActiveLocation: Boolean = false
    ): Boolean {
        // Clean expired alerts first
        cleanExpiredAlerts(location)

        val targetMap = if (alert.isTestFixture || alert.isSyntheticTest) testAlertsByLocation else liveAlertsByLocation
        val key = locationKey(location)
        val locationAlerts = targetMap.getOrPut(key) { mutableListOf() }

        // Rule 1: Expiry check
        val info = alert.infoList.firstOrNull()
        if (info?.expires != null && info.expires.isBefore(Instant.now())) {
            Log.d(TAG, "Alert ${alert.identifier} is already expired upon arrival. Ignoring.")
            return false
        }

        when (alert.msgType.lowercase(Locale.US)) {
            "cancel" -> {
                // Remove cancelled alerts by referenced IDs
                val refIds = alert.getReferencedIdentifiers()
                Log.d(TAG, "Processing Cancel for ${alert.identifier}, references: $refIds")
                locationAlerts.removeAll { existing ->
                    val isRef = refIds.contains(existing.identifier) || alert.identifier.contains(existing.identifier)
                    if (isRef) {
                        AlertNotificationHelper.dismissNotification(context, existing.identifier)
                    }
                    isRef
                }
                return true
            }
            "update" -> {
                // Replace superseded alerts by referenced IDs
                val refIds = alert.getReferencedIdentifiers()
                Log.d(TAG, "Processing Update for ${alert.identifier}, references: $refIds")
                locationAlerts.removeAll { existing ->
                    val isRef = refIds.contains(existing.identifier) || alert.identifier.contains(existing.identifier)
                    if (isRef) {
                        AlertNotificationHelper.dismissNotification(context, existing.identifier)
                    }
                    isRef
                }
            }
            "alert" -> {
                // Deduplication check
                val exists = locationAlerts.any { it.identifier == alert.identifier && it.sent == alert.sent }
                if (exists) {
                    Log.d(TAG, "Duplicate alert ${alert.identifier} sent at ${alert.sent}. Skipping.")
                    return false
                }
            }
        }

        // Store new or updated alert
        locationAlerts.add(alert)

        // Post system notification if alerts enabled and location matches active foreground/background notification rules
        if (locationPreferences.getAlertConsentState() == LocationPreferences.ALERT_CONSENT_OPT_IN && isForegroundActiveLocation) {
            AlertNotificationHelper.postNotification(context, alert, location, timezoneStr)
        }

        val alertEvent = info?.event ?: info?.headline ?: "Weather Alert"
        val severity = info?.severity ?: "Unknown"
        val timestamp = alert.sent.toEpochMilli()

        AlertLogRepository.saveEntry(
            context,
            AlertLogEntry(
                id = System.currentTimeMillis(),
                timestampUtc = timestamp,
                locationName = location.displayName,
                alertEvent = alertEvent,
                severity = severity,
                isTest = alert.isTestFixture || alert.isSyntheticTest
            )
        )

        // Sync location alerts to persistent AlertCache
        val cachedAlerts = locationAlerts.mapNotNull {
            with(AlertCache) { it.toCachedAlert(location) }
        }
        AlertCache.writeCacheForLocation(context, location, cachedAlerts)

        return true
    }

    fun getActiveAlertForLocation(location: LocationItem): AlertCache.CachedAlert? {
        val nowMs = Instant.now().toEpochMilli()
        val key = AlertCache.cacheKey(location)
        val cachedAlerts = AlertCache.readCache(context)[key] ?: emptyList()

        val validAlerts = cachedAlerts.filter { alert ->
            val isExpired = alert.expiresUtcMs != null && alert.expiresUtcMs < nowMs
            !isExpired
        }

        return validAlerts.maxByOrNull { it.severityLevel() }
    }

    fun getLiveAlertsForLocation(location: LocationItem): List<CapAlert> {
        val key = locationKey(location)
        return liveAlertsByLocation[key]?.toList() ?: emptyList()
    }

    fun cleanExpiredAlerts(location: LocationItem) {
        val now = Instant.now()
        val removeAction: (CapAlert) -> Boolean = { alert ->
            val exp = alert.infoList.firstOrNull()?.expires
            val isExpired = exp != null && exp.isBefore(now)
            if (isExpired) {
                AlertNotificationHelper.dismissNotification(context, alert.identifier)
            }
            isExpired
        }

        val key = locationKey(location)
        liveAlertsByLocation[key]?.removeAll(removeAction)
        testAlertsByLocation[key]?.removeAll(removeAction)
    }

    fun clearTestAlerts(location: LocationItem) {
        val key = locationKey(location)
        testAlertsByLocation[key]?.forEach { alert ->
            AlertNotificationHelper.dismissNotification(context, alert.identifier)
        }
        testAlertsByLocation.remove(key)
    }
}
