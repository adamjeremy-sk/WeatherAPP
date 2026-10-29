package com.weatherapp.libre.alerts

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object AlertLogRepository {
    private const val PREFS_NAME = "alert_log_prefs"
    private const val KEY_ENTRIES = "alert_log_entries_v1"
    private const val MAX_ENTRIES = 200
    private val gson = Gson()
    private val logMutex = Mutex()

    suspend fun saveEntry(context: Context, entry: AlertLogEntry) = logMutex.withLock {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentEntries = getAllEntries(context).toMutableList()
        currentEntries.add(0, entry)
        if (currentEntries.size > MAX_ENTRIES) {
            currentEntries.subList(MAX_ENTRIES, currentEntries.size).clear()
        }
        prefs.edit().putString(KEY_ENTRIES, gson.toJson(currentEntries)).apply()
    }

    fun getAllEntries(context: Context): List<AlertLogEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<AlertLogEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun clearAll(context: Context) = logMutex.withLock {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_ENTRIES).apply()
    }
}
