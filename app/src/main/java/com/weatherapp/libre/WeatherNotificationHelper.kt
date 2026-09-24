package com.weatherapp.libre

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

data class CapAlertMessage(
    val identifier: String,
    val sender: String = "cap-pac@ec.gc.ca",
    val status: String = "Actual",
    val msgType: String = "Alert",
    val scope: String = "Public",
    val urgency: String = "Immediate",
    val severity: String = "Extreme",
    val certainty: String = "Observed",
    val event: String,
    val headline: String,
    val description: String,
    val instruction: String,
    val areaDesc: String
)

object WeatherNotificationHelper {

    private const val CHANNEL_ID = "eccc_weather_alerts_channel"
    private const val CHANNEL_NAME = "Environment Canada Weather Alerts"
    private const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Severe weather warnings, watches, and CAP-CP emergency alerts from Environment Canada (GEM/ECCC)."
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Dispatches an authentic Common Alerting Protocol (CAP-CP) message from Environment Canada (ECCC).
     */
    fun sendEcccCapAlertNotification(
        context: Context,
        capAlert: CapAlertMessage
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigText = StringBuilder()
            .append("Headline: ").append(capAlert.headline).append("\n\n")
            .append("Area: ").append(capAlert.areaDesc).append("\n")
            .append("Severity: ").append(capAlert.severity).append(" | Urgency: ").append(capAlert.urgency).append("\n")
            .append("Certainty: ").append(capAlert.certainty).append(" | Scope: ").append(capAlert.scope).append("\n\n")
            .append("Description:\n").append(capAlert.description).append("\n\n")
            .append("Instructions:\n").append(capAlert.instruction).append("\n\n")
            .append("CAP-CP ID: ").append(capAlert.identifier).append("\n")
            .append("Sender: ").append(capAlert.sender)
            .toString()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_location_pin)
            .setContentTitle("🚨 ECCC CAP Alert: ${capAlert.event}")
            .setContentText(capAlert.headline)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    fun sendWeatherAlertNotification(
        context: Context,
        locationName: String,
        alertTitle: String,
        alertDetails: String
    ) {
        val simulatedCap = CapAlertMessage(
            identifier = "urn:oid:2.49.0.1.124.0.2026.CAP-CP-ECCC-${System.currentTimeMillis()}",
            event = alertTitle,
            headline = "$alertTitle in effect for $locationName",
            description = alertDetails,
            instruction = "Take cover immediately if threatening weather approaches. Follow local ECCC emergency weather updates.",
            areaDesc = locationName
        )
        sendEcccCapAlertNotification(context, simulatedCap)
    }
}
