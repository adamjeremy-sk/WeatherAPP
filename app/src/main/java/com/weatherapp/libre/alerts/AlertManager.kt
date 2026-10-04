package com.weatherapp.libre.alerts

import android.content.Context
import android.util.Log
import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.LocationPreferences
import com.weatherapp.libre.alerts.models.CapAlert
import com.weatherapp.libre.alerts.models.CapInfo
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class AlertManager(
    private val context: Context,
    private val locationPreferences: LocationPreferences,
    private val sourceAdapter: AlertSourceAdapter = EcccCapSourceAdapter()
) {

    companion object {
        private const val TAG = "AlertManager"
        private const val FETCH_INTERVAL_FOREGROUND_MS = 5 * 60 * 1000L // 5 minutes
    }

    private var lastFetchTimeMs = 0L
    private val liveAlertsByLocation = ConcurrentHashMap<Long, MutableList<CapAlert>>()
    private val testAlertsByLocation = ConcurrentHashMap<Long, MutableList<CapAlert>>()

    fun processCapAlert(
        alert: CapAlert,
        location: LocationItem,
        timezoneStr: String?,
        isForegroundActiveLocation: Boolean = false
    ): Boolean {
        // Clean expired alerts first
        cleanExpiredAlerts(location)

        val targetMap = if (alert.isTestFixture) testAlertsByLocation else liveAlertsByLocation
        val locationAlerts = targetMap.getOrPut(location.id) { mutableListOf() }

        // Rule 1: Expiry check
        val info = alert.infoList.firstOrNull()
        if (info?.expires != null && info.expires.isBefore(Instant.now())) {
            Log.d(TAG, "Alert ${alert.identifier} is already expired upon arrival. Ignoring.")
            return false
        }

        when (alert.msgType.lowercase()) {
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

    fun getLiveAlertsForLocation(locationId: Long): List<CapAlert> {
        return liveAlertsByLocation[locationId]?.toList() ?: emptyList()
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

        liveAlertsByLocation[location.id]?.removeAll(removeAction)
        testAlertsByLocation[location.id]?.removeAll(removeAction)
    }

    fun clearTestAlerts(location: LocationItem) {
        testAlertsByLocation[location.id]?.forEach { alert ->
            AlertNotificationHelper.dismissNotification(context, alert.identifier)
        }
        testAlertsByLocation.remove(location.id)
    }

    fun fetchAndEvaluateAlertsForLocation(
        location: LocationItem,
        timezoneStr: String?,
        isForegroundActiveLocation: Boolean = false,
        forceRefresh: Boolean = false
    ): Int {
        val nowMs = System.currentTimeMillis()
        if (!forceRefresh && (nowMs - lastFetchTimeMs) < FETCH_INTERVAL_FOREGROUND_MS) {
            Log.d(TAG, "Skipping fetch, last fetch was ${(nowMs - lastFetchTimeMs)/1000}s ago.")
            return getActiveAlertForLocation(location)?.let { 1 } ?: 0
        }

        Log.d(TAG, "Fetching live ECCC alerts for location: ${location.displayName}")
        val fetchResult = sourceAdapter.fetchAlerts()
        lastFetchTimeMs = nowMs

        if (fetchResult.statusCode != 200 && fetchResult.statusCode != 304) {
            Log.e(TAG, "Failed to fetch ECCC alerts: ${fetchResult.errorMessage}")
            return 0
        }

        var matchCount = 0
        for (alert in fetchResult.alerts) {
            val matchResult = AlertMatcher.matchesLocation(alert, location)
            if (matchResult.isMatched) {
                matchCount++
                Log.d(TAG, "ECCC Alert ${alert.identifier} matched ${location.displayName} via rule ${matchResult.rule}")
                processCapAlert(alert, location, timezoneStr, isForegroundActiveLocation)
            }
        }

        return matchCount
    }
}
