package com.weatherapp.libre.almanac

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import com.weatherapp.libre.LocationItem
import java.io.File
import java.lang.reflect.Type
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class LocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
    override fun serialize(src: LocalDate?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.toString() ?: "")
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDate {
        val str = json?.asString ?: return LocalDate.now()
        return try {
            LocalDate.parse(str)
        } catch (e: Exception) {
            LocalDate.now()
        }
    }
}

class InstantAdapter : JsonSerializer<Instant>, JsonDeserializer<Instant> {
    override fun serialize(src: Instant?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.toEpochMilli() ?: 0L)
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Instant? {
        val str = json?.asString ?: return null
        if (str.isEmpty()) return null
        return try {
            val millis = str.toLongOrNull()
            if (millis != null) Instant.ofEpochMilli(millis) else Instant.parse(str)
        } catch (e: Exception) {
            null
        }
    }
}

@androidx.annotation.Keep
data class Lunar3YearCacheWrapper(
    val version: Int = LunarCacheManager.CACHE_SCHEMA_VERSION,
    val data: List<MoonAlmanacDay> = emptyList()
)

object LunarCacheManager {

    private const val CACHE_FILE_NAME = "lunar_events_3yr_cache.json"
    const val CACHE_SCHEMA_VERSION = 1

    val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
        .registerTypeAdapter(Instant::class.java, InstantAdapter())
        .create()

    fun getCached3YearMap(context: Context): List<MoonAlmanacDay>? {
        val file = File(context.cacheDir, CACHE_FILE_NAME)
        if (!file.exists() || !file.canRead()) return null
        return try {
            val json = file.readText()
            val type = object : TypeToken<Lunar3YearCacheWrapper>() {}.type
            val wrapper: Lunar3YearCacheWrapper? = gson.fromJson(json, type)
            if (wrapper != null && wrapper.version == CACHE_SCHEMA_VERSION) {
                wrapper.data
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun save3YearMapCache(context: Context, events: List<MoonAlmanacDay>) {
        try {
            val file = File(context.cacheDir, CACHE_FILE_NAME)
            val wrapper = Lunar3YearCacheWrapper(version = CACHE_SCHEMA_VERSION, data = events)
            val json = gson.toJson(wrapper)
            file.writeText(json)
        } catch (e: Exception) {
            // Ignore cache write failure
        }
    }

    fun generate3YearMap(
        location: LocationItem,
        zoneId: ZoneId = ZoneId.systemDefault(),
        onProgress: (percent: Int) -> Unit
    ): List<MoonAlmanacDay> {
        val startDate = LocalDate.now(zoneId)
        val filteredEvents = mutableListOf<MoonAlmanacDay>()

        // 36 months = 3 years
        for (m in 0 until 36) {
            val monthStart = startDate.plusMonths(m.toLong()).withDayOfMonth(1)
            val daysInMonth = monthStart.lengthOfMonth()
            val days = MoonAlmanacManager.getMoonAlmanac(monthStart, daysInMonth, location, zoneId)

            val special = days.filter { day ->
                day.isBloodMoon || day.isSupermoon || day.isBlueMoon || day.isHarvestMoon || day.eclipseInfo != null
            }
            filteredEvents.addAll(special)

            val percent = (((m + 1) * 100) / 36).coerceIn(0, 100)
            onProgress(percent)
        }

        return filteredEvents
    }
}
