package com.weatherapp.libre.alerts

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.weatherapp.libre.BoutiqueThemeEngine
import com.weatherapp.libre.LocationPreferences

object AlertConsentDialog {

    const val CURRENT_ALERT_CONSENT_VERSION = 1

    fun checkAndShowConsentPrompt(
        activity: AppCompatActivity,
        locationPreferences: LocationPreferences,
        isUpdateInstall: Boolean = false,
        onConsentCompleted: (() -> Unit)? = null
    ) {
        val storedVersion = locationPreferences.getAlertConsentVersion()
        if (storedVersion >= CURRENT_ALERT_CONSENT_VERSION) {
            onConsentCompleted?.invoke()
            return
        }

        if (activity.isFinishing || activity.isDestroyed) return

        val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())
        val density = activity.resources.displayMetrics.density

        val dialog = Dialog(activity)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        val rootCard = MaterialCardView(activity).apply {
            radius = 20 * density
            strokeWidth = (1 * density).toInt()
            strokeColor = palette.dividerColor
            setCardBackgroundColor(palette.surfaceCard)
            layoutParams = ViewGroup.LayoutParams((320 * density).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (24 * density).toInt(), (24 * density).toInt(), (24 * density).toInt())
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val titleTv = TextView(activity).apply {
            text = "🚨 Severe Weather Alerts"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(palette.textPrimary)
            gravity = Gravity.CENTER
        }

        val bodyText = if (isUpdateInstall && locationPreferences.isWeatherAlertsEnabled()) {
            "The alert system has been rebuilt to fetch official, authentic Common Alerting Protocol (CAP-CP) alerts directly from Environment and Climate Change Canada (ECCC).\n\nPrevious notification choices do not carry over. Would you like to enable official ECCC severe weather notifications and background monitoring?"
        } else {
            "WeatherAPP can monitor official Environment and Climate Change Canada (ECCC) severe weather warnings in the background and notify you when severe weather threatens your active city.\n\nBackground monitoring and notifications are optional and strictly opt-in. Would you like to enable weather alert notifications?"
        }

        val bodyTv = TextView(activity).apply {
            text = bodyText
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(palette.textSecondary)
            setPadding(0, (12 * density).toInt(), 0, (20 * density).toInt())
            gravity = Gravity.START
        }

        val btnEnable = MaterialButton(activity).apply {
            text = "Enable Alerts"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
            setBackgroundColor(palette.brandPrimary)
            setTextColor(Color.WHITE)
            cornerRadius = (12 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (48 * density).toInt())
        }

        val btnNoThanks = MaterialButton(activity, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
            text = "No Thanks"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(palette.textSecondary)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (44 * density).toInt()).apply {
                topMargin = (8 * density).toInt()
            }
        }

        btnEnable.setOnClickListener {
            locationPreferences.saveAlertConsentVersion(CURRENT_ALERT_CONSENT_VERSION)
            locationPreferences.saveWeatherAlertsEnabled(true)

            AlertNotificationHelper.createNotificationChannels(activity)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
            AlertPollWorker.schedule(activity.applicationContext)

            dialog.dismiss()
            onConsentCompleted?.invoke()
        }

        btnNoThanks.setOnClickListener {
            locationPreferences.saveAlertConsentVersion(CURRENT_ALERT_CONSENT_VERSION)
            locationPreferences.saveWeatherAlertsEnabled(false)

            AlertPollWorker.cancel(activity.applicationContext)

            dialog.dismiss()
            onConsentCompleted?.invoke()
        }

        dialog.setOnCancelListener {
            locationPreferences.saveAlertConsentVersion(CURRENT_ALERT_CONSENT_VERSION)
            locationPreferences.saveWeatherAlertsEnabled(false)
            AlertPollWorker.cancel(activity.applicationContext)
            onConsentCompleted?.invoke()
        }

        container.addView(titleTv)
        container.addView(bodyTv)
        container.addView(btnEnable)
        container.addView(btnNoThanks)
        rootCard.addView(container)

        dialog.setContentView(rootCard)
        dialog.show()
    }
}
