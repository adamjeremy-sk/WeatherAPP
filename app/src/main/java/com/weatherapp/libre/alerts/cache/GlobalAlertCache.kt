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

    /** Stable key for a chain: the alert's own chainKey if present, else its id. */
    fun chainKeyOf(alert: UnifiedAlert): String = alert.chainKey ?: alert.id

    private fun reconcile(
        existingAlerts: List<UnifiedAlert>,
        newAlerts: List<UnifiedAlert>
    ): List<UnifiedAlert> {
        // 1. Pool = existing + new, de-duplicated by id (a duplicate id keeps the newAlerts copy,
        //    preserving any existing non-null chainKey if the new alert has none).
        val poolById = LinkedHashMap<String, UnifiedAlert>()
        for (alert in existingAlerts) poolById[alert.id] = alert
        for (alert in newAlerts) {
            val existing = poolById[alert.id]
            if (existing?.chainKey != null && alert.chainKey == null) {
                poolById[alert.id] = alert.copy(chainKey = existing.chainKey)
            } else {
                poolById[alert.id] = alert
            }
        }
        val pool = poolById.values.toList()
        if (pool.isEmpty()) return emptyList()

        // 2. Union-find over pool ids plus every referenced id. Referenced ids that are not
        //    present in the pool still act as nodes, so two messages that reference the same
        //    absent original land in the same chain.
        val parent = HashMap<String, String>()
        fun find(node: String): String {
            var root = node
            while (true) {
                val rootParent = parent[root]
                if (rootParent == null || rootParent == root) return root
                root = rootParent
            }
        }
        fun union(a: String, b: String) {
            parent.putIfAbsent(a, a)
            parent.putIfAbsent(b, b)
            val rootA = find(a)
            val rootB = find(b)
            if (rootA != rootB) parent[rootA] = rootB
        }
        for (alert in pool) {
            parent.putIfAbsent(alert.id, alert.id)
            for (refId in alert.references) union(alert.id, refId)
        }

        val chains = HashMap<String, MutableList<UnifiedAlert>>()
        for (alert in pool) {
            chains.getOrPut(find(alert.id)) { mutableListOf() }.add(alert)
        }

        // 3-5. Pick the winner per chain; drop the whole chain when the winner is a Cancel.
        val kept = mutableListOf<UnifiedAlert>()
        for (chain in chains.values) {
            val winner = chain.maxWith { a, b -> compareChainWinners(a, b) }
            if (winner.msgType.equals("cancel", ignoreCase = true)) continue
            // Stable chain key: the smallest non-null chainKey among the chain's pool members,
            // else the winner's id. Stays constant as the winning message changes across Updates.
            val key = chain.mapNotNull { it.chainKey }.minOrNull() ?: winner.id
            kept.add(winner.copy(chainKey = key))
        }
        return kept
    }

    private fun sentEpochMillis(alert: UnifiedAlert): Long? {
        val sent = alert.sent ?: return null
        return try {
            OffsetDateTime.parse(sent).toInstant().toEpochMilli()
        } catch (_: Exception) {
            try {
                Instant.parse(sent).toEpochMilli()
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun msgTypeRank(alert: UnifiedAlert): Int {
        return when (alert.msgType.lowercase(Locale.US)) {
            "cancel" -> 2
            "update" -> 1
            else -> 0
        }
    }

    private fun compareChainWinners(a: UnifiedAlert, b: UnifiedAlert): Int {
        val aSent = sentEpochMillis(a)
        val bSent = sentEpochMillis(b)
        if (aSent != null && bSent != null && aSent != bSent) {
            return aSent.compareTo(bSent)
        }
        return msgTypeRank(a).compareTo(msgTypeRank(b))
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
                val alerts = cacheEntry?.alerts ?: emptyList()
                // Gson can leave these null for legacy JSON written before the fields existed.
                alerts.map { alert ->
                    alert.copy(
                        msgType = (alert.msgType as String?) ?: "Alert",
                        references = (alert.references as List<String>?) ?: emptyList()
                    )
                }
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
