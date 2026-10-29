package com.weatherapp.libre.alerts

import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.Build
import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.LocationPreferences
import com.weatherapp.libre.alerts.models.AlertSeverity
import com.weatherapp.libre.alerts.models.AlertSource
import com.weatherapp.libre.alerts.models.UnifiedAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlertNotificationHelperTest {

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

    private class TestContext(base: Context) : ContextWrapper(base) {
        private val prefsMap = mutableMapOf<String, InMemorySharedPreferences>()

        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
            val key = name ?: "default"
            return prefsMap.getOrPut(key) { InMemorySharedPreferences() }
        }
    }

    private lateinit var testContext: TestContext
    private lateinit var notificationManager: NotificationManager
    private lateinit var locationPreferences: LocationPreferences

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
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

        testContext = TestContext(app)
        notificationManager = testContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        locationPreferences = LocationPreferences(testContext)
    }

    @Test
    fun testChannelCreation() {
        AlertNotificationHelper.createNotificationChannels(testContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val severeChannel = notificationManager.getNotificationChannel(AlertNotificationHelper.CHANNEL_SEVERE_ID)
            assertNotNull("Severe channel must be created", severeChannel)
            assertEquals(NotificationManager.IMPORTANCE_HIGH, severeChannel.importance)

            val defaultChannel = notificationManager.getNotificationChannel(AlertNotificationHelper.CHANNEL_DEFAULT_ID)
            assertNotNull("Default channel must be created", defaultChannel)
            assertEquals(NotificationManager.IMPORTANCE_DEFAULT, defaultChannel.importance)
        }
    }

    @Test
    fun testSeverityMapping() {
        locationPreferences.saveAlertConsentState(LocationPreferences.ALERT_CONSENT_OPT_IN)

        val redAlert = UnifiedAlert(
            id = "alert-red-01",
            source = AlertSource.ECCC,
            locationLat = ottawa.latitude,
            locationLon = ottawa.longitude,
            severity = AlertSeverity.RED,
            eventNative = "Tornado Warning",
            headline = "Tornado Warning in effect",
            description = null,
            instruction = null,
            onset = null,
            expires = null,
            areaDesc = "Ottawa",
        )

        AlertNotificationHelper.postUnifiedAlertNotification(testContext, redAlert, ottawa)

        val shadowNm = shadowOf(notificationManager)
        assertEquals(1, shadowNm.allNotifications.size)
        val notifRed = shadowNm.allNotifications[0]
        assertEquals(AlertNotificationHelper.CHANNEL_SEVERE_ID, notifRed.channelId)

        val yellowAlert = UnifiedAlert(
            id = "alert-yellow-01",
            source = AlertSource.ECCC,
            locationLat = ottawa.latitude,
            locationLon = ottawa.longitude,
            severity = AlertSeverity.YELLOW,
            eventNative = "Frost Advisory",
            headline = "Frost Advisory in effect",
            description = null,
            instruction = null,
            onset = null,
            expires = null,
            areaDesc = "Ottawa",
        )

        AlertNotificationHelper.postUnifiedAlertNotification(testContext, yellowAlert, ottawa)

        assertEquals(2, shadowNm.allNotifications.size)
        val notifYellow = shadowNm.allNotifications[1]
        assertEquals(AlertNotificationHelper.CHANNEL_DEFAULT_ID, notifYellow.channelId)
    }

    @Test
    fun testConsentGate() {
        locationPreferences.saveAlertConsentState(LocationPreferences.ALERT_CONSENT_OPT_OUT)

        val alert = UnifiedAlert(
            id = "alert-gate-01",
            source = AlertSource.ECCC,
            locationLat = ottawa.latitude,
            locationLon = ottawa.longitude,
            severity = AlertSeverity.RED,
            eventNative = "Tornado Warning",
            headline = "Tornado Warning in effect",
            description = null,
            instruction = null,
            onset = null,
            expires = null,
            areaDesc = "Ottawa",
        )

        AlertNotificationHelper.postUnifiedAlertNotification(
            context = testContext,
            alert = alert,
            location = ottawa,
            isTestFixture = false,
        )

        val shadowNm = shadowOf(notificationManager)
        assertEquals("No notification should be posted when consent is OPT_OUT", 0, shadowNm.allNotifications.size)
    }

    @Test
    fun testTestFixtureBypass() {
        locationPreferences.saveAlertConsentState(LocationPreferences.ALERT_CONSENT_OPT_OUT)

        val testFixtureAlert = UnifiedAlert(
            id = "alert-fixture-01",
            source = AlertSource.ECCC,
            locationLat = ottawa.latitude,
            locationLon = ottawa.longitude,
            severity = AlertSeverity.RED,
            eventNative = "Tornado Warning",
            headline = "TEST — Tornado Warning in effect",
            description = null,
            instruction = null,
            onset = null,
            expires = null,
            areaDesc = "Ottawa",
            isTest = true,
        )

        AlertNotificationHelper.postUnifiedAlertNotification(
            context = testContext,
            alert = testFixtureAlert,
            location = ottawa,
            isTestFixture = true,
        )

        val shadowNm = shadowOf(notificationManager)
        assertEquals("Test fixture notification must bypass consent gate and be posted", 1, shadowNm.allNotifications.size)
        val postedNotif = shadowNm.allNotifications[0]
        assertNotNull(postedNotif)
    }
}
