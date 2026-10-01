import re

def main():
    # In MainActivity, we need to extract AlertBannerManager
    with open('app/src/main/java/com/weatherapp/libre/alerts/AlertBannerManager.kt', 'w') as f:
        f.write('''package com.weatherapp.libre.alerts

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
''')
    
    # Remove it from WeatherDisplayManager
    with open('app/src/main/java/com/weatherapp/libre/WeatherDisplayManager.kt', 'r') as f:
        lines = f.readlines()
        
    start_idx = -1
    end_idx = -1
    for i, line in enumerate(lines):
        if 'fun updateAlertBanner()' in line:
            start_idx = i
        elif start_idx != -1 and 'fun updateDetailCard(day: DayForecast)' in line:
            end_idx = i
            break
            
    if start_idx != -1 and end_idx != -1:
        new_lines = lines[:start_idx] + lines[end_idx:]
        with open('app/src/main/java/com/weatherapp/libre/WeatherDisplayManager.kt', 'w') as f:
            f.writelines(new_lines)
            
    # Modify MainActivity to use AlertBannerManager
    with open('app/src/main/java/com/weatherapp/libre/MainActivity.kt', 'r') as f:
        main_lines = f.readlines()
        
    for i, line in enumerate(main_lines):
        if 'val weatherDisplayManager = WeatherDisplayManager(' in line:
            main_lines.insert(i, '    val alertBannerManager = com.weatherapp.libre.alerts.AlertBannerManager(this, binding, locationPreferences)\n')
            break
            
    for i, line in enumerate(main_lines):
        if 'weatherDisplayManager.updateAlertBanner()' in line:
            main_lines[i] = '        alertBannerManager.updateAlertBanner(weatherDisplayManager.currentLocation)\n'
            
    with open('app/src/main/java/com/weatherapp/libre/MainActivity.kt', 'w') as f:
        f.writelines(main_lines)
        
    print("Extracted AlertBannerManager.")

if __name__ == "__main__":
    main()
