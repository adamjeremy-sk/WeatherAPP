package com.weatherapp.libre.alerts

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
import java.util.concurrent.TimeUnit

class AlertPollWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "AlertPollWorker"
        const val WORK_NAME = "EcccAlertBackgroundPollWorker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<AlertPollWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "Scheduled background alert poll worker.")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Cancelled background alert poll worker.")
        }
    }

    override suspend fun doWork(): Result {
        val prefs = LocationPreferences(appContext)
        if (prefs.getAlertConsentState() != LocationPreferences.ALERT_CONSENT_OPT_IN) {
            Log.d(TAG, "Alerts disabled in preferences. Cancelling worker.")
            cancel(appContext)
            return Result.success()
        }

        val alertManager = AlertManager(appContext, prefs)
        val tabCount = prefs.getTabCount()

        for (i in 0 until tabCount) {
            val loc = prefs.getTabLocation(i)
            val isFirstTab = (i == 0)
            alertManager.fetchAndEvaluateAlertsForLocation(
                location = loc,
                timezoneStr = null,
                isForegroundActiveLocation = isFirstTab,
                forceRefresh = true
            )
        }

        return Result.success()
    }
}
