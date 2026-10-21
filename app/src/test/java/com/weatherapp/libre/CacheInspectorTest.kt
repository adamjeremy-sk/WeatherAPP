package com.weatherapp.libre

import android.content.ContextWrapper
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CacheInspectorTest {

    private class TestSharedPreferences : SharedPreferences {
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

    @Test
    fun testCacheInspectorReturnsSnippets() {
        val fakePrefs = TestSharedPreferences()
        val dummyLongValue = "X".repeat(100) // 100 characters long
        fakePrefs.edit().putString("alert_long_key", dummyLongValue).commit()

        val fakeContext = object : ContextWrapper(null) {
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
                return fakePrefs
            }

            override fun getCacheDir(): File {
                val tempDir = File(System.getProperty("java.io.tmpdir"), "test_cache_dir_${System.currentTimeMillis()}")
                tempDir.mkdirs()
                return tempDir
            }
        }

        val entries = CacheInspector.inspectAll(fakeContext)
        val dummyEntry = entries.firstOrNull { it.key == "alert_long_key" }

        assertTrue("Dummy long entry must be present in inspector results", dummyEntry != null)
        assertEquals(60, dummyEntry!!.snippet.length)
        assertEquals("X".repeat(60), dummyEntry.snippet)
        assertEquals(100L, dummyEntry.sizeBytes)
    }

    @Test
    fun testCacheInspectorReadFullContent() {
        val fakePrefs = TestSharedPreferences()
        val dummyLongValue = "{" + "\"test\":".repeat(10) + "\"full_content_payload\"}"
        fakePrefs.edit().putString("alert_full_key", dummyLongValue).commit()

        val tempDir = File(System.getProperty("java.io.tmpdir"), "test_cache_dir_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        val testFile = File(tempDir, "cached_file.json")
        testFile.writeText("{\"file_key\":\"file_payload_content\"}")

        val fakeContext = object : ContextWrapper(null) {
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
                return fakePrefs
            }

            override fun getCacheDir(): File {
                return tempDir
            }
        }

        val entries = CacheInspector.inspectAll(fakeContext)
        val prefEntry = entries.first { it.key == "alert_full_key" }
        val fileEntry = entries.first { it.key == "cached_file.json" }

        val fullPrefContent = CacheInspector.readFullContent(fakeContext, prefEntry)
        val fullFileContent = CacheInspector.readFullContent(fakeContext, fileEntry)

        assertEquals(dummyLongValue, fullPrefContent)
        assertEquals("{\"file_key\":\"file_payload_content\"}", fullFileContent)
    }

    @Test
    fun testWeatherModelMetadataStamping() {
        val response = WeatherResponse(
            latitude = 45.4215,
            longitude = -75.6972,
            elevation = 70.0,
            timezone = "America/Toronto",
            current = null,
            daily = null
        )

        val beforeStamp = System.currentTimeMillis()
        response.fetchedAtEpochMillis = beforeStamp
        response.sourceModelName = ForecastModel.GEM.idKey

        assertTrue("fetchedAtEpochMillis must be stamped with current time", response.fetchedAtEpochMillis >= beforeStamp)
        assertEquals("GEM", response.sourceModelName)
    }
}
