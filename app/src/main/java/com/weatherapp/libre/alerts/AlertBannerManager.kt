package com.weatherapp.libre.alerts

import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.weatherapp.theme.BoutiqueThemeEngine
import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.LocationPreferences
import com.weatherapp.libre.MainActivity
import com.weatherapp.libre.R
import com.weatherapp.libre.alerts.cache.GlobalAlertCache
import com.weatherapp.libre.alerts.models.AlertSeverity
import com.weatherapp.libre.alerts.models.AlertSource
import com.weatherapp.libre.alerts.routing.AlertProviderRouter
import com.weatherapp.libre.databinding.ActivityMainBinding
import com.weatherapp.libre.databinding.SectionAlertBannerBinding
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Live in-app alert surface. Reads active alerts from both ECCC AlertCache and GlobalAlertCache,
 * displaying single alerts directly or multi-alert summary banners.
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

    private data class DisplayAlert(
        val id: String,
        val sourceName: String,
        val event: String,
        val headline: String,
        val instruction: String?,
        val areaDesc: String?,
        val severityLevel: Int, // 4 = extreme/red, 3 = severe/orange, 2 = moderate/yellow, 1 = minor/info
        val sentUtcMs: Long?,
        val expiresUtcMs: Long?,
        val isTestFixture: Boolean = false
    )

    private val bannerBinding = SectionAlertBannerBinding.bind(binding.layoutAlertBanner.root)

    fun updateAlertBanner(currentLocation: LocationItem) {
        val isAlertsEnabled = locationPreferences.getAlertConsentState() == LocationPreferences.ALERT_CONSENT_OPT_IN
        val activeDisplayAlerts = mutableListOf<DisplayAlert>()

        val isCanada = currentLocation.countryCode.equals("CA", ignoreCase = true) ||
                currentLocation.country.equals("Canada", ignoreCase = true)

        // 1. ECCC Alerts (Canada ONLY)
        if (isCanada) {
            val activeCapAlert = alertManager.getActiveAlertForLocation(currentLocation)
            if (activeCapAlert != null) {
                val isDismissed = locationPreferences.isAlertDismissed(activeCapAlert.identifier, currentLocation)
                val isTest = activeCapAlert.isTestFixture
                if (!isDismissed && (isAlertsEnabled || isTest)) {
                    val level = when (activeCapAlert.severity.lowercase(Locale.getDefault())) {
                        "extreme" -> 4
                        "severe" -> 3
                        "moderate" -> 2
                        else -> 1
                    }
                    activeDisplayAlerts.add(
                        DisplayAlert(
                            id = activeCapAlert.identifier,
                            sourceName = activity.getString(R.string.source_eccc_protocol),
                            event = activeCapAlert.event,
                            headline = activeCapAlert.headline ?: "${activeCapAlert.event} in effect for ${currentLocation.displayName}",
                            instruction = activeCapAlert.instruction ?: activeCapAlert.description,
                            areaDesc = activeCapAlert.areaDesc,
                            severityLevel = level,
                            sentUtcMs = activeCapAlert.sentUtcMs,
                            expiresUtcMs = activeCapAlert.expiresUtcMs,
                            isTestFixture = isTest
                        )
                    )
                }
            }
        }

        // 2. Global Multi-Region Alerts (MeteoAlarm, etc.)
        val countryCode = currentLocation.countryCode?.uppercase(Locale.US) ?: ""
        val isMeteoAlarmEligible = AlertProviderRouter.METEOALARM_COUNTRY_CODES.contains(countryCode)
        if (isAlertsEnabled && isMeteoAlarmEligible) {
            val globalAlerts = GlobalAlertCache.readAlertsForLocation(activity, currentLocation)
            val unDismissedGlobal = globalAlerts.filter { !locationPreferences.isAlertDismissed(it.id, currentLocation) }
            for (alert in unDismissedGlobal) {
                val level = when (alert.severity) {
                    AlertSeverity.RED -> 4
                    AlertSeverity.ORANGE -> 3
                    AlertSeverity.YELLOW -> 2
                    else -> 1
                }
                val sourceLabel = when (alert.source) {
                    AlertSource.METEOALARM -> activity.getString(R.string.source_meteoalarm)
                    AlertSource.ECCC -> activity.getString(R.string.source_eccc_protocol)
                    else -> alert.source.name
                }
                activeDisplayAlerts.add(
                    DisplayAlert(
                        id = alert.id,
                        sourceName = sourceLabel,
                        event = alert.eventNative,
                        headline = alert.headline ?: "${alert.eventNative} in effect for ${currentLocation.displayName}",
                        instruction = alert.instruction ?: alert.description,
                        areaDesc = alert.areaDesc ?: currentLocation.displayName,
                        severityLevel = level,
                        sentUtcMs = null,
                        expiresUtcMs = null,
                        isTestFixture = false
                    )
                )
            }
        }

        if (activeDisplayAlerts.isEmpty()) {
            binding.layoutAlertBanner.root.visibility = View.GONE
            return
        }

        binding.layoutAlertBanner.root.visibility = View.VISIBLE
        val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())
        binding.layoutAlertBanner.root.setCardBackgroundColor(palette.surfaceCard)

        bannerBinding.tvAlertHeadline.setTextColor(palette.textPrimary)
        bannerBinding.tvAlertInstructions.setTextColor(palette.textSecondary)
        bannerBinding.tvAlertDetails.setTextColor(palette.textSecondary)
        bannerBinding.tvAlertSource.setTextColor(palette.textSecondary)

        if (activeDisplayAlerts.size == 1) {
            val alert = activeDisplayAlerts.first()
            val prefix = if (alert.isTestFixture) "TEST — " else "🚨 "
            bannerBinding.tvAlertHeaderTitle.text = "$prefix${alert.event.uppercase(Locale.getDefault())}"
            bannerBinding.tvAlertHeadline.text = alert.headline

            if (!alert.instruction.isNullOrBlank()) {
                bannerBinding.tvAlertInstructions.visibility = View.VISIBLE
                bannerBinding.tvAlertInstructions.text = alert.instruction
            } else {
                bannerBinding.tvAlertInstructions.visibility = View.GONE
            }

            if (alert.sentUtcMs != null) {
                val zoneId = ZoneId.systemDefault()
                val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a", Locale.getDefault())
                val sentStr = java.time.Instant.ofEpochMilli(alert.sentUtcMs).atZone(zoneId).format(dateFormatter)
                val expiresStr = alert.expiresUtcMs?.let { java.time.Instant.ofEpochMilli(it).atZone(zoneId).format(dateFormatter) } ?: "Until further notice"
                bannerBinding.tvAlertDetails.visibility = View.VISIBLE
                bannerBinding.tvAlertDetails.text = "Area: ${alert.areaDesc}\nIssued: $sentStr • Expires: $expiresStr"
            } else if (!alert.areaDesc.isNullOrBlank()) {
                bannerBinding.tvAlertDetails.visibility = View.VISIBLE
                bannerBinding.tvAlertDetails.text = "Area: ${alert.areaDesc}"
            } else {
                bannerBinding.tvAlertDetails.visibility = View.GONE
            }

            bannerBinding.tvAlertSource.text = alert.sourceName

            val severityColor = when (alert.severityLevel) {
                4 -> palette.alertExtreme
                3 -> palette.alertSevere
                2 -> palette.alertModerate
                else -> palette.alertMinor
            }

            binding.layoutAlertBanner.root.strokeColor = severityColor
            bannerBinding.tvAlertHeaderTitle.setTextColor(severityColor)
            bannerBinding.btnDismissAlert.setTextColor(severityColor)

            bannerBinding.btnDismissAlert.setOnClickListener {
                Log.d(TAG, "Dismissing alert ${alert.id} for location ${currentLocation.latitude}, ${currentLocation.longitude}")
                locationPreferences.dismissAlert(alert.id, currentLocation)
                updateAlertBanner(currentLocation)
            }

            binding.layoutAlertBanner.root.setOnClickListener {
                (activity as? MainActivity)?.settingsDialogManager?.showAlertLogScreen()
            }
        } else {
            // Multiple Active Alerts Summary View
            val alertCount = activeDisplayAlerts.size
            bannerBinding.tvAlertHeaderTitle.text = "🚨 $alertCount ACTIVE WEATHER ALERTS"
            bannerBinding.tvAlertHeadline.text = "Tap to view details"
            bannerBinding.tvAlertInstructions.text = ""
            bannerBinding.tvAlertInstructions.visibility = View.GONE
            bannerBinding.tvAlertDetails.visibility = View.GONE
            bannerBinding.tvAlertSource.text = "Multiple Alert Sources"

            val maxSeverityLevel = activeDisplayAlerts.maxOf { it.severityLevel }
            val severityColor = when (maxSeverityLevel) {
                4 -> palette.alertExtreme
                3 -> palette.alertSevere
                2 -> palette.alertModerate
                else -> palette.alertMinor
            }

            binding.layoutAlertBanner.root.strokeColor = severityColor
            bannerBinding.tvAlertHeaderTitle.setTextColor(severityColor)
            bannerBinding.btnDismissAlert.setTextColor(severityColor)

            bannerBinding.btnDismissAlert.setOnClickListener {
                val topAlert = activeDisplayAlerts.first()
                Log.d(TAG, "Dismissing top alert ${topAlert.id} for location ${currentLocation.latitude}, ${currentLocation.longitude}")
                locationPreferences.dismissAlert(topAlert.id, currentLocation)
                updateAlertBanner(currentLocation)
            }

            binding.layoutAlertBanner.root.setOnClickListener {
                (activity as? MainActivity)?.settingsDialogManager?.showAlertLogScreen()
            }
        }
    }
}
