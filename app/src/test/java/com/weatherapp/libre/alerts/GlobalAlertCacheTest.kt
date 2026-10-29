package com.weatherapp.libre.alerts

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.alerts.cache.GlobalAlertCache
import com.weatherapp.libre.alerts.models.AlertSeverity
import com.weatherapp.libre.alerts.models.AlertSource
import com.weatherapp.libre.alerts.models.UnifiedAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.OffsetDateTime

class GlobalAlertCacheTest {

    private class InMemorySharedPreferences : SharedPreferences {
        val map = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = HashMap(map)

        override fun getString(key: String, defValue: String?): String? {
            return (map[key] as? String) ?: defValue
        }

        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? {
            @Suppress("UNCHECKED_CAST")
            return (map[key] as? MutableSet<String>) ?: defValues
        }

        override fun getInt(key: String, defValue: Int): Int {
            return (map[key] as? Int) ?: defValue
        }

        override fun getLong(key: String, defValue: Long): Long {
            return (map[key] as? Long) ?: defValue
        }

        override fun getFloat(key: String, defValue: Float): Float {
            return (map[key] as? Float) ?: defValue
        }

        override fun getBoolean(key: String, defValue: Boolean): Boolean {
            return (map[key] as? Boolean) ?: defValue
        }

        override fun contains(key: String): Boolean = map.containsKey(key)

        override fun edit(): SharedPreferences.Editor = EditorImpl()

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        inner class EditorImpl : SharedPreferences.Editor {
            private val changes = mutableMapOf<String, Any?>()
            private val removes = mutableSetOf<String>()

            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                changes[key] = value
                removes.remove(key)
                return this
            }

            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor {
                changes[key] = values
                removes.remove(key)
                return this
            }

            override fun putInt(key: String, value: Int): SharedPreferences.Editor {
                changes[key] = value
                removes.remove(key)
                return this
            }

            override fun putLong(key: String, value: Long): SharedPreferences.Editor {
                changes[key] = value
                removes.remove(key)
                return this
            }

