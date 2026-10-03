package com.weatherapp.libre.alerts

import android.graphics.Color
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.LocationPreferences
import com.weatherapp.libre.databinding.ActivityMainBinding
import com.weatherapp.libre.databinding.SectionAlertBannerBinding
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
                val info = activeCapAlert.infoList.firstOrNull()
                if (info != null) {
                    binding.layoutAlertBanner.root.visibility = View.VISIBLE

                    val prefix = if (isTest) "TEST — " else "🚨 "
                    bannerBinding.tvAlertHeaderTitle.text = "$prefix${info.event.uppercase(Locale.getDefault())}"
                    bannerBinding.tvAlertHeadline.text = info.headline ?: "${info.event} in effect for ${currentLocation.displayName}"
                    bannerBinding.tvAlertInstructions.text = info.instruction ?: info.description ?: ""

                    val zoneId = ZoneId.systemDefault()
                    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a", Locale.getDefault())
                    val sentStr = activeCapAlert.sent.atZone(zoneId).format(dateFormatter)
                    val expiresStr = info.expires?.atZone(zoneId)?.format(dateFormatter) ?: "Until further notice"
                    val areaDesc = info.areaList.firstOrNull()?.areaDesc ?: currentLocation.displayName

                    bannerBinding.tvAlertDetails.visibility = View.VISIBLE
                    bannerBinding.tvAlertDetails.text = "Area: $areaDesc\nIssued: $sentStr • Expires: $expiresStr"

                    // Severity Styling
                    when (info.severity.lowercase(Locale.getDefault())) {
                        "extreme" -> {
                            binding.layoutAlertBanner.root.setCardBackgroundColor(Color.parseColor("#2A1015"))
                            binding.layoutAlertBanner.root.strokeColor = Color.parseColor("#FF4D4D")
                            bannerBinding.tvAlertHeaderTitle.setTextColor(Color.parseColor("#FF4D4D"))
                        }
                        "severe" -> {
                            binding.layoutAlertBanner.root.setCardBackgroundColor(Color.parseColor("#2A1B10"))
                            binding.layoutAlertBanner.root.strokeColor = Color.parseColor("#FF9900")
                            bannerBinding.tvAlertHeaderTitle.setTextColor(Color.parseColor("#FF9900"))
                        }
                        else -> {
                            binding.layoutAlertBanner.root.setCardBackgroundColor(Color.parseColor("#1B2010"))
                            binding.layoutAlertBanner.root.strokeColor = Color.parseColor("#FFCC00")
                            bannerBinding.tvAlertHeaderTitle.setTextColor(Color.parseColor("#FFCC00"))
                        }
                    }

                    bannerBinding.btnDismissAlert.setOnClickListener {
                        Log.d(TAG, "Dismissing alert ${activeCapAlert.identifier} for location ${currentLocation.id}")
                        locationPreferences.dismissAlert(activeCapAlert.identifier)
                        binding.layoutAlertBanner.root.visibility = View.GONE
                    }
                    return
                }
            }
        }

        binding.layoutAlertBanner.root.visibility = View.GONE
    }
}
