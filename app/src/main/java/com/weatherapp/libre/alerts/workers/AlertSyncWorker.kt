package com.weatherapp.libre.alerts.workers

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.weatherapp.libre.LocationPreferences
import com.weatherapp.libre.alerts.cache.GlobalAlertCache
import com.weatherapp.libre.alerts.models.AlertFetchResult
import com.weatherapp.libre.alerts.routing.AlertProviderRouter
import com.weatherapp.libre.alerts.utils.CountryCodeResolver
import java.util.concurrent.TimeUnit

class AlertSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "AlertSyncWorker"
        const val WORK_NAME = "GlobalAlertSyncWorker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<AlertSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "Scheduled background global alert sync worker.")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Cancelled background global alert sync worker.")
        }

        fun runImmediateSync(context: Context) {
            val request = androidx.work.OneTimeWorkRequestBuilder<AlertSyncWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "GlobalAlertImmediateSyncWork",
                androidx.work.ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d(TAG, "Enqueued immediate global alert sync worker.")
        }
    }

    override suspend fun doWork(): Result {
        val prefs = LocationPreferences(appContext)
        if (prefs.getAlertConsentState() != LocationPreferences.ALERT_CONSENT_OPT_IN) {
            Log.d(TAG, "Alerts disabled in preferences. Cancelling GlobalAlertSyncWorker.")
            cancel(appContext)
            return Result.success()
        }

        var totalAttempts = 0
        var failedAttempts = 0

        val tabCount = prefs.getTabCount()
        for (i in 0 until tabCount) {
            val loc = prefs.getTabLocation(i)
            val countryCode = CountryCodeResolver.resolveAndCacheCountryCode(appContext, loc, prefs)
                ?: loc.countryCode

            val provider = AlertProviderRouter.getProviderForCountryCode(countryCode)
            if (provider != null) {
                totalAttempts++
                val previousAlerts = GlobalAlertCache.readAlertsForLocation(appContext, loc)
                val previousIds = previousAlerts.map { it.id }.toSet()

                when (val result = provider.fetchAlertsForLocation(loc)) {
                    is AlertFetchResult.Success -> {
                        GlobalAlertCache.writeAlertsForLocation(appContext, loc, result.alerts)

                        val newAlerts = result.alerts.filter { !previousIds.contains(it.id) }
                        for (newAlert in newAlerts) {
                            com.weatherapp.libre.alerts.AlertNotificationHelper.postUnifiedAlertNotification(
                                context = appContext,
                                alert = newAlert,
                                location = loc
                            )
                        }
                    }
                    is AlertFetchResult.Failure -> {
                        failedAttempts++
                        Log.w(TAG, "Alert sync failed for ${loc.name}", result.cause)
                        // DO NOT write to GlobalAlertCache
                        // DO NOT clear existing alerts
                        // DO NOT trigger notifications
                    }
                }
            }
        }

        GlobalAlertCache.clearExpiredAlerts(appContext)

        if (totalAttempts > 0 && totalAttempts == failedAttempts) {
            return Result.retry()
        }
        return Result.success()
    }
}
