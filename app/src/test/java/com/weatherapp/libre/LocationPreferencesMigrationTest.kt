package com.weatherapp.libre

import android.content.ContextWrapper
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LocationPreferencesMigrationTest {

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

        override fun contains(key: String): Boolean {
            return map.containsKey(key)
        }

        override fun edit(): SharedPreferences.Editor = Editor()

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        inner class Editor : SharedPreferences.Editor {
            private val pendingPuts = mutableMapOf<String, Any?>()
            private val pendingRemoves = mutableSetOf<String>()
            private var clearAll = false

            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                pendingPuts[key] = value
                return this
            }

            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor {
                pendingPuts[key] = values
                return this
            }

            override fun putInt(key: String, value: Int): SharedPreferences.Editor {
                pendingPuts[key] = value
                return this
            }

            override fun putLong(key: String, value: Long): SharedPreferences.Editor {
                pendingPuts[key] = value
                return this
            }

            override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
                pendingPuts[key] = value
                return this
            }

            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
                pendingPuts[key] = value
                return this
            }

            override fun remove(key: String): SharedPreferences.Editor {
                pendingRemoves.add(key)
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                clearAll = true
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearAll) {
                    map.clear()
                }
                for (key in pendingRemoves) {
                    map.remove(key)
                }
                map.putAll(pendingPuts)
            }
        }
    }

    private fun createLocationPreferences(fakePrefs: InMemorySharedPreferences): LocationPreferences {
        val fakeContext = object : ContextWrapper(null) {
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
                return fakePrefs
            }
        }
        return LocationPreferences(fakeContext)
    }

    @Test
    fun testMigrationWithLegacyWeatherAlertsTrue() {
        val fakePrefs = InMemorySharedPreferences()
        fakePrefs.map["app_weather_alerts_enabled"] = true

        val prefs = createLocationPreferences(fakePrefs)
        prefs.migrateLegacyConsentState()

        assertEquals(LocationPreferences.ALERT_CONSENT_OPT_IN, prefs.getAlertConsentState())
        assertFalse(fakePrefs.contains("app_weather_alerts_enabled"))
        assertFalse(fakePrefs.contains("app_alert_consent_version"))
    }

    @Test
    fun testMigrationWithLegacyWeatherAlertsFalse() {
        val fakePrefs = InMemorySharedPreferences()
        fakePrefs.map["app_weather_alerts_enabled"] = false

        val prefs = createLocationPreferences(fakePrefs)
        prefs.migrateLegacyConsentState()

        assertEquals(LocationPreferences.ALERT_CONSENT_OPT_OUT, prefs.getAlertConsentState())
        assertFalse(fakePrefs.contains("app_weather_alerts_enabled"))
        assertFalse(fakePrefs.contains("app_alert_consent_version"))
    }

    @Test
    fun testMigrationWithLegacyAlertConsentVersionOnly() {
        val fakePrefs = InMemorySharedPreferences()
        fakePrefs.map["app_alert_consent_version"] = 1

        val prefs = createLocationPreferences(fakePrefs)
        prefs.migrateLegacyConsentState()

        assertEquals(LocationPreferences.ALERT_CONSENT_OPT_IN, prefs.getAlertConsentState())
        assertFalse(fakePrefs.contains("app_weather_alerts_enabled"))
        assertFalse(fakePrefs.contains("app_alert_consent_version"))
    }

    @Test
    fun testMigrationWithNoLegacyKeysRemainsUnset() {
        val fakePrefs = InMemorySharedPreferences()

        val prefs = createLocationPreferences(fakePrefs)
        prefs.migrateLegacyConsentState()

        assertEquals(LocationPreferences.ALERT_CONSENT_UNSET, prefs.getAlertConsentState())
        assertFalse(fakePrefs.contains("app_alert_consent_state"))
    }

    @Test
    fun testMigrationIsIdempotent() {
        val fakePrefs = InMemorySharedPreferences()
        fakePrefs.map["app_weather_alerts_enabled"] = true

        val prefs = createLocationPreferences(fakePrefs)
        prefs.migrateLegacyConsentState()

        assertEquals(LocationPreferences.ALERT_CONSENT_OPT_IN, prefs.getAlertConsentState())

        // Call migration second time
        prefs.migrateLegacyConsentState()

        assertEquals(LocationPreferences.ALERT_CONSENT_OPT_IN, prefs.getAlertConsentState())
        assertFalse(fakePrefs.contains("app_weather_alerts_enabled"))
        assertFalse(fakePrefs.contains("app_alert_consent_version"))
    }

    @Test
    fun testAlertConsentStatePreservedAcrossVersionUpdate() {
        val fakePrefs = InMemorySharedPreferences()
        fakePrefs.map["app_last_run_version_code"] = 1
        fakePrefs.map["app_alert_consent_state"] = LocationPreferences.ALERT_CONSENT_OPT_IN

        val prefs = createLocationPreferences(fakePrefs)
        assertEquals(LocationPreferences.ALERT_CONSENT_OPT_IN, prefs.getAlertConsentState())

        val repo = WeatherRepository()

        prefs.checkAndPerformVersionMigration(currentVersionCode = 2, repository = repo)

        assertEquals(LocationPreferences.ALERT_CONSENT_OPT_IN, prefs.getAlertConsentState())
    }
}
