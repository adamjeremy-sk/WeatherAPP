package com.weatherapp.libre.alerts.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.LocationPreferences
import com.weatherapp.libre.alerts.AlertLogEntry
import com.weatherapp.libre.alerts.AlertLogRepository
import com.weatherapp.libre.alerts.cache.GlobalAlertCache
import com.weatherapp.libre.alerts.models.AlertFetchResult
import com.weatherapp.libre.alerts.models.AlertSeverity
import com.weatherapp.libre.alerts.models.AlertSource
import com.weatherapp.libre.alerts.models.UnifiedAlert
import com.weatherapp.libre.alerts.providers.AlertProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests for [AlertSyncWorker] hardening invariants:
 *  - consent gate (denied consent => no provider call, success)
 *  - failure isolation (a failed fetch must NOT wipe the existing alert cache)
 *  - alert dedup (only NEW alerts are notified/logged; already-cached ones are skipped)
 *
 * Follows the established project conventions: JUnit 4, Robolectric, a hand-rolled fake
 * [AlertProvider] (no mocking frameworks), and TestListenableWorkerBuilder to construct
 * the worker with valid WorkerParameters.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlertSyncWorkerTest {

    private lateinit var context: Context
    private lateinit var prefs: LocationPreferences

    private val toronto = LocationItem(
        id = 1001L,
        name = "Toronto",
        admin1 = "Ontario",
        country = "Canada",
        countryCode = "CA",
        latitude = 43.6532,
        longitude = -79.3832,
        elevationMeters = 76.0
    )

    /** A controllable fake provider that records every fetch call. */
    private class FakeProvider(private val result: AlertFetchResult) : AlertProvider {
        val fetchCallCount = AtomicInteger(0)
        override val source: AlertSource get() = AlertSource.ECCC
        override suspend fun fetchAlertsForLocation(location: LocationItem): AlertFetchResult {
            fetchCallCount.incrementAndGet()
            return result
        }
    }

    private fun alert(id: String, event: String): UnifiedAlert = UnifiedAlert(
        id = id,
        source = AlertSource.ECCC,
        locationLat = toronto.latitude,
        locationLon = toronto.longitude,
        severity = AlertSeverity.ORANGE,
        eventNative = event,
        headline = event,
        description = "desc $id",
        instruction = null,
        onset = null,
        expires = OffsetDateTime.now().plusHours(6).toString(),
        areaDesc = "Toronto"
    )

    private fun seedOptedInSingleTab() {
        val raw = context.getSharedPreferences("weather_location_prefs", Context.MODE_PRIVATE)
        raw.edit()
            .putInt("app_location_tab_count", 1)
            .putInt("app_alert_consent_state", LocationPreferences.ALERT_CONSENT_OPT_IN)
            .apply()
        prefs.saveTabLocation(0, toronto)
    }

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        // Clean all relevant prefs so tests are fully isolated.
        for (name in listOf("weather_location_prefs", "global_alert_cache_prefs", "alert_log_prefs")) {
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
        }
        prefs = LocationPreferences(context)
    }

    private fun buildWorker(provider: AlertProvider): AlertSyncWorker {
        val worker = TestListenableWorkerBuilder.from(context, AlertSyncWorker::class.java)
            .build()
        worker.providerOverride = provider
        return worker
    }

    // ---------------------------------------------------------------------
    // Consent gate
    // ---------------------------------------------------------------------

    @Test
    fun testConsentGate() {
        // Consent is DENIED (OPT_OUT) -> worker must return success and never call the provider.
        val raw = context.getSharedPreferences("weather_location_prefs", Context.MODE_PRIVATE)
        raw.edit()
            .putInt("app_location_tab_count", 1)
            .putInt("app_alert_consent_state", LocationPreferences.ALERT_CONSENT_OPT_OUT)
            .apply()
        prefs.saveTabLocation(0, toronto)

        val provider = FakeProvider(AlertFetchResult.Success(listOf(alert("a1", "Tornado Warning"))))
        val worker = buildWorker(provider)

        val result = runBlocking { worker.doWork() }

        assertTrue(
            "A consent-gated worker must return success",
            result is ListenableWorker.Result.Success
        )
        assertEquals(
            "The provider must NEVER be called when consent is denied",
            0,
            provider.fetchCallCount.get()
        )
    }

    // ---------------------------------------------------------------------
    // Failure isolation
    // ---------------------------------------------------------------------

    @Test
    fun testFailureIsolation() {
        seedOptedInSingleTab()

        // Pre-populate the cache with a valid (unexpired) alert that must survive the failure.
        val existing = alert("existing-1", "Blizzard Warning")
        GlobalAlertCache.writeAlertsForLocation(context, toronto, listOf(existing))
        assertEquals(
            "Sanity: cache must contain the pre-existing alert",
            1,
            GlobalAlertCache.readAlertsForLocation(context, toronto).size
        )

        // Provider fails with an IOException.
        val provider = FakeProvider(AlertFetchResult.Failure(IOException("Network unreachable")))
        val worker = buildWorker(provider)

        val result = runBlocking { worker.doWork() }

        assertTrue(
            "An all-failed sync must return retry()",
            result is ListenableWorker.Result.Retry
        )
        assertEquals("Provider must be called exactly once", 1, provider.fetchCallCount.get())

        // The pre-existing alert must NOT have been wiped by the failed fetch.
        val cached = GlobalAlertCache.readAlertsForLocation(context, toronto)
        assertEquals(
            "A failed fetch must not wipe the existing alert cache",
            1,
            cached.size
        )
        assertEquals("existing-1", cached.first().id)
    }

    // ---------------------------------------------------------------------
    // Alert dedup
    // ---------------------------------------------------------------------

    @Test
    fun testAlertDedup() {
        seedOptedInSingleTab()

        // Alert A is already cached; the provider returns A + B.
        val alertA = alert("alert-A", "Severe Thunderstorm Warning")
        GlobalAlertCache.writeAlertsForLocation(context, toronto, listOf(alertA))

        val alertB = alert("alert-B", "Tornado Warning")
        val provider = FakeProvider(AlertFetchResult.Success(listOf(alertA, alertB)))
        val worker = buildWorker(provider)

        runBlocking { AlertLogRepository.clearAll(context) }

        val result = runBlocking { worker.doWork() }

        assertTrue("A successful dedup sync must return success", result is ListenableWorker.Result.Success)

        // Only the NEW alert (B) must be logged — A was already known and must be skipped.
        val entries = AlertLogRepository.getAllEntries(context)
        assertEquals("Only the new alert must be logged (dedup)", 1, entries.size)
        assertEquals("Tornado Warning", entries.first().alertEvent)
    }

    // ---------------------------------------------------------------------
    // Repeated alert notifications (Step 2): the worker must notify only
    // alerts that are net-new in the reconciled cache. Superseded Alerts and
    // Cancels returned by the provider on every sync must NOT be re-notified
    // or re-logged, and a Cancel must drop its whole chain from the cache.
    // ---------------------------------------------------------------------

    /** Builds a [UnifiedAlert] with explicit msgType, references and sent timestamp. */
    private fun fogAlert(
        id: String,
        msgType: String,
        references: List<String>,
        sent: String,
        event: String = "Dense Fog Advisory"
    ): UnifiedAlert =
        UnifiedAlert(
            id = id,
            source = AlertSource.ECCC,
            locationLat = toronto.latitude,
            locationLon = toronto.longitude,
            severity = AlertSeverity.ORANGE,
            eventNative = event,
            headline = event,
            description = "desc $id",
            instruction = null,
            onset = null,
            expires = OffsetDateTime.now().plusHours(6).toString(),
            areaDesc = "Toronto",
            msgType = msgType,
            references = references,
            sent = sent
        )

    /**
     * Runs one simulated sync: builds a fresh [FakeProvider] (fixed result) and worker,
     * invokes doWork(), and returns the log entries and cached alert ids afterwards.
     */
    private fun runSync(alerts: List<UnifiedAlert>): Pair<List<AlertLogEntry>, List<String>> =
        runBlocking {
            val provider = FakeProvider(AlertFetchResult.Success(alerts))
            val worker = buildWorker(provider)
            worker.doWork()
            AlertLogRepository.getAllEntries(context) to GlobalAlertCache.readAlertsForLocation(context, toronto).map { it.id }
        }

    @Test
    fun testFogChainForwardOrder_notifiesOnlyNetNew() {
        seedOptedInSingleTab()
        runBlocking { AlertLogRepository.clearAll(context) }

        val a = fogAlert("fog-A", "Alert", emptyList(), "2024-01-01T00:00:00Z")
        val u = fogAlert("fog-U", "Update", listOf("fog-A"), "2024-01-01T01:00:00Z")
        val c = fogAlert("fog-C", "Cancel", listOf("fog-A"), "2024-01-01T02:00:00Z")

        runSync(listOf(a))
        runSync(listOf(a, u))
        runSync(listOf(a, u))
        runSync(listOf(a, u))
        runSync(listOf(a, u, c))
        runSync(listOf(a, u, c))

        val entries = runBlocking { AlertLogRepository.getAllEntries(context) }
        assertEquals("Fog chain must log exactly 2 entries (A once, U once)", 2, entries.size)
        assertEquals("The surviving Update must be logged", "Dense Fog Advisory", entries.first().alertEvent)
        assertEquals("The original Alert must be logged", "Dense Fog Advisory", entries.last().alertEvent)

        val cached = GlobalAlertCache.readAlertsForLocation(context, toronto)
        assertEquals("A Cancel must drop the whole chain, leaving an empty cache", 0, cached.size)
    }

    @Test
    fun testFogChainReversedOrder_notifiesOnlyNetNew() {
        seedOptedInSingleTab()
        runBlocking { AlertLogRepository.clearAll(context) }

        val a = fogAlert("fog-A", "Alert", emptyList(), "2024-01-01T00:00:00Z")
        val u = fogAlert("fog-U", "Update", listOf("fog-A"), "2024-01-01T01:00:00Z")
        val c = fogAlert("fog-C", "Cancel", listOf("fog-A"), "2024-01-01T02:00:00Z")

        // Same chain, each provider list reversed. Reconcile is order-independent.
        runSync(listOf(u, a))
        runSync(listOf(u, a))
        runSync(listOf(u, a))
        runSync(listOf(u, a))
        runSync(listOf(c, u, a))
        runSync(listOf(c, u, a))

        val entries = runBlocking { AlertLogRepository.getAllEntries(context) }
        // Reconcile picks the latest-sent survivor (U), so A is superseded on the very
        // first sync and never becomes a survivor; only U is logged. Order of the provider
        // list does not change this, unlike the forward case where sync 1 is [A] alone.
        assertEquals("Reversed fog chain must log exactly 1 entry (only the surviving Update)", 1, entries.size)

        val cached = GlobalAlertCache.readAlertsForLocation(context, toronto)
        assertEquals("A Cancel must drop the whole chain, leaving an empty cache", 0, cached.size)
    }

    @Test
    fun testFogChainSteadyState_notifiesOnlyNetNew() {
        seedOptedInSingleTab()
        runBlocking { AlertLogRepository.clearAll(context) }

        val a = fogAlert("fog-A", "Alert", emptyList(), "2024-01-01T00:00:00Z")
        val u = fogAlert("fog-U", "Update", listOf("fog-A"), "2024-01-01T01:00:00Z")

        // Steady state: the surviving Update is net-new on the first sync, then stable.
        runSync(listOf(a, u))
        runSync(listOf(a, u))
        runSync(listOf(a, u))

        val entries = runBlocking { AlertLogRepository.getAllEntries(context) }
        // Reconcile picks the latest-sent survivor (U) on the first sync, so A is never a
        // survivor and only U is logged; repeated [A, U] syncs add nothing.
        assertEquals("Steady-state fog chain must log exactly 1 entry (only the surviving Update)", 1, entries.size)

        val cached = GlobalAlertCache.readAlertsForLocation(context, toronto)
        assertEquals("The surviving Update must remain cached", 1, cached.size)
        assertEquals("fog-U", cached.first().id)
    }

    @Test
    fun testUnrelatedNewAlertIsLoggedOnce() {
        seedOptedInSingleTab()
        runBlocking { AlertLogRepository.clearAll(context) }

        val a = fogAlert("fog-A", "Alert", emptyList(), "2024-01-01T00:00:00Z")
        val u = fogAlert("fog-U", "Update", listOf("fog-A"), "2024-01-01T01:00:00Z")
        val b = fogAlert("tornado-B", "Alert", emptyList(), "2024-01-01T03:00:00Z", event = "Tornado Warning")

        // First two syncs of the fog chain, then an unrelated new alert B arrives.
        runSync(listOf(a))
        runSync(listOf(a, u))
        runSync(listOf(a, u, b))

        val entries = runBlocking { AlertLogRepository.getAllEntries(context) }
        assertEquals("Fog chain plus an unrelated new alert must log exactly 3 entries", 3, entries.size)
        assertEquals("The unrelated new alert B must be logged exactly once", 1, entries.count { it.alertEvent == "Tornado Warning" })

        val cached = GlobalAlertCache.readAlertsForLocation(context, toronto)
        val cachedIds = cached.map { it.id }.toSet()
        assertTrue("The unrelated new alert B must be cached", cachedIds.contains("tornado-B"))
        assertEquals("The surviving Update must remain cached", 1, cached.count { it.id == "fog-U" })
    }
}
