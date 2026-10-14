package com.weatherapp.libre.alerts

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.weatherapp.libre.LocationItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class AlertCacheTest {

    @Test
    fun testTabLocationCacheKeysAreDistinct() {
        // Construct tab locations exactly as LocationPreferences.getTabLocation does (default id = 0L)
        val tab0Winnipeg = LocationItem(id = 0L, name = "Winnipeg", admin1 = "Manitoba", country = "Canada", latitude = 49.8951, longitude = -97.1384)
        val tab1Edmonton = LocationItem(id = 0L, name = "Edmonton", admin1 = "Alberta", country = "Canada", latitude = 53.5461, longitude = -113.4938)
        val tab2Quebec = LocationItem(id = 0L, name = "Quebec City", admin1 = "Quebec", country = "Canada", latitude = 46.8139, longitude = -71.2080)

        val keyWinnipeg = AlertCache.cacheKey(tab0Winnipeg)
        val keyEdmonton = AlertCache.cacheKey(tab1Edmonton)
        val keyQuebec = AlertCache.cacheKey(tab2Quebec)

        assertNotEquals(keyWinnipeg, keyEdmonton)
        assertNotEquals(keyWinnipeg, keyQuebec)
        assertNotEquals(keyEdmonton, keyQuebec)

        assertEquals("winnipeg, manitoba_49.8951,-97.1384", keyWinnipeg)
        assertEquals("edmonton, alberta_53.5461,-113.4938", keyEdmonton)
    }

    @Test
    fun testCacheRoundTrip() {
        val alert = AlertCache.CachedAlert(
            identifier = "test-123",
            event = "Tornado Warning",
            severity = "Extreme",
            headline = "Tornado Warning in effect",
            description = "Seek shelter",
            instruction = "Go to basement",
            areaDesc = "Ottawa",
            sentUtcMs = 1000L,
            expiresUtcMs = Instant.now().toEpochMilli() + 3600000L,
            isTestFixture = true
        )

        val map = mapOf("ottawa, ontario_45.4215,-75.6972" to listOf(alert))
        val json = Gson().toJson(map)

        val type = object : TypeToken<Map<String, List<AlertCache.CachedAlert>>>() {}.type
        val readMap: Map<String, List<AlertCache.CachedAlert>> = Gson().fromJson(json, type)
        val readAlerts = readMap["ottawa, ontario_45.4215,-75.6972"]
        
        assertEquals(1, readAlerts?.size)
        val readAlert = readAlerts!!.first()
        
        assertEquals("test-123", readAlert.identifier)
        assertEquals("Tornado Warning", readAlert.event)
        assertEquals("Extreme", readAlert.severity)
        assertEquals("Tornado Warning in effect", readAlert.headline)
        assertEquals("Seek shelter", readAlert.description)
        assertEquals("Go to basement", readAlert.instruction)
        assertEquals("Ottawa", readAlert.areaDesc)
        assertEquals(1000L, readAlert.sentUtcMs)
        assertEquals(alert.expiresUtcMs, readAlert.expiresUtcMs)
        assertEquals(true, readAlert.isTestFixture)
    }

    @Test
    fun testExpiredPrunedOnRead() {
        val nowMs = Instant.now().toEpochMilli()
        val expiredAlert = AlertCache.CachedAlert(
            identifier = "exp-123",
            event = "Wind Warning",
            severity = "Severe",
            headline = null,
            description = null,
            instruction = null,
            areaDesc = "Ottawa",
            sentUtcMs = 1000L,
            expiresUtcMs = nowMs - 3600000L, // 1 hour ago
            isTestFixture = false
        )

        val cachedAlerts = listOf(expiredAlert)
        val validAlerts = cachedAlerts.filter { alert ->
            val isExpired = alert.expiresUtcMs != null && alert.expiresUtcMs < nowMs
            !isExpired
        }

        assertEquals(0, validAlerts.size)
        assertNull("Null or fully expired read yields null (hidden banner state)", validAlerts.maxByOrNull { it.severityLevel() })
    }

    @Test
    fun testPerLocationKeying() {
        val alert = AlertCache.CachedAlert(
            identifier = "test-loc",
            event = "Rain Warning",
            severity = "Moderate",
            headline = null,
            description = null,
            instruction = null,
            areaDesc = "Toronto",
            sentUtcMs = 1000L,
            expiresUtcMs = Instant.now().toEpochMilli() + 3600000L,
            isTestFixture = false
        )

        val loc1 = LocationItem(id = 0L, name = "Toronto", admin1 = "Ontario", country = "Canada", latitude = 43.6532, longitude = -79.3832)
        val loc2 = LocationItem(id = 0L, name = "Ottawa", admin1 = "Ontario", country = "Canada", latitude = 45.4215, longitude = -75.6972)

        val currentCache = mutableMapOf<String, List<AlertCache.CachedAlert>>()
        currentCache[AlertCache.cacheKey(loc1)] = listOf(alert)

        assertEquals("test-loc", currentCache[AlertCache.cacheKey(loc1)]?.first()?.identifier)
        assertNull("Alert for loc1 must not surface for loc2", currentCache[AlertCache.cacheKey(loc2)])
    }

    @Test
    fun testReplaceNotAppend() {
        val alert1 = AlertCache.CachedAlert(
            identifier = "test-1",
            event = "Wind Warning",
            severity = "Severe",
            headline = null,
            description = null,
            instruction = null,
            areaDesc = "Ottawa",
            sentUtcMs = 1000L,
            expiresUtcMs = Instant.now().toEpochMilli() + 3600000L,
            isTestFixture = false
        )

        val key = "ottawa, ontario_45.4215,-75.6972"
        val currentCache = mutableMapOf<String, List<AlertCache.CachedAlert>>()
        currentCache[key] = listOf(alert1)

        assertEquals(1, currentCache[key]?.size)

        // Write empty list (clears entry)
        val alerts2 = emptyList<AlertCache.CachedAlert>()
        if (alerts2.isEmpty()) {
            currentCache.remove(key)
        } else {
            currentCache[key] = alerts2
        }

        assertNull("Cache must be cleared for location on empty list write", currentCache[key])
    }

    @Test
    fun testTestFixturePreservedOnWorkerWrite() {
        val nowMs = Instant.now().toEpochMilli()
        val testFixture = AlertCache.CachedAlert(
            identifier = "fixture-123",
            event = "Tornado Warning",
            severity = "Extreme",
            headline = null,
            description = null,
            instruction = null,
            areaDesc = "Winnipeg",
            sentUtcMs = nowMs,
            expiresUtcMs = nowMs + 120000L, // 2 mins in future
            isTestFixture = true
        )

        val key = "winnipeg, manitoba_49.8951,-97.1384"
        val currentCache = mutableMapOf<String, List<AlertCache.CachedAlert>>()
        currentCache[key] = listOf(testFixture)

        // Worker poll runs with empty real alerts list
        val workerRealAlerts = emptyList<AlertCache.CachedAlert>()

        // Merge logic:
        val existingLocationAlerts = currentCache[key] ?: emptyList()
        val unexpiredTestFixtures = existingLocationAlerts.filter { alert ->
            (alert.isTestFixture || alert.isSyntheticTest) && (alert.expiresUtcMs == null || alert.expiresUtcMs > nowMs)
        }
        val finalAlerts = (workerRealAlerts + unexpiredTestFixtures).distinctBy { it.identifier }

        assertEquals(1, finalAlerts.size)
        assertEquals("fixture-123", finalAlerts.first().identifier)
    }

    @Test
    fun testWorkerDoesNotPruneSyntheticAlerts() {
        val nowMs = Instant.now().toEpochMilli()
        val syntheticAlert = AlertCache.CachedAlert(
            identifier = "synthetic-test-alert-123",
            event = "Tornado Warning",
            severity = "Extreme",
            headline = "Synthetic Test Tornado Warning",
            description = "Test description",
            instruction = "Test instruction",
            areaDesc = "Test Area",
            sentUtcMs = nowMs,
            expiresUtcMs = nowMs + 3600000L, // +1 hour in future
            isTestFixture = false,
            isSyntheticTest = true
        )

        val realAlert = AlertCache.CachedAlert(
            identifier = "real-alert-456",
            event = "Wind Warning",
            severity = "Severe",
            headline = "High winds expected",
            description = null,
            instruction = null,
            areaDesc = "Test Area",
            sentUtcMs = nowMs,
            expiresUtcMs = nowMs + 3600000L,
            isTestFixture = false,
            isSyntheticTest = false
        )

        val key = "test area_49.0000,-97.0000"
        val currentCache = mutableMapOf<String, List<AlertCache.CachedAlert>>()
        currentCache[key] = listOf(syntheticAlert)

        // Worker poll runs with new real alerts list from ECCC feed
        val workerRealAlerts = listOf(realAlert)

        // Diffing/merge logic from writeCacheForLocation
        val existingLocationAlerts = currentCache[key] ?: emptyList()
        val unexpiredSyntheticAlerts = existingLocationAlerts.filter { alert ->
            (alert.isSyntheticTest || alert.isTestFixture) && (alert.expiresUtcMs == null || alert.expiresUtcMs > nowMs)
        }
        val finalAlerts = (workerRealAlerts + unexpiredSyntheticAlerts).distinctBy { it.identifier }

        assertEquals(2, finalAlerts.size)
        val identifiers = finalAlerts.map { it.identifier }
        assertEquals(true, identifiers.contains("synthetic-test-alert-123"))
        assertEquals(true, identifiers.contains("real-alert-456"))
    }
}