            override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
                changes[key] = value
                removes.remove(key)
                return this
            }

            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
                changes[key] = value
                removes.remove(key)
                return this
            }

            override fun remove(key: String): SharedPreferences.Editor {
                removes.add(key)
                changes.remove(key)
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                removes.addAll(map.keys)
                changes.clear()
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                for (key in removes) {
                    map.remove(key)
                }
                for ((key, value) in changes) {
                    map[key] = value
                }
            }
        }
    }

    private class MockContext : ContextWrapper(null) {
        val prefs = InMemorySharedPreferences()

        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
            return prefs
        }
    }

    private val testLocation = LocationItem(
        id = 100L,
        name = "Winnipeg",
        admin1 = "MB",
        country = "Canada",
        latitude = 49.8951,
        longitude = -97.1384
    )

    @Test
    fun testReconciliationUpdateMessageReplacesReferencedAlert() {
        val context = MockContext()
        val originalAlert = UnifiedAlert(
            id = "alert-001",
            source = AlertSource.ECCC,
            locationLat = testLocation.latitude,
            locationLon = testLocation.longitude,
            severity = AlertSeverity.YELLOW,
            eventNative = "Special Weather Statement",
            headline = "Special Statement",
            description = "Initial description",
            instruction = null,
            onset = null,
            expires = OffsetDateTime.now().plusHours(5).toString(),
            areaDesc = "Winnipeg",
            msgType = "Alert"
        )

        // Write original alert
        GlobalAlertCache.writeAlertsForLocation(context, testLocation, listOf(originalAlert))
        var cached = GlobalAlertCache.readAlertsForLocation(context, testLocation)
        assertEquals(1, cached.size)
        assertEquals("alert-001", cached[0].id)

        // Issue an Update message referencing alert-001
        val updateAlert = UnifiedAlert(
            id = "alert-002",
            source = AlertSource.ECCC,
            locationLat = testLocation.latitude,
            locationLon = testLocation.longitude,
            severity = AlertSeverity.ORANGE,
            eventNative = "Severe Thunderstorm Warning",
            headline = "Severe Thunderstorm Warning Updated",
            description = "Updated description",
            instruction = "Take cover",
            onset = null,
            expires = OffsetDateTime.now().plusHours(3).toString(),
            areaDesc = "Winnipeg",
            msgType = "Update",
            references = listOf("alert-001")
        )

        GlobalAlertCache.writeAlertsForLocation(context, testLocation, listOf(updateAlert))
        cached = GlobalAlertCache.readAlertsForLocation(context, testLocation)

        assertEquals(1, cached.size)
        assertEquals("alert-002", cached[0].id)
        assertEquals("Severe Thunderstorm Warning", cached[0].eventNative)
    }

    @Test
    fun testReconciliationCancelMessageRemovesReferencedAlert() {
        val context = MockContext()
        val alert1 = UnifiedAlert(
            id = "alert-100",
            source = AlertSource.ECCC,
            locationLat = testLocation.latitude,
            locationLon = testLocation.longitude,
            severity = AlertSeverity.RED,
            eventNative = "Tornado Warning",
            headline = "Tornado Warning",
            description = null,
            instruction = null,
            onset = null,
            expires = OffsetDateTime.now().plusHours(2).toString(),
            areaDesc = "Winnipeg",
            msgType = "Alert"
        )

        GlobalAlertCache.writeAlertsForLocation(context, testLocation, listOf(alert1))
        assertEquals(1, GlobalAlertCache.readAlertsForLocation(context, testLocation).size)

        // Issue a Cancel message
        val cancelAlert = UnifiedAlert(
            id = "alert-101",
            source = AlertSource.ECCC,
            locationLat = testLocation.latitude,
            locationLon = testLocation.longitude,
            severity = AlertSeverity.INFO,
            eventNative = "Tornado Warning",
            headline = "Tornado Warning Cancelled",
            description = null,
            instruction = null,
            onset = null,
            expires = OffsetDateTime.now().plusHours(1).toString(),
            areaDesc = "Winnipeg",
            msgType = "Cancel",
            references = listOf("alert-100")
        )

        GlobalAlertCache.writeAlertsForLocation(context, testLocation, listOf(cancelAlert))
        val cached = GlobalAlertCache.readAlertsForLocation(context, testLocation)

        assertTrue(cached.isEmpty())
    }

    @Test
    fun testExpiredAlertPruningWithOffsetDateTimeAndInstant() {
        val context = MockContext()
        val expiredOffsetDateTime = UnifiedAlert(
            id = "expired-1",
            source = AlertSource.ECCC,
            locationLat = testLocation.latitude,
            locationLon = testLocation.longitude,
            severity = AlertSeverity.YELLOW,
            eventNative = "Blizzard Watch",
            headline = null,
            description = null,
            instruction = null,
            onset = null,
            expires = OffsetDateTime.now().minusHours(1).toString(),
            areaDesc = "Winnipeg"
        )

        val expiredInstant = UnifiedAlert(
            id = "expired-2",
            source = AlertSource.ECCC,
            locationLat = testLocation.latitude,
            locationLon = testLocation.longitude,
            severity = AlertSeverity.YELLOW,
            eventNative = "Blizzard Watch",
            headline = null,
            description = null,
            instruction = null,
            onset = null,
            expires = Instant.now().minusSeconds(3600).toString(),
            areaDesc = "Winnipeg"
        )

        val validAlert = UnifiedAlert(
            id = "active-1",
            source = AlertSource.ECCC,
            locationLat = testLocation.latitude,
            locationLon = testLocation.longitude,
            severity = AlertSeverity.RED,
            eventNative = "Blizzard Warning",
            headline = null,
            description = null,
            instruction = null,
            onset = null,
            expires = OffsetDateTime.now().plusHours(5).toString(),
            areaDesc = "Winnipeg"
        )

        GlobalAlertCache.writeAlertsForLocation(
            context,
            testLocation,
            listOf(expiredOffsetDateTime, expiredInstant, validAlert)
        )

        val cached = GlobalAlertCache.readAlertsForLocation(context, testLocation)
        assertEquals(1, cached.size)
        assertEquals("active-1", cached[0].id)
    }

    @Test
    fun testClearExpiredAlerts() {
        val context = MockContext()
        val key = GlobalAlertCache.cacheKey(testLocation)

        val expiredAlert = UnifiedAlert(
            id = "alert-expired",
            source = AlertSource.ECCC,
            locationLat = testLocation.latitude,
            locationLon = testLocation.longitude,
            severity = AlertSeverity.YELLOW,
            eventNative = "Severe Wind",
            headline = null,
            description = null,
            instruction = null,
            onset = null,
            expires = OffsetDateTime.now().minusHours(3).toString(),
            areaDesc = "Winnipeg"
        )

        val futureAlert = UnifiedAlert(
            id = "alert-future",
            source = AlertSource.ECCC,
            locationLat = testLocation.latitude,
            locationLon = testLocation.longitude,
            severity = AlertSeverity.RED,
            eventNative = "Tornado Warning",
            headline = null,
            description = null,
            instruction = null,
            onset = null,
            expires = OffsetDateTime.now().plusHours(3).toString(),
            areaDesc = "Winnipeg"
        )

        val cacheEntry = GlobalAlertCache.CacheEntry(listOf(expiredAlert, futureAlert))
        val gson = com.google.gson.GsonBuilder()
            .registerTypeAdapter(AlertSeverity::class.java, com.weatherapp.libre.alerts.cache.AlertSeverityTypeAdapter())
            .create()

        context.prefs.edit().putString(key, gson.toJson(cacheEntry)).apply()

        GlobalAlertCache.clearExpiredAlerts(context)

        val cached = GlobalAlertCache.readAlertsForLocation(context, testLocation)
        assertEquals(1, cached.size)
        assertEquals("alert-future", cached[0].id)
    }

    @Test
    fun testCorruptedJsonRecovery() {
        val context = MockContext()
        val key = GlobalAlertCache.cacheKey(testLocation)

        context.prefs.edit().putString(key, "CORRUPTED_GARBAGE_JSON{{{[[[").apply()

        val cached = GlobalAlertCache.readAlertsForLocation(context, testLocation)
        assertTrue(cached.isEmpty())
    }

    @Test
    fun testMultiReferenceCancellation() {
        val context = MockContext()
        val alert1 = UnifiedAlert(
            id = "alert-multi-1",
            source = AlertSource.ECCC,
            locationLat = testLocation.latitude,
            locationLon = testLocation.longitude,
            severity = AlertSeverity.YELLOW,
            eventNative = "Thunderstorm Watch",
            headline = null,
            description = null,
            instruction = null,
            onset = null,
            expires = OffsetDateTime.now().plusHours(4).toString(),
            areaDesc = "Winnipeg",
            msgType = "Alert"
        )

        val alert2 = UnifiedAlert(
            id = "alert-multi-2",
            source = AlertSource.ECCC,
            locationLat = testLocation.latitude,
            locationLon = testLocation.longitude,
            severity = AlertSeverity.ORANGE,
            eventNative = "Flood Warning",
            headline = null,
            description = null,
            instruction = null,
            onset = null,
            expires = OffsetDateTime.now().plusHours(4).toString(),
            areaDesc = "Winnipeg",
            msgType = "Alert"
        )

        val alert3 = UnifiedAlert(
            id = "alert-multi-3",
            source = AlertSource.ECCC,
            locationLat = testLocation.latitude,
            locationLon = testLocation.longitude,
            severity = AlertSeverity.RED,
            eventNative = "Tornado Warning",
            headline = null,
            description = null,
            instruction = null,
            onset = null,
            expires = OffsetDateTime.now().plusHours(4).toString(),
            areaDesc = "Winnipeg",
            msgType = "Alert"
        )

        GlobalAlertCache.writeAlertsForLocation(context, testLocation, listOf(alert1, alert2, alert3))
        assertEquals(3, GlobalAlertCache.readAlertsForLocation(context, testLocation).size)

        val multiCancelAlert = UnifiedAlert(
            id = "cancel-all",
            source = AlertSource.ECCC,
            locationLat = testLocation.latitude,
            locationLon = testLocation.longitude,
            severity = AlertSeverity.INFO,
            eventNative = "All Warnings Cancelled",
            headline = null,
            description = null,
            instruction = null,
            onset = null,
            expires = OffsetDateTime.now().plusHours(1).toString(),
            areaDesc = "Winnipeg",
            msgType = "Cancel",
            references = listOf("alert-multi-1", "alert-multi-2", "alert-multi-3")
        )

        GlobalAlertCache.writeAlertsForLocation(context, testLocation, listOf(multiCancelAlert))
        val cached = GlobalAlertCache.readAlertsForLocation(context, testLocation)

        assertTrue(cached.isEmpty())
    }
}
