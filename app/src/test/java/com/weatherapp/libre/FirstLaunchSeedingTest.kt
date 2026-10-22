package com.weatherapp.libre

import android.content.ContextWrapper
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstLaunchSeedingTest {

    private class InMemorySharedPreferences : SharedPreferences {
        val map = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = HashMap(map)

        override fun getString(key: String, defValue: String?): String? = (map[key] as? String) ?: defValue

        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? {
            @Suppress("UNCHECKED_CAST")
            return (map[key] as? MutableSet<String>) ?: defValues
        }

        override fun getInt(key: String, defValue: Int): Int = (map[key] as? Int) ?: defValue

        override fun getLong(key: String, defValue: Long): Long = (map[key] as? Long) ?: defValue

        override fun getFloat(key: String, defValue: Float): Float = (map[key] as? Float) ?: defValue

        override fun getBoolean(key: String, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue

        override fun contains(key: String): Boolean = map.containsKey(key)

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
    fun testDefaultLocationConstants() {
        assertEquals(1001L, LocationPreferences.DEFAULT_EDMONTON.id)
        assertEquals("Edmonton", LocationPreferences.DEFAULT_EDMONTON.name)
        assertEquals("Alberta", LocationPreferences.DEFAULT_EDMONTON.admin1)
        assertEquals("Canada", LocationPreferences.DEFAULT_EDMONTON.country)
        assertEquals(53.5461, LocationPreferences.DEFAULT_EDMONTON.latitude, 0.0001)
        assertEquals(-113.4938, LocationPreferences.DEFAULT_EDMONTON.longitude, 0.0001)
        assertEquals(645.0, LocationPreferences.DEFAULT_EDMONTON.elevationMeters, 0.01)

        assertEquals(1002L, LocationPreferences.DEFAULT_MONTREAL.id)
        assertEquals("Montreal", LocationPreferences.DEFAULT_MONTREAL.name)
        assertEquals("Quebec", LocationPreferences.DEFAULT_MONTREAL.admin1)
        assertEquals("Canada", LocationPreferences.DEFAULT_MONTREAL.country)
        assertEquals(45.5019, LocationPreferences.DEFAULT_MONTREAL.latitude, 0.0001)
        assertEquals(-73.5674, LocationPreferences.DEFAULT_MONTREAL.longitude, 0.0001)
        assertEquals(216.0, LocationPreferences.DEFAULT_MONTREAL.elevationMeters, 0.01)

        assertEquals(1003L, LocationPreferences.DEFAULT_HALIFAX.id)
        assertEquals("Halifax", LocationPreferences.DEFAULT_HALIFAX.name)
        assertEquals("Nova Scotia", LocationPreferences.DEFAULT_HALIFAX.admin1)
        assertEquals("Canada", LocationPreferences.DEFAULT_HALIFAX.country)
        assertEquals(44.6488, LocationPreferences.DEFAULT_HALIFAX.latitude, 0.0001)
        assertEquals(-63.5752, LocationPreferences.DEFAULT_HALIFAX.longitude, 0.0001)
        assertEquals(60.0, LocationPreferences.DEFAULT_HALIFAX.elevationMeters, 0.01)
    }

    @Test
    fun testFirstLaunchSeedingOrderAndCompletion() {
        val fakePrefs = InMemorySharedPreferences()
        val prefs = createLocationPreferences(fakePrefs)

        assertTrue(prefs.isFirstLaunch())

        prefs.saveTabCount(3)
        prefs.saveTabLocation(0, LocationPreferences.DEFAULT_EDMONTON)
        prefs.saveTabLocation(1, LocationPreferences.DEFAULT_MONTREAL)
        prefs.saveTabLocation(2, LocationPreferences.DEFAULT_HALIFAX)
        prefs.setFirstLaunchCompleted()

        assertFalse(prefs.isFirstLaunch())
        assertEquals(3, prefs.getTabCount())

        val tab0 = prefs.getTabLocation(0)
        assertEquals("Edmonton", tab0.name)
        assertEquals(53.5461, tab0.latitude, 0.0001)
        assertEquals(-113.4938, tab0.longitude, 0.0001)

        val tab1 = prefs.getTabLocation(1)
        assertEquals("Montreal", tab1.name)
        assertEquals(45.5019, tab1.latitude, 0.0001)
        assertEquals(-73.5674, tab1.longitude, 0.0001)

        val tab2 = prefs.getTabLocation(2)
        assertEquals("Halifax", tab2.name)
        assertEquals(44.6488, tab2.latitude, 0.0001)
        assertEquals(-63.5752, tab2.longitude, 0.0001)
    }
}
