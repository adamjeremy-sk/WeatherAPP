package com.weatherapp.libre.alerts

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.weatherapp.libre.LocationItem
import com.weatherapp.libre.LocationPreferences
import com.weatherapp.libre.databinding.ActivityMainBinding
import java.util.Locale

class AlertBannerManager(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val locationPreferences: LocationPreferences
) {
    fun updateAlertBanner(currentLocation: LocationItem) {
        val isAlertsEnabled = locationPreferences.isWeatherAlertsEnabled()
        if (!isAlertsEnabled) {
            binding.cardAlertBanner.visibility = View.GONE
            return
        }

        val alertManager = AlertManager(activity.applicationContext, locationPreferences)
        val activeCapAlert = alertManager.getActiveAlertForLocation(currentLocation)

        if (activeCapAlert != null && !locationPreferences.isAlertDismissed(activeCapAlert.identifier)) {
            val info = activeCapAlert.infoList.firstOrNull()
            if (info != null) {
                binding.cardAlertBanner.visibility = View.VISIBLE
                val prefix = if (activeCapAlert.isTestFixture) "TEST — " else "🚨 "
                binding.tvAlertHeaderTitle.text = "$prefix${info.event.uppercase(Locale.getDefault())}"
                binding.tvAlertHeadline.text = info.headline ?: "${info.event} in effect"
                binding.tvAlertInstructions.text = info.instruction ?: info.description ?: ""
                binding.btnDismissAlert.setOnClickListener {
                    locationPreferences.dismissAlert(activeCapAlert.identifier)
                    binding.cardAlertBanner.visibility = View.GONE
                }
                return
            }
        }

        binding.cardAlertBanner.visibility = View.GONE
    }
}
