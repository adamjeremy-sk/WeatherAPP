package com.weatherapp.libre.alerts

import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.weatherapp.libre.BoutiqueThemeEngine
import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.LocationPreferences
import com.weatherapp.libre.databinding.ActivityMainBinding
import com.weatherapp.libre.databinding.SectionAlertBannerBinding
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Live in-app alert surface. Reads active alerts from the persistent AlertCache written by AlertPollWorker, updated on the minute cadence.
 * Note: Test fixtures (isTest = true) intentionally bypass consent gating as debug behavior for in-app inspection.
 */
class AlertBannerManager(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val locationPreferences: LocationPreferences,
    private val alertManager: AlertManager
) {
    companion object {
        private const val TAG = "AlertBannerManager"
    }

    private val bannerBinding = SectionAlertBannerBinding.bind(binding.layoutAlertBanner.root)

    fun updateAlertBanner(currentLocation: LocationItem) {
        val activeCapAlert = alertManager.getActiveAlertForLocation(currentLocation)
        val isAlertsEnabled = locationPreferences.getAlertConsentState() == LocationPreferences.ALERT_CONSENT_OPT_IN

        if (activeCapAlert != null) {
            val isDismissed = locationPreferences.isAlertDismissed(activeCapAlert.identifier)
            val isTest = activeCapAlert.isTestFixture

            if (!isDismissed && (isAlertsEnabled || isTest)) {
                binding.layoutAlertBanner.root.visibility = View.VISIBLE

                val prefix = if (isTest) "TEST — " else "🚨 "
                bannerBinding.tvAlertHeaderTitle.text = "$prefix${activeCapAlert.event.uppercase(Locale.getDefault())}"
                bannerBinding.tvAlertHeadline.text = activeCapAlert.headline ?: "${activeCapAlert.event} in effect for ${currentLocation.displayName}"
                bannerBinding.tvAlertInstructions.text = activeCapAlert.instruction ?: activeCapAlert.description ?: ""

                val zoneId = ZoneId.systemDefault()
                val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a", Locale.getDefault())
                val sentStr = java.time.Instant.ofEpochMilli(activeCapAlert.sentUtcMs).atZone(zoneId).format(dateFormatter)
                val expiresStr = activeCapAlert.expiresUtcMs?.let { java.time.Instant.ofEpochMilli(it).atZone(zoneId).format(dateFormatter) } ?: "Until further notice"
                val areaDesc = activeCapAlert.areaDesc

                bannerBinding.tvAlertDetails.visibility = View.VISIBLE
                bannerBinding.tvAlertDetails.text = "Area: $areaDesc\nIssued: $sentStr • Expires: $expiresStr"

                // Palette Theming
                val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())
                binding.layoutAlertBanner.root.setCardBackgroundColor(palette.surfaceCard)

                val severityColor = when (activeCapAlert.severity.lowercase(Locale.getDefault())) {
                    "extreme" -> palette.alertExtreme
                    "severe" -> palette.alertSevere
                    "moderate" -> palette.alertModerate
                    else -> palette.alertMinor
                }

                binding.layoutAlertBanner.root.strokeColor = severityColor
                bannerBinding.tvAlertHeaderTitle.setTextColor(severityColor)
                bannerBinding.btnDismissAlert.setTextColor(severityColor)

                bannerBinding.tvAlertHeadline.setTextColor(palette.textPrimary)
                bannerBinding.tvAlertInstructions.setTextColor(palette.textSecondary)
                bannerBinding.tvAlertDetails.setTextColor(palette.textSecondary)
                bannerBinding.tvAlertSource.setTextColor(palette.textSecondary)

                bannerBinding.btnDismissAlert.setOnClickListener {
                    Log.d(TAG, "Dismissing alert ${activeCapAlert.identifier} for location ${currentLocation.id}")
                    locationPreferences.dismissAlert(activeCapAlert.identifier)
                    binding.layoutAlertBanner.root.visibility = View.GONE
                }
                return
            }
        }

        binding.layoutAlertBanner.root.visibility = View.GONE
    }
}
