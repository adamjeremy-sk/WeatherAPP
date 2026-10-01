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
import com.weatherapp.libre.MainActivity
import com.weatherapp.libre.R
import com.weatherapp.libre.alerts.models.CapAlert
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

    fun postNotification(
        context: Context,
        alert: CapAlert,
        location: LocationItem,
        timezoneStr: String?
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        createNotificationChannels(context)

        val info = alert.infoList.firstOrNull() ?: return
        val isSevere = info.severity.equals("Extreme", ignoreCase = true) || info.severity.equals("Severe", ignoreCase = true)
        val channelId = if (isSevere) CHANNEL_SEVERE_ID else CHANNEL_DEFAULT_ID

        val zoneId = try {
            if (!timezoneStr.isNull_or_blank_compat()) ZoneId.of(timezoneStr) else ZoneId.systemDefault()
        } catch (e: Exception) {
            ZoneId.systemDefault()
        }

        val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a", Locale.getDefault())
        val sentStr = alert.sent.atZone(zoneId).format(dateFormatter)
        val expiresStr = info.expires?.atZone(zoneId)?.format(dateFormatter) ?: "Until further notice"

        val titlePrefix = if (alert.isTestFixture) "TEST — " else "🚨 "
        val eventTitle = "$titlePrefix${info.event}"

        val areaDesc = info.areaList.firstOrNull()?.areaDesc ?: location.displayName

        val bigText = StringBuilder()
            .append(info.headline ?: "${info.event} in effect for $areaDesc").append("\n\n")
            .apply {
                if (!info.instruction.isNull_or_blank_compat()) {
                    append("Instructions:\n").append(info.instruction).append("\n\n")
                } else if (!info.description.isNull_or_blank_compat()) {
                    append("Description:\n").append(info.description).append("\n\n")
                }
            }
            .append("Area: ").append(areaDesc).append("\n")
            .append("Issued: ").append(sentStr).append("\n")
            .append("Expires: ").append(expiresStr).append("\n\n")
            .append("Source: Environment and Climate Change Canada")
            .toString()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_LOCATION_ID", location.id)
            putExtra("EXTRA_ALERT_ID", alert.identifier)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.identifier.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_location_pin)
            .setContentTitle(eventTitle)
            .setContentText(info.headline ?: "${info.event} in effect")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(if (isSevere) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setGroup("WEATHER_ALERTS_${location.id}")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(alert.identifier.hashCode(), notification)
    }

    fun dismissNotification(context: Context, identifier: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(identifier.hashCode())
    }

    private fun String?.isNull_or_blank_compat(): Boolean = this == null || this.trim().isEmpty()
}
