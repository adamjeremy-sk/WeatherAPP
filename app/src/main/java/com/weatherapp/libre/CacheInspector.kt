package com.weatherapp.libre

import android.content.Context
import androidx.annotation.Keep

@Keep
data class CacheEntry(
    val source: String,
    val key: String,
    val snippet: String,
    val sizeBytes: Long
)

object CacheInspector {

    fun inspectAll(context: Context): List<CacheEntry> {
        val entries = mutableListOf<CacheEntry>()
        val fileSourceStr = context.getString(R.string.cache_source_file)
        val prefsSourceStr = context.getString(R.string.cache_source_prefs)

        // 1. Files in context.cacheDir
        val cacheFiles = try { context.cacheDir?.listFiles() } catch (e: Exception) { null } ?: emptyArray()
        for (file in cacheFiles) {
            val size = file.length()
            val snippet = try {
                if (file.isFile && file.canRead() && size > 0) {
                    file.readText().take(60)
                } else {
                    context.getString(R.string.cache_file_size_bytes, size)
                }
            } catch (e: Exception) {
                context.getString(R.string.cache_binary_file_bytes, size)
            }
            entries.add(
                CacheEntry(
                    source = fileSourceStr,
                    key = file.name,
                    snippet = snippet.take(60),
                    sizeBytes = size
                )
            )
        }

        // 2. Weather-specific SharedPreferences entries
        // a) Alert cache ("alert_cache_prefs")
        try {
            val alertPrefs = context.getSharedPreferences("alert_cache_prefs", Context.MODE_PRIVATE)
            for ((key, value) in alertPrefs.all) {
                val valStr = value?.toString() ?: ""
                entries.add(
                    CacheEntry(
                        source = prefsSourceStr,
                        key = key,
                        snippet = valStr.take(60),
                        sizeBytes = valStr.length.toLong()
                    )
                )
            }
        } catch (e: Exception) {
            // Ignore
        }

        // b) Weather forecast cache entries in "weather_location_prefs"
        try {
            val weatherPrefs = context.getSharedPreferences("weather_location_prefs", Context.MODE_PRIVATE)
            for ((key, value) in weatherPrefs.all) {
                if (key.startsWith("disk_cache_")) {
                    val valStr = value?.toString() ?: ""
                    entries.add(
                        CacheEntry(
                            source = prefsSourceStr,
                            key = key,
                            snippet = valStr.take(60),
                            sizeBytes = valStr.length.toLong()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        return entries
    }

    fun clearWeatherCache(context: Context, locationPreferences: LocationPreferences? = null) {
        // 1. Clear cacheDir recursively
        try {
            context.cacheDir?.deleteRecursively()
        } catch (e: Exception) {
            // Ignore failure
        }

        // 2. Clear alert_cache_prefs
        try {
            context.getSharedPreferences("alert_cache_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        } catch (e: Exception) {
            // Ignore failure
        }

        // 3. Clear weather forecast disk cache keys in weather_location_prefs
        locationPreferences?.clearPersistentCache()
    }
}
