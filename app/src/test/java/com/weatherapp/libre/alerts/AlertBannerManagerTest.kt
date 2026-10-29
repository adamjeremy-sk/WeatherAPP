package com.weatherapp.libre.alerts

import android.content.SharedPreferences
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.LocationPreferences
import com.weatherapp.libre.alerts.cache.GlobalAlertCache
import com.weatherapp.libre.alerts.models.AlertSeverity
import com.weatherapp.libre.alerts.models.AlertSource
import com.weatherapp.libre.alerts.models.UnifiedAlert
import com.weatherapp.libre.databinding.ActivityMainBinding
import com.weatherapp.libre.databinding.SectionAlertBannerBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlertBannerManagerTest {

    class InMemorySharedPreferences : SharedPreferences {
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

    class TestActivity : AppCompatActivity() {
        private val prefsMap = mutableMapOf<String, InMemorySharedPreferences>()

        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
            val key = name ?: "default"
            return prefsMap.getOrPut(key) { InMemorySharedPreferences() }
        }

        fun getInMemoryPrefs(name: String): SharedPreferences {
            return prefsMap.getOrPut(name) { InMemorySharedPreferences() }
        }
    }

    private lateinit var activity: TestActivity
    private lateinit var binding: ActivityMainBinding
    private lateinit var bannerBinding: SectionAlertBannerBinding
    private lateinit var locationPreferences: LocationPreferences
    private lateinit var bannerManager: AlertBannerManager

    private val ottawa = LocationItem(
        id = 1L,
        name = "Ottawa",
        admin1 = "Ontario",
        country = "Canada",
        countryCode = "CA",
        latitude = 45.4215,
        longitude = -75.6972,
    )

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(TestActivity::class.java).setup().get()
        binding = ActivityMainBinding.inflate(activity.layoutInflater)
        bannerBinding = SectionAlertBannerBinding.bind(binding.layoutAlertBanner.root)
        locationPreferences = LocationPreferences(activity)
        bannerManager = AlertBannerManager(activity, binding, locationPreferences)
    }

    @Test
    fun testBannerHiddenWhenNoAlerts() {
        locationPreferences.saveAlertConsentState(LocationPreferences.ALERT_CONSENT_OPT_IN)
        bannerManager.updateAlertBanner(ottawa)

        assertEquals(View.GONE, bannerBinding.root.visibility)
    }

    @Test
    fun testBannerShownForActiveAlert() {
        locationPreferences.saveAlertConsentState(LocationPreferences.ALERT_CONSENT_OPT_IN)

        val alert = UnifiedAlert(
            id = "alert-001",
            source = AlertSource.ECCC,
            locationLat = ottawa.latitude,
            locationLon = ottawa.longitude,
            severity = AlertSeverity.RED,
            eventNative = "Tornado Warning",
            headline = "Tornado Warning in effect for Ottawa",
            description = "Severe weather imminent",
            instruction = "Take shelter immediately",
            onset = Instant.now().toString(),
            expires = Instant.now().plusSeconds(3600).toString(),
            areaDesc = "Ottawa Region",
        )

        GlobalAlertCache.writeAlertsForLocation(activity, ottawa, listOf(alert))
        bannerManager.updateAlertBanner(ottawa)

        assertEquals(View.VISIBLE, bannerBinding.root.visibility)

        assertEquals("Tornado Warning in effect for Ottawa", bannerBinding.tvAlertHeadline.text.toString())

        assertTrue(
            "Title should contain TORNADO WARNING, got: ${bannerBinding.tvAlertHeaderTitle.text}",
            bannerBinding.tvAlertHeaderTitle.text.toString().contains("TORNADO WARNING"),
        )
    }

    @Test
    fun testDismissalPersists() {
        locationPreferences.saveAlertConsentState(LocationPreferences.ALERT_CONSENT_OPT_IN)

        val alertId = "alert-001"
        val alert = UnifiedAlert(
            id = alertId,
            source = AlertSource.ECCC,
            locationLat = ottawa.latitude,
            locationLon = ottawa.longitude,
            severity = AlertSeverity.RED,
            eventNative = "Tornado Warning",
            headline = "Tornado Warning in effect for Ottawa",
            description = "Severe weather imminent",
            instruction = "Take shelter immediately",
            onset = Instant.now().toString(),
            expires = Instant.now().plusSeconds(3600).toString(),
            areaDesc = "Ottawa Region",
        )

        GlobalAlertCache.writeAlertsForLocation(activity, ottawa, listOf(alert))
        bannerManager.updateAlertBanner(ottawa)
        assertEquals(View.VISIBLE, bannerBinding.root.visibility)

        bannerBinding.btnDismissAlert.performClick()

        // 1. Check helper method
        assertTrue(locationPreferences.isAlertDismissed(alertId, ottawa))

        // 2. Check coordinate-scoped key in in-memory preferences
        val locationPreferencesPrefs = activity.getInMemoryPrefs("weather_location_prefs")
        val coordKey = String.format(Locale.US, "%.4f_%.4f", ottawa.latitude, ottawa.longitude)
        val expectedKey = "app_alert_dismissed_${coordKey}_$alertId"
        assertTrue(
            "Expected preference key $expectedKey to be true in in-memory preferences",
            locationPreferencesPrefs.getBoolean(expectedKey, false),
        )

        // 3. Verify banner becomes GONE after dismissal
        assertEquals(View.GONE, bannerBinding.root.visibility)
    }

    @Test
    fun testIsTestBooleanHandling() {
        locationPreferences.saveAlertConsentState(LocationPreferences.ALERT_CONSENT_OPT_IN)

        val testAlert = UnifiedAlert(
            id = "alert-test-01",
            source = AlertSource.ECCC,
            locationLat = ottawa.latitude,
            locationLon = ottawa.longitude,
            severity = AlertSeverity.ORANGE,
            eventNative = "Severe Thunderstorm Watch",
            headline = "Test Watch Headline",
            description = "Test description",
            instruction = null,
            onset = Instant.now().toString(),
            expires = Instant.now().plusSeconds(3600).toString(),
            areaDesc = "Ottawa Region",
            isTest = true,
        )

        GlobalAlertCache.writeAlertsForLocation(activity, ottawa, listOf(testAlert))
        bannerManager.updateAlertBanner(ottawa)

        assertEquals(View.VISIBLE, bannerBinding.root.visibility)

        val titleText = bannerBinding.tvAlertHeaderTitle.text.toString()
        assertTrue(
            "Header title for test alert should start with 'TEST — ', got: '$titleText'",
            titleText.startsWith("TEST — "),
        )
    }
}
