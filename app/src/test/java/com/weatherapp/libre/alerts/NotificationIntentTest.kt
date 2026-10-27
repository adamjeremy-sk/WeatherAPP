package com.weatherapp.libre.alerts

import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.alerts.cache.GlobalAlertCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NotificationIntentTest {

    @Test
    fun testRequestCodeDerivation() {
        val loc1 = LocationItem(id = 0L, name = "Winnipeg", admin1 = "Manitoba", country = "Canada", latitude = 49.8951, longitude = -97.1384)
        val loc2 = LocationItem(id = 0L, name = "Edmonton", admin1 = "Alberta", country = "Canada", latitude = 53.5461, longitude = -113.4938)

        val alertId1 = "alert-tornado-123"
        val alertId2 = "alert-wind-456"

        val locKey1 = GlobalAlertCache.cacheKey(loc1)
        val locKey2 = GlobalAlertCache.cacheKey(loc2)

        val reqCode1 = "${alertId1}_${locKey1}".hashCode()
        val reqCode2 = "${alertId1}_${locKey2}".hashCode()
        val reqCode3 = "${alertId2}_${locKey1}".hashCode()
        val reqCode1Duplicate = "${alertId1}_${locKey1}".hashCode()

        // 1. Request codes differ for two locations sharing one alert identifier
        assertNotEquals(reqCode1, reqCode2)

        // 2. Request codes differ for two identifiers sharing one location
        assertNotEquals(reqCode1, reqCode3)

        // 3. Identical alert plus location yields an identical code
        assertEquals(reqCode1, reqCode1Duplicate)
    }

    @Test
    fun testTabResolutionMatchingKeyAndFallback() {
        val tab0 = LocationItem(id = 0L, name = "Winnipeg", admin1 = "Manitoba", country = "Canada", latitude = 49.8951, longitude = -97.1384)
        val tab1 = LocationItem(id = 0L, name = "Edmonton", admin1 = "Alberta", country = "Canada", latitude = 53.5461, longitude = -113.4938)
        val tab2 = LocationItem(id = 0L, name = "Quebec City", admin1 = "Quebec", country = "Canada", latitude = 46.8139, longitude = -71.2080)

        val savedTabs = listOf(tab0, tab1, tab2)

        fun resolveTabIndex(targetKey: String?, defaultFallback: Int = 0): Int {
            if (targetKey == null) return defaultFallback
            for (i in savedTabs.indices) {
                if (GlobalAlertCache.cacheKey(savedTabs[i]).equals(targetKey, ignoreCase = true)) {
                    return i
                }
            }
            return defaultFallback
        }

        val winnipegKey = GlobalAlertCache.cacheKey(tab0)
        val edmontonKey = GlobalAlertCache.cacheKey(tab1)
        val unknownKey = "non_existent_city_0.0000,0.0000"

        // 1. Tab resolution returns correct index for matching key
        assertEquals(0, resolveTabIndex(winnipegKey))
        assertEquals(1, resolveTabIndex(edmontonKey))

        // 2. Documented fallback for a non-matching key (returns default index 0 without crashing)
        assertEquals(0, resolveTabIndex(unknownKey))
        assertEquals(0, resolveTabIndex(null))
    }
}