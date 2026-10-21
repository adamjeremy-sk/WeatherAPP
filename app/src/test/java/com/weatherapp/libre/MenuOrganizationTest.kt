package com.weatherapp.libre

import android.content.ContextWrapper
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MenuOrganizationTest {

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
            private val temp = mutableMapOf<String, Any?>()

            override fun putString(key: String, value: String?): SharedPreferences.Editor { temp[key] = value; return this }
            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor { temp[key] = values; return this }
            override fun putInt(key: String, value: Int): SharedPreferences.Editor { temp[key] = value; return this }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor { temp[key] = value; return this }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor { temp[key] = value; return this }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor { temp[key] = value; return this }
            override fun remove(key: String): SharedPreferences.Editor { temp[key] = null; return this }
            override fun clear(): SharedPreferences.Editor { map.clear(); return this }
            override fun apply() { commit() }
            override fun commit(): Boolean {
                for ((k, v) in temp) {
                    if (v == null) map.remove(k) else map[k] = v
                }
                return true
            }
        }
    }

    private lateinit var prefs: LocationPreferences
    private lateinit var sharedPrefs: InMemorySharedPreferences

    @Before
    fun setUp() {
        sharedPrefs = InMemorySharedPreferences()
        val context = object : ContextWrapper(null) {
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
                return sharedPrefs
            }
        }
        prefs = LocationPreferences(context)
    }

    @Test
    fun testMenuOrderPreferenceRoundTrip() {
        val initialOrder = prefs.getMenuOrder()
        assertEquals(10, initialOrder.size)
        assertEquals("CURRENT", initialOrder[0])
        assertEquals("SEPARATOR", initialOrder[3])

        val customOrder = listOf(
            "SOLUNAR",
            "DARK_SKY",
            "SEPARATOR",
            "CURRENT",
            "HOURLY",
            "SEVEN_DAY",
            "NATURAL_LIGHT",
            "NIGHT_VISIBILITY",
            "SEPARATOR",
            "LUNAR_CALENDAR"
        )
        prefs.saveMenuOrder(customOrder)

        val retrievedOrder = prefs.getMenuOrder()
        assertEquals(customOrder, retrievedOrder)
    }

    @Test
    fun testSeparatorCountRoundTripAndClamping() {
        assertEquals(2, prefs.getMenuOrder().count { it == "SEPARATOR" })
    }

    @Test
    fun testEnabledFlagsPreferenceRoundTrip() {
        val initialFlags = prefs.getMenuEnabledFlags()
        assertTrue(initialFlags.values.all { it })

        val customFlags = initialFlags.toMutableMap()
        customFlags[InlineSubTab.SOLUNAR] = false
        prefs.saveMenuEnabledFlags(customFlags)

        val retrievedFlags = prefs.getMenuEnabledFlags()
        assertFalse(retrievedFlags[InlineSubTab.SOLUNAR]!!)
        assertTrue(retrievedFlags[InlineSubTab.CURRENT]!!)
    }

    @Test
    fun testDefaultLaunchSubTabPreferenceRoundTrip() {
        assertEquals(InlineSubTab.CURRENT, prefs.getDefaultLaunchSubTab())

        prefs.saveDefaultLaunchSubTab(InlineSubTab.HOURLY)
        assertEquals(InlineSubTab.HOURLY, prefs.getDefaultLaunchSubTab())
    }

    @Test
    fun testLaunchResolutionFreshVsRecreation() {
        prefs.saveDefaultLaunchSubTab(InlineSubTab.SOLUNAR)
        prefs.saveInlineSubTab(InlineSubTab.HOURLY.ordinal)

        val isFreshLaunch = true
        val freshResolved = if (isFreshLaunch) prefs.getDefaultLaunchSubTab() else InlineSubTab.entries[prefs.getSavedInlineSubTab()]
        assertEquals(InlineSubTab.SOLUNAR, freshResolved)

        val isRecreation = false
        val recreationResolved = if (isRecreation) prefs.getDefaultLaunchSubTab() else InlineSubTab.entries[prefs.getSavedInlineSubTab()]
        assertEquals(InlineSubTab.HOURLY, recreationResolved)
    }

    @Test
    fun testActiveTabValidityEdgeCase() {
        prefs.saveDefaultLaunchSubTab(InlineSubTab.CURRENT)
        
        fun resolveActiveTabAfterToggle(
            activeTab: InlineSubTab,
            showSevenDayInMenu: Boolean,
            enabledFlags: Map<InlineSubTab, Boolean>
        ): InlineSubTab {
            val isEnabled = enabledFlags[activeTab] ?: true
            val isSevenDayInvalid = (activeTab == InlineSubTab.SEVEN_DAY && !showSevenDayInMenu)
            return if (!isEnabled || isSevenDayInvalid) {
                prefs.getDefaultLaunchSubTab()
            } else {
                activeTab
            }
        }

        // Active tab HOURLY is enabled -> stays HOURLY
        val flags1 = mapOf(InlineSubTab.HOURLY to true)
        assertEquals(InlineSubTab.HOURLY, resolveActiveTabAfterToggle(InlineSubTab.HOURLY, false, flags1))

        // Active tab HOURLY is disabled -> bumped to DEFAULT (CURRENT)
        val flags2 = mapOf(InlineSubTab.HOURLY to false)
        assertEquals(InlineSubTab.CURRENT, resolveActiveTabAfterToggle(InlineSubTab.HOURLY, false, flags2))

        // Active tab SEVEN_DAY with menu mode OFF -> bumped to DEFAULT (CURRENT)
        assertEquals(InlineSubTab.CURRENT, resolveActiveTabAfterToggle(InlineSubTab.SEVEN_DAY, false, mapOf(InlineSubTab.SEVEN_DAY to true)))
    }

    @Test
    fun testSevenDayInlineVisibilityPredicate() {
        fun is7DayVisible(isSevenDayAsMenuItem: Boolean, subTab: InlineSubTab): Boolean {
            val isInlineMode = !isSevenDayAsMenuItem
            return isInlineMode && (subTab == InlineSubTab.CURRENT || subTab == InlineSubTab.HOURLY)
        }

        assertTrue(is7DayVisible(isSevenDayAsMenuItem = false, subTab = InlineSubTab.CURRENT))
        assertTrue(is7DayVisible(isSevenDayAsMenuItem = false, subTab = InlineSubTab.HOURLY))
        assertFalse(is7DayVisible(isSevenDayAsMenuItem = false, subTab = InlineSubTab.NATURAL_LIGHT))
        assertFalse(is7DayVisible(isSevenDayAsMenuItem = false, subTab = InlineSubTab.SEVEN_DAY))

        assertFalse(is7DayVisible(isSevenDayAsMenuItem = true, subTab = InlineSubTab.CURRENT))
        assertFalse(is7DayVisible(isSevenDayAsMenuItem = true, subTab = InlineSubTab.HOURLY))
        assertFalse(is7DayVisible(isSevenDayAsMenuItem = true, subTab = InlineSubTab.SEVEN_DAY))
    }

    @Test
    fun testBuildMenuListFromOrderAndFlags() {
        prefs.saveSevenDayAsMenuItem(true)
        val order = listOf("HOURLY", "CURRENT", "SEPARATOR", "SEVEN_DAY", "SOLUNAR")
        val flags = mapOf(
            InlineSubTab.HOURLY to true,
            InlineSubTab.CURRENT to true,
            InlineSubTab.SEVEN_DAY to false,
            InlineSubTab.SOLUNAR to true
        )

        fun buildVisibleMenuItems(sevenDayInMenu: Boolean, orderList: List<String>, enabledMap: Map<InlineSubTab, Boolean>): List<String> {
            val result = mutableListOf<String>()
            for (item in orderList) {
                if (item == "SEPARATOR") {
                    result.add("SEPARATOR")
                    continue
                }
                val subTab = InlineSubTab.valueOf(item)
                if (subTab == InlineSubTab.SEVEN_DAY && !sevenDayInMenu) continue
                if (enabledMap[subTab] == true) {
                    result.add(item)
                }
            }
            return result
        }

        val visibleTabs = buildVisibleMenuItems(
            sevenDayInMenu = prefs.getSevenDayAsMenuItem(),
            orderList = order,
            enabledMap = flags
        )

        assertEquals(listOf("HOURLY", "CURRENT", "SEPARATOR", "SOLUNAR"), visibleTabs)
    }
}
