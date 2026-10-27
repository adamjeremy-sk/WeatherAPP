package com.weatherapp.libre.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.LocationPreferences
import com.weatherapp.libre.MainActivity
import com.weatherapp.libre.R
import com.weatherapp.libre.alerts.cache.GlobalAlertCache
import com.weatherapp.libre.alerts.models.AlertSeverity
import com.weatherapp.libre.alerts.models.AlertSource
import com.weatherapp.libre.alerts.models.UnifiedAlert
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object AlertNotificationHelper {

    const val CHANNEL_SEVERE_ID = "eccc_severe_alerts_channel"
    const val CHANNEL_SEVERE_NAME = "Severe Weather Alerts (ECCC)"

    const val CHANNEL_DEFAULT_ID = "eccc_watches_channel"
    const val CHANNEL_DEFAULT_NAME = "Weather Watches and Statements (ECCC)"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val severeChannel = NotificationChannel(
                CHANNEL_SEVERE_ID,
                CHANNEL_SEVERE_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Severe weather warnings and emergency CAP-CP alerts from Environment and Climate Change Canada"
                enableVibration(true)
            }

            val defaultChannel = NotificationChannel(
                CHANNEL_DEFAULT_ID,
                CHANNEL_DEFAULT_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Weather watches, advisories, and special weather statements from Environment and Climate Change Canada"
            }

            notificationManager.createNotificationChannel(severeChannel)
            notificationManager.createNotificationChannel(defaultChannel)
        }
    }



    fun postUnifiedAlertNotification(
        context: Context,
        alert: UnifiedAlert,
        location: LocationItem,
        timezoneStr: String? = null,
        isTestFixture: Boolean = false
    ) {
        val prefs = LocationPreferences(context)
        if (!isTestFixture && prefs.getAlertConsentState() != LocationPreferences.ALERT_CONSENT_OPT_IN) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        createNotificationChannels(context)

        val isSevere = alert.severity == AlertSeverity.RED || alert.severity == AlertSeverity.ORANGE
        val channelId = if (isSevere) CHANNEL_SEVERE_ID else CHANNEL_DEFAULT_ID

        val zoneId = try {
            if (!timezoneStr.isNull_or_blank_compat()) ZoneId.of(timezoneStr) else ZoneId.systemDefault()
        } catch (e: Exception) {
            ZoneId.systemDefault()
        }

        val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a", Locale.getDefault())
        val expiresStr = alert.expires?.let {
            try {
                java.time.Instant.parse(it).atZone(zoneId).format(dateFormatter)
            } catch (e: Exception) {
                it
            }
        } ?: "Until further notice"

        val titlePrefix = if (isTestFixture) "TEST — " else "🚨 "
        val eventTitle = "$titlePrefix${alert.eventNative}"

        val areaDesc = alert.areaDesc ?: location.displayName

        val sourceStr = when (alert.source) {
            AlertSource.METEOALARM -> context.getString(R.string.source_meteoalarm)
            AlertSource.ECCC -> context.getString(R.string.source_eccc_protocol)
            else -> alert.source.name
        }

        val bigText = StringBuilder()
            .append(alert.headline ?: "${alert.eventNative} in effect for $areaDesc").append("\n\n")
            .apply {
                if (!alert.instruction.isNull_or_blank_compat()) {
                    append("Instructions:\n").append(alert.instruction).append("\n\n")
                } else if (!alert.description.isNull_or_blank_compat()) {
                    append("Description:\n").append(alert.description).append("\n\n")
                }
            }
            .append("Area: ").append(areaDesc).append("\n")
            .append("Expires: ").append(expiresStr).append("\n")
            .append("Source: ").append(sourceStr)
            .toString()

        val locationKey = GlobalAlertCache.cacheKey(location)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_LOCATION_KEY", locationKey)
            putExtra("EXTRA_ALERT_ID", alert.id)
        }

        val requestCode = "${alert.id}_$locationKey".hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_map_pin)
            .setContentTitle(eventTitle)
            .setContentText(alert.headline ?: "${alert.eventNative} in effect")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(if (isSevere) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setGroup(String.format(Locale.US, "WEATHER_ALERTS_%.4f_%.4f", location.latitude, location.longitude))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(alert.id.hashCode(), notification)
    }

    fun dismissNotification(context: Context, identifier: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(identifier.hashCode())
    }

    private fun String?.isNull_or_blank_compat(): Boolean = this == null || this.trim().isEmpty()
}
